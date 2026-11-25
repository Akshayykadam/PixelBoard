// SPDX-License-Identifier: GPL-3.0-only
package com.akshaykadam.pixelboard.patches.gboard.features.rambler

import com.akshaykadam.pixelboard.patches.shared.toMutable
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import org.junit.Assert.*
import org.junit.Test

class GboardMicGesturePatchTest {
    private fun lifecycle() = ImmutableMethod(
        "Lpbb;", "onFinishInput", emptyList(), "V", AccessFlags.PUBLIC.value,
        null, null, ImmutableMethodImplementation(
            25, listOf(BuilderInstruction10x(Opcode.RETURN_VOID)), emptyList(), emptyList(),
        ),
    ).toMutable()

    @Test fun entryHookPreservesNativeReturnAndSupportsHighParameterRegisters() {
        val method = lifecycle()
        method.applyMicGestureEntry(RuntimeCallId.MIC_GESTURE_RESET_SESSION, "p0 .. p0")
        val instructions = method.implementation!!.instructions
        assertEquals(Opcode.INVOKE_STATIC_RANGE, instructions.first().opcode)
        assertEquals(24, (instructions.first() as RegisterRangeInstruction).startRegister)
        assertEquals(Opcode.RETURN_VOID, instructions.last().opcode)
        assertEquals(25, method.implementation!!.registerCount)
    }

    @Test fun reapplyingLifecycleHookNeverDuplicatesDispatch() {
        val method = lifecycle()
        repeat(2) { method.applyMicGestureEntry(RuntimeCallId.MIC_GESTURE_RESET_SESSION, "p0 .. p0") }
        assertEquals(2, method.implementation!!.instructions.size)
    }

    @Test(expected = IllegalStateException::class)
    fun partialHookAtWrongPositionIsRejected() {
        val method = lifecycle()
        method.applyMicGestureEntry(RuntimeCallId.MIC_GESTURE_RESET_SESSION, "p0 .. p0")
        method.implementation!!.addInstruction(0, BuilderInstruction10x(Opcode.NOP))
        method.applyMicGestureEntry(RuntimeCallId.MIC_GESTURE_RESET_SESSION, "p0 .. p0")
    }
}
