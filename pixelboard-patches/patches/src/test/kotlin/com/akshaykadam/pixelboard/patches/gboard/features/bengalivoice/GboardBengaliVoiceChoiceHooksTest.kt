package com.akshaykadam.pixelboard.patches.gboard.features.bengalivoice

import com.akshaykadam.pixelboard.patches.gboard.shared.invokeRegisters
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.gboard.shared.semanticShape
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GboardBengaliVoiceChoiceHooksTest {
    @Test
    fun `language settings hook uses p0 range at v18 and is idempotent`() {
        val method = fixture("aU", 19, "V", false, "return-void")

        method.installBengaliLanguageSettingsRow()

        val instructions = method.implementation!!.instructions
        val reference = reference(RuntimeCallId.BENGALI_VOICE_AFTER_LANGUAGE_SETTINGS_REBUILT)
        assertTrue(instructions[0].isOpcode("INVOKE_STATIC_RANGE"))
        assertEquals(listOf(18), instructions[0].invokeRegisters(reference))
        assertTrue(instructions[1].isOpcode("RETURN_VOID"))
        val beforeRepeat = instructions.map { it.semanticShape() }
        method.installBengaliLanguageSettingsRow()
        assertEquals(beforeRepeat, method.implementation!!.instructions.map { it.semanticShape() })
    }

    @Test
    fun `selector hook follows global observer and preserves overwritten p0 boolean`() {
        val observer = reference(RuntimeCallId.RAMBLER_RUNTIME_UPDATE_OFFICIAL_SELECTION)
        val method = fixture("a", 3, "Z", true, """
            const/4 p0, 0x1
            invoke-static {p0}, $observer
            return p0
        """.trimIndent())

        method.installBengaliLanguageSelectionOverride()

        val instructions = method.implementation!!.instructions
        val reference = reference(RuntimeCallId.BENGALI_VOICE_AFTER_NATIVE_SELECTION)
        assertTrue(instructions[1].isMethodReference(observer))
        assertEquals(listOf(2), instructions[2].invokeRegisters(reference))
        assertTrue(instructions[3].isOpcode("MOVE_RESULT"))
        assertEquals(2, (instructions[3] as OneRegisterInstruction).registerA)
        assertTrue(instructions[4].isOpcode("RETURN"))
        assertEquals(2, (instructions[4] as OneRegisterInstruction).registerA)
        val beforeRepeat = instructions.map { it.semanticShape() }
        method.installBengaliLanguageSelectionOverride()
        assertEquals(beforeRepeat, method.implementation!!.instructions.map { it.semanticShape() })
    }

    @Test
    fun `selector hook rejects missing global observer without mutation`() {
        val method = fixture("a", 3, "Z", true, "const/4 p0, 0x1\nreturn p0")
        val before = method.implementation!!.instructions.map { it.semanticShape() }

        try {
            method.installBengaliLanguageSelectionOverride()
            fail("Expected a missing-observer failure")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("global-selection observer"))
        }

        assertEquals(before, method.implementation!!.instructions.map { it.semanticShape() })
    }

    @Test
    fun `microphone hook implements both overrides and preserves both native results`() {
        for (nativeResult in 0..1) {
            val method = fixture("B", 4, "Z", true,
                "const/4 v0, 0x$nativeResult\nreturn v0")
            method.installBengaliLanguageMicOverride()
            assertEquals(nativeResult, executeMicFixture(method, 0))
            assertEquals(0, executeMicFixture(method, 1))
            assertEquals(1, executeMicFixture(method, 2))
            val beforeRepeat = method.implementation!!.instructions.map { it.semanticShape() }
            method.installBengaliLanguageMicOverride()
            assertEquals(beforeRepeat, method.implementation!!.instructions.map { it.semanticShape() })
        }
    }

    private fun fixture(name: String, registerCount: Int, returnType: String,
            isStatic: Boolean, body: String): MutableMethod {
        val parameters = if (isStatic) {
            listOf(ImmutableMethodParameter("Landroid/content/Context;", emptySet(), null))
        } else emptyList()
        return MutableMethod(ImmutableMethod(
            "Ltest/BengaliNativeFixture;", name, parameters, returnType,
            AccessFlags.PUBLIC.value or if (isStatic) AccessFlags.STATIC.value else 0,
            emptySet(), emptySet(),
            ImmutableMethodImplementation(registerCount, emptyList(), emptyList(), emptyList()),
        )).also { it.addInstructions(0, body) }
    }

    /** Executes the generated branch/arithmetic subset, including its real assembled offsets. */
    private fun executeMicFixture(method: MutableMethod, languageRoute: Int): Int {
        val instructions = method.implementation!!.instructions
        val addressToIndex = mutableMapOf<Int, Int>()
        val addresses = IntArray(instructions.size)
        var address = 0
        instructions.forEachIndexed { index, instruction ->
            addresses[index] = address
            addressToIndex[address] = index
            address += instruction.codeUnits
        }
        val registers = IntArray(method.implementation!!.registerCount)
        var result = 0
        var index = 0
        repeat(50) {
            val instruction = instructions[index]
            when {
                instruction.isMethodReference(reference(RuntimeCallId.BENGALI_VOICE_ACTIVE_ROUTE_OVERRIDE)) ->
                    result = languageRoute
                instruction.isOpcode("MOVE_RESULT") ->
                    registers[(instruction as OneRegisterInstruction).registerA] = result
                instruction.isOpcode("IF_EQZ") -> {
                    val register = (instruction as OneRegisterInstruction).registerA
                    if (registers[register] == 0) {
                        val target = addresses[index] + (instruction as OffsetInstruction).codeOffset
                        index = checkNotNull(addressToIndex[target]) { "Branch target is not an instruction" }
                        return@repeat
                    }
                }
                instruction.isOpcode("ADD_INT_LIT8") -> {
                    val operands = instruction as TwoRegisterInstruction
                    registers[operands.registerA] = registers[operands.registerB] +
                        (instruction as NarrowLiteralInstruction).narrowLiteral
                }
                instruction.isOpcode("CONST_4") ->
                    registers[(instruction as OneRegisterInstruction).registerA] =
                        (instruction as NarrowLiteralInstruction).narrowLiteral
                instruction.isOpcode("NOP") -> Unit
                instruction.isOpcode("RETURN") ->
                    return registers[(instruction as OneRegisterInstruction).registerA]
                else -> error("Unexpected fixture instruction ${instruction.opcode}")
            }
            index++
        }
        error("Microphone fixture did not return")
    }

    private fun reference(call: RuntimeCallId): String = RuntimeAbiCatalog.abi(call).reference
}
