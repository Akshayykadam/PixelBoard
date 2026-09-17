package com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi

import java.io.File
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ASTORE
import org.objectweb.asm.Opcodes.CHECKCAST
import org.objectweb.asm.Opcodes.DUP
import org.objectweb.asm.Opcodes.DUP2
import org.objectweb.asm.Opcodes.DUP2_X1
import org.objectweb.asm.Opcodes.DUP2_X2
import org.objectweb.asm.Opcodes.DUP_X1
import org.objectweb.asm.Opcodes.DUP_X2
import org.objectweb.asm.Opcodes.GETSTATIC
import org.objectweb.asm.Opcodes.NEW
import org.objectweb.asm.Opcodes.NOP
import org.objectweb.asm.Opcodes.PUTSTATIC
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.SourceInterpreter
import org.objectweb.asm.tree.analysis.SourceValue

internal data class CompiledPatchRuntimeCalls(
    val unregisteredRuntimeReferences: Set<String>,
    val usedCallIds: Set<RuntimeCallId>,
)

internal object CompiledPatchRuntimeCallInventory {
    private const val RUNTIME_ABI_PACKAGE =
        "com/akshaykadam/pixelboard/patches/gboard/shared/runtimeabi"
    private const val RUNTIME_CALL_ID_OWNER = "$RUNTIME_ABI_PACKAGE/RuntimeCallId"
    private const val RUNTIME_CALL_ID_DESCRIPTOR = "L$RUNTIME_CALL_ID_OWNER;"
    private const val RUNTIME_CALL_EMITTER_OWNER = "$RUNTIME_ABI_PACKAGE/RuntimeCallEmitter"
    private const val SOFT_KEY_FEATURE_OWNER =
        "com/akshaykadam/pixelboard/patches/gboard/shared/GboardSoftKeyFamilyFeature"
    private const val SOFT_KEY_COMPOSER_OWNER =
        "com/akshaykadam/pixelboard/patches/gboard/shared/GboardSoftKeyFamilyComposerKt"
    private const val SOFT_KEY_EMITTER_NAME = "emitSoftKeyRuntimeCall"
    private const val GESTURE_COMPOSER_OWNER =
        "com/akshaykadam/pixelboard/patches/gboard/shared/GboardGestureFamilyComposerKt"
    private const val GESTURE_STAGE_OWNER =
        "com/akshaykadam/pixelboard/patches/gboard/shared/GboardGestureFamilyStage"
    private const val GESTURE_EMITTER_NAME = "emitGestureRuntimeCall"
    private const val STRING_BUILDER_OWNER = "java/lang/StringBuilder"

    private val runtimeReference = Regex(
        "Lcom/akshaykadam/pixelboard/extension[^;]*;->" +
            "[A-Za-z0-9_$<>]+\\([A-Za-z0-9_/$;\\[]*\\)[A-Za-z0-9_/$;\\[]+"
    )
    private val callIdsByName = RuntimeCallId.entries.associateBy(RuntimeCallId::name)

    fun read(classesRoot: File): CompiledPatchRuntimeCalls {
        require(classesRoot.isDirectory) { "Compiled patch classes not found: $classesRoot" }
        val classes = classesRoot.walkTopDown()
            .filter { file -> file.isFile && file.extension == "class" }
            .map(::readClass)
            .filterNot { node -> node.name.startsWith("$RUNTIME_ABI_PACKAGE/") }
            .toList()
        val aliases = findRuntimeCallAliases(classes)
        val methods = classes.flatMap { node ->
            node.methods
                .filter { method -> method.instructions.size() > 0 }
                .map { method -> analyzeMethod(node.name, method, aliases) }
        }
        val summaries = findRuntimeCallFlows(methods)
        val softKeyInventory = findSoftKeyRuntimeCallInventory(classes)
        val emitsSoftKeyInventory = summaries.any { (method, flow) ->
            method.owner == SOFT_KEY_COMPOSER_OWNER &&
                method.name == SOFT_KEY_EMITTER_NAME &&
                flow.parameterIndices.contains(0)
        }
        val gestureInventory = findRuntimeCallInventory(classes, GESTURE_STAGE_OWNER)
        val emitsGestureInventory = summaries.any { (method, flow) ->
            method.owner == GESTURE_COMPOSER_OWNER &&
                method.name == GESTURE_EMITTER_NAME &&
                flow.parameterIndices.contains(0)
        }

        return CompiledPatchRuntimeCalls(
            unregisteredRuntimeReferences = methods
                .flatMap(AnalyzedMethod::runtimeReferences)
                .toSet(),
            usedCallIds = summaries.values
                .flatMapTo(linkedSetOf(), RuntimeCallFlow::callIds)
                .apply {
                    if (emitsSoftKeyInventory) addAll(softKeyInventory)
                    if (emitsGestureInventory) addAll(gestureInventory)
                },
        )
    }

    private fun readClass(file: File): ClassNode = ClassNode().also { node ->
        ClassReader(file.readBytes()).accept(node, ClassReader.SKIP_DEBUG)
    }

    private fun findSoftKeyRuntimeCallInventory(classes: List<ClassNode>): Set<RuntimeCallId> =
        findRuntimeCallInventory(classes, SOFT_KEY_FEATURE_OWNER)

    private fun findRuntimeCallInventory(
        classes: List<ClassNode>,
        owner: String,
    ): Set<RuntimeCallId> =
        classes.singleOrNull { node -> node.name == owner }
            ?.methods
            ?.singleOrNull { method -> method.name == "<clinit>" }
            ?.instructions
            ?.asSequence()
            ?.filterIsInstance<FieldInsnNode>()
            ?.filter { instruction -> instruction.opcode == GETSTATIC }
            ?.mapNotNull { instruction -> instruction.runtimeCallId() }
            ?.toCollection(linkedSetOf())
            .orEmpty()

    private fun findRuntimeCallAliases(classes: List<ClassNode>): Map<FieldKey, RuntimeCallId> =
        buildMap {
            classes.forEach { node ->
                node.methods.filter { method -> method.name == "<clinit>" }.forEach { method ->
                    var pendingCall: RuntimeCallId? = null
                    method.instructions.forEach { instruction ->
                        when {
                            instruction.isMetadataOrNop() -> Unit
                            instruction is FieldInsnNode && instruction.opcode == GETSTATIC -> {
                                pendingCall = instruction.runtimeCallId()
                            }
                            instruction is FieldInsnNode &&
                                instruction.opcode == PUTSTATIC &&
                                instruction.desc == RUNTIME_CALL_ID_DESCRIPTOR -> {
                                pendingCall?.let { call ->
                                    put(FieldKey(instruction.owner, instruction.name, instruction.desc), call)
                                }
                                pendingCall = null
                            }
                            else -> pendingCall = null
                        }
                    }
                }
            }
        }

    private fun analyzeMethod(
        owner: String,
        method: MethodNode,
        aliases: Map<FieldKey, RuntimeCallId>,
    ): AnalyzedMethod {
        val instructions = method.instructions.toArray()
        val frames = Analyzer(SourceInterpreter()).analyze(owner, method)
        val analysis = MethodAnalysis(
            method = method,
            frames = frames,
            instructionIndex = instructions.withIndex().associate { (index, instruction) ->
                instruction to index
            },
            parameterByLocalSlot = parameterByLocalSlot(method),
            aliases = aliases,
        )
        val baseFlow = RuntimeCallFlow()
        val callSites = mutableListOf<CallSite>()

        instructions.forEachIndexed { index, instruction ->
            if (instruction !is MethodInsnNode) return@forEachIndexed
            val frame = frames[index] ?: return@forEachIndexed
            val arguments = invocationArguments(frame, instruction.desc)
            if (instruction.isRuntimeCallEmitterInvocation()) {
                arguments.firstOrNull()?.let { callArgument ->
                    baseFlow.add(analysis.origins(callArgument))
                }
            } else {
                callSites += CallSite(
                    target = MethodKey(instruction.owner, instruction.name, instruction.desc),
                    argumentOrigins = arguments.map { argument -> analysis.origins(argument) },
                )
            }
        }

        return AnalyzedMethod(
            key = MethodKey(owner, method.name, method.desc),
            baseFlow = baseFlow,
            callSites = callSites,
            runtimeReferences = findRuntimeReferences(analysis, instructions),
        )
    }

    private fun findRuntimeCallFlows(
        methods: List<AnalyzedMethod>,
    ): Map<MethodKey, RuntimeCallFlow> {
        val flows = methods.associate { method -> method.key to method.baseFlow.copy() }.toMutableMap()
        var changed: Boolean
        do {
            changed = false
            methods.forEach { method ->
                val updated = method.baseFlow.copy()
                method.callSites.forEach { callSite ->
                    flows[callSite.target]?.parameterIndices?.forEach { parameterIndex ->
                        callSite.argumentOrigins.getOrNull(parameterIndex)?.let(updated::add)
                    }
                }
                if (updated != flows.getValue(method.key)) {
                    flows[method.key] = updated
                    changed = true
                }
            }
        } while (changed)
        return flows
    }

    private fun invocationArguments(
        frame: Frame<SourceValue>,
        descriptor: String,
    ): List<SourceValue> {
        val argumentCount = Type.getArgumentTypes(descriptor).size
        val firstArgument = frame.stackSize - argumentCount
        return (0 until argumentCount).map { offset -> frame.getStack(firstArgument + offset) }
    }

    private fun parameterByLocalSlot(method: MethodNode): Map<Int, Int> = buildMap {
        var localSlot = if (method.access and ACC_STATIC != 0) 0 else 1
        Type.getArgumentTypes(method.desc).forEachIndexed { parameterIndex, argumentType ->
            put(localSlot, parameterIndex)
            localSlot += argumentType.size
        }
    }

    private fun findRuntimeReferences(
        analysis: MethodAnalysis,
        instructions: Array<AbstractInsnNode>,
    ): Set<String> {
        val candidates = linkedSetOf<String>()
        instructions.forEachIndexed { index, instruction ->
            if (instruction is LdcInsnNode && instruction.cst is String) {
                candidates += instruction.cst as String
            }
            if (
                instruction is MethodInsnNode &&
                instruction.owner == STRING_BUILDER_OWNER &&
                instruction.name == "toString" &&
                instruction.desc == "()Ljava/lang/String;"
            ) {
                val frame = analysis.frames[index] ?: return@forEachIndexed
                val receiver = frame.getStack(frame.stackSize - 1)
                analysis.builderValue(receiver)?.let(candidates::add)
            }
        }
        return candidates.flatMapTo(linkedSetOf()) { candidate ->
            runtimeReference.findAll(candidate).map(MatchResult::value).toList()
        }
    }

    private fun MethodAnalysis.origins(value: SourceValue): RuntimeCallFlow =
        value.insns
            .mapNotNull { source -> originFrom(source) }
            .fold(RuntimeCallFlow(), RuntimeCallFlow::merge)

    private fun MethodAnalysis.originFrom(source: AbstractInsnNode): RuntimeCallFlow? {
        val index = instructionIndex[source] ?: return null
        return when {
            source is FieldInsnNode && source.opcode == GETSTATIC -> {
                val call = source.runtimeCallId()
                    ?: aliases[FieldKey(source.owner, source.name, source.desc)]
                call?.let { RuntimeCallFlow(callIds = linkedSetOf(it)) }
            }
            source is VarInsnNode && source.opcode == ALOAD -> {
                val local = frames[index]?.getLocal(source.`var`) ?: return null
                if (local.insns.isEmpty()) {
                    parameterByLocalSlot[source.`var`]
                        ?.let { RuntimeCallFlow(parameterIndices = linkedSetOf(it)) }
                } else {
                    origins(local)
                }
            }
            source is VarInsnNode && source.opcode == ASTORE -> {
                frames[index]?.topStackValue()?.let { value -> origins(value) }
            }
            source is TypeInsnNode && source.opcode == CHECKCAST -> {
                frames[index]?.topStackValue()?.let { value -> origins(value) }
            }
            source.opcode in DUP_OPCODES -> frames[index]?.topStackValue()?.let { value ->
                origins(value)
            }
            else -> null
        }
    }

    private fun MethodAnalysis.builderValue(value: SourceValue): String? =
        value.insns
            .mapNotNull { source -> builderValueFrom(source) }
            .distinct()
            .singleOrNull()

    private fun MethodAnalysis.builderValueFrom(source: AbstractInsnNode): String? {
        val index = instructionIndex[source] ?: return null
        return when {
            source is TypeInsnNode && source.opcode == NEW && source.desc == STRING_BUILDER_OWNER -> ""
            source is VarInsnNode && source.opcode == ALOAD -> {
                frames[index]?.getLocal(source.`var`)?.let { value -> builderValue(value) }
            }
            source.opcode in DUP_OPCODES -> frames[index]?.topStackValue()?.let { value ->
                builderValue(value)
            }
            source is MethodInsnNode &&
                source.owner == STRING_BUILDER_OWNER &&
                source.name == "append" &&
                source.desc == "(Ljava/lang/String;)Ljava/lang/StringBuilder;" -> {
                val frame = frames[index] ?: return null
                val receiver = frame.getStack(frame.stackSize - 2)
                val argument = frame.getStack(frame.stackSize - 1)
                val prefix = builderValue(receiver) ?: return null
                val suffix = stringValue(argument) ?: return null
                prefix + suffix
            }
            else -> null
        }
    }

    private fun MethodAnalysis.stringValue(value: SourceValue): String? =
        value.insns
            .mapNotNull { source -> stringValueFrom(source) }
            .distinct()
            .singleOrNull()

    private fun MethodAnalysis.stringValueFrom(source: AbstractInsnNode): String? {
        val index = instructionIndex[source] ?: return null
        return when {
            source is LdcInsnNode && source.cst is String -> source.cst as String
            source is VarInsnNode && source.opcode == ALOAD -> {
                frames[index]?.getLocal(source.`var`)?.let { value -> stringValue(value) }
            }
            source is TypeInsnNode && source.opcode == CHECKCAST -> {
                frames[index]?.topStackValue()?.let { value -> stringValue(value) }
            }
            source is MethodInsnNode &&
                source.owner == STRING_BUILDER_OWNER &&
                source.name == "toString" &&
                source.desc == "()Ljava/lang/String;" -> {
                frames[index]?.topStackValue()?.let { value -> builderValue(value) }
            }
            else -> null
        }
    }

    private fun MethodInsnNode.isRuntimeCallEmitterInvocation(): Boolean =
        owner == RUNTIME_CALL_EMITTER_OWNER &&
            name == "invoke" &&
            desc.startsWith("($RUNTIME_CALL_ID_DESCRIPTOR")

    private fun FieldInsnNode.runtimeCallId(): RuntimeCallId? =
        takeIf { field ->
            field.owner == RUNTIME_CALL_ID_OWNER && field.desc == RUNTIME_CALL_ID_DESCRIPTOR
        }?.let { field -> callIdsByName[field.name] }

    private fun AbstractInsnNode.isMetadataOrNop(): Boolean =
        this is LabelNode ||
            this is LineNumberNode ||
            this is FrameNode ||
            (this is InsnNode && opcode == NOP)

    private fun Frame<SourceValue>.topStackValue(): SourceValue? =
        takeIf { frame -> frame.stackSize > 0 }?.getStack(stackSize - 1)

    private data class FieldKey(
        val owner: String,
        val name: String,
        val descriptor: String,
    )

    private data class MethodKey(
        val owner: String,
        val name: String,
        val descriptor: String,
    )

    private data class CallSite(
        val target: MethodKey,
        val argumentOrigins: List<RuntimeCallFlow>,
    )

    private data class AnalyzedMethod(
        val key: MethodKey,
        val baseFlow: RuntimeCallFlow,
        val callSites: List<CallSite>,
        val runtimeReferences: Set<String>,
    )

    private data class MethodAnalysis(
        val method: MethodNode,
        val frames: Array<Frame<SourceValue>?>,
        val instructionIndex: Map<AbstractInsnNode, Int>,
        val parameterByLocalSlot: Map<Int, Int>,
        val aliases: Map<FieldKey, RuntimeCallId>,
    )

    private data class RuntimeCallFlow(
        val callIds: LinkedHashSet<RuntimeCallId> = linkedSetOf(),
        val parameterIndices: LinkedHashSet<Int> = linkedSetOf(),
    ) {
        fun add(other: RuntimeCallFlow) {
            callIds += other.callIds
            parameterIndices += other.parameterIndices
        }

        fun merge(other: RuntimeCallFlow): RuntimeCallFlow = apply { add(other) }

        fun copy(): RuntimeCallFlow = RuntimeCallFlow(
            callIds = LinkedHashSet(callIds),
            parameterIndices = LinkedHashSet(parameterIndices),
        )
    }

    private val DUP_OPCODES = setOf(DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2)
}
