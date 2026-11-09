package com.akshaykadam.pixelboard.patches.gboard.shared

import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.BytecodePatchContext
import com.akshaykadam.pixelboard.patches.shared.MutableClass
import com.akshaykadam.pixelboard.patches.shared.MutableField
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.toMutable
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

internal fun BytecodePatchContext.mutableClass(type: String) = mutableClassDefBy(type)

internal fun BytecodePatchContext.addFieldIfMissing(
    classType: String,
    fieldName: String,
    fieldType: String,
    accessFlags: Int
) {
    mutableClass(classType).addFieldIfMissing(
        fieldName = fieldName,
        fieldType = fieldType,
        accessFlags = accessFlags,
    )
}

internal fun MutableClass.addFieldIfMissing(
    fieldName: String,
    fieldType: String,
    accessFlags: Int,
) {
    if (fields.any { it.name == fieldName && it.type == fieldType }) return

    fields.add(
        ImmutableField(
            type,
            fieldName,
            fieldType,
            accessFlags,
            null,
            null,
            null
        ).toMutable()
    )
}

internal fun BytecodePatchContext.findMutableMethodOrThrow(
    classType: String,
    name: String,
    returnType: String,
    parameterTypes: List<String>
): MutableMethod {
    val mutableClass = mutableClass(classType)
    return mutableClass.methods.firstOrNull {
        it.name == name && it.returnType == returnType && it.parameterTypes == parameterTypes
    } ?: error("Could not find $classType->$name(${parameterTypes.joinToString("")})$returnType")
}

internal fun BytecodePatchContext.findMutableMethodOrThrow(
    binding: GboardMethodTarget,
): MutableMethod = binding.resolve(this)

internal fun BytecodePatchContext.findMutableMethodOrNull(
    binding: GboardMethodTarget,
): MutableMethod? = binding.resolveOrNull(this)

internal fun BytecodePatchContext.mutableFieldOrThrow(
    binding: GboardFieldTarget,
): MutableField = binding.resolve(this)

internal fun BytecodePatchContext.mutableFieldOrNull(
    binding: GboardFieldTarget,
): MutableField? = binding.resolveOrNull(this)

internal fun BytecodePatchContext.addHelperMethodIfMissing(
    classType: String,
    name: String,
    parameterTypes: List<String>,
    returnType: String,
    accessFlags: Int,
    registerCount: Int,
    body: String
) {
    mutableClass(classType).addHelperMethodIfMissing(
        name = name,
        parameterTypes = parameterTypes,
        returnType = returnType,
        accessFlags = accessFlags,
        registerCount = registerCount,
        body = body,
    )
}

internal fun MutableClass.addHelperMethodIfMissing(
    name: String,
    parameterTypes: List<String>,
    returnType: String,
    accessFlags: Int,
    registerCount: Int,
    body: String,
) {
    if (methods.any {
            it.name == name && it.returnType == returnType && it.parameterTypes == parameterTypes
        }) {
        return
    }

    methods.add(
        ImmutableMethod(
            type,
            name,
            parameterTypes.map { ImmutableMethodParameter(it, null, null) },
            returnType,
            accessFlags,
            null,
            null,
            MutableMethodImplementation(registerCount)
        ).toMutable().apply {
            addInstructions(0, body.trimIndent())
        }
    )
}

internal fun MutableMethod.indexOfFirstMethodCall(
    definingClass: String,
    name: String,
    returnType: String? = null,
    parameterTypes: List<String>? = null
): Int {
    return methodCallIndices(definingClass, name, returnType, parameterTypes).firstOrNull() ?: -1
}

internal fun MutableMethod.methodCallIndices(
    definingClass: String,
    name: String,
    returnType: String? = null,
    parameterTypes: List<String>? = null
): List<Int> {
    val instructions = implementation?.instructions ?: return emptyList()
    return instructions.mapIndexedNotNull { index, instruction ->
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            ?: return@mapIndexedNotNull null
        if (
            reference.definingClass == definingClass &&
            reference.name == name &&
            (returnType == null || reference.returnType == returnType) &&
            (parameterTypes == null || reference.parameterTypes.toList() == parameterTypes)
        ) {
            index
        } else {
            null
        }
    }
}

internal fun MutableMethod.indexOfFirstMoveResultAfter(instructionIndex: Int): Int {
    val instructions = implementation?.instructions ?: return -1
    for (index in (instructionIndex + 1) until instructions.size) {
        if (instructions[index].isOpcode("MOVE_RESULT") ||
            instructions[index].isOpcode("MOVE_RESULT_OBJECT") ||
            instructions[index].isOpcode("MOVE_RESULT_WIDE")) {
            return index
        }
    }
    return -1
}

internal fun MutableMethod.indexOfFirstConst4LiteralFollowedByIfEqz(literal: Int): Int {
    val instructions = implementation?.instructions ?: return -1
    for (index in 0 until instructions.lastIndex) {
        val instruction = instructions[index]
        if (instruction.isOpcode("CONST_4") &&
            instruction.isLiteralWrite(
                register = (instruction as? OneRegisterInstruction)?.registerA ?: continue,
                literal = literal.toLong(),
            ) &&
            instructions[index + 1].isOpcode("IF_EQZ")
        ) {
            return index
        }
    }
    return -1
}

internal fun MutableMethod.indexOfFirstInstructionWritingRegister(register: Int): Int {
    val instructions = implementation?.instructions ?: return -1
    return instructions.indexOfFirst { instruction ->
        (instruction as? OneRegisterInstruction)?.registerA == register
    }
}

internal fun MutableMethod.instructionIndices(opcodeName: String): List<Int> {
    val instructions = implementation?.instructions ?: return emptyList()
    return instructions.mapIndexedNotNull { index, instruction ->
        if (instruction.isOpcode(opcodeName)) {
            index
        } else {
            null
        }
    }
}

internal fun MutableMethod.returnInstructionIndices(): List<Int> {
    val instructions = implementation?.instructions ?: return emptyList()
    return instructions.indices.filter { index ->
        instructions[index].isOpcode("RETURN") ||
            instructions[index].isOpcode("RETURN_OBJECT") ||
            instructions[index].isOpcode("RETURN_VOID") ||
            instructions[index].isOpcode("RETURN_WIDE")
    }
}

internal fun MutableMethod.indexOfFirstFieldAccess(
    definingClass: String,
    name: String,
    type: String,
    opcodeName: String? = null
): Int {
    val instructions = implementation?.instructions ?: return -1
    return instructions.indexOfFirst { instruction ->
        val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference ?: return@indexOfFirst false
        reference.definingClass == definingClass &&
            reference.name == name &&
            reference.type == type &&
            (opcodeName == null || instruction.isOpcode(opcodeName))
    }
}
