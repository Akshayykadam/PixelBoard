package com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RuntimeCallEmitterTest {
    @Test
    fun `emits an exact static invocation from a semantic call id`() {
        assertEquals(
            "invoke-static {}, " +
                "Lcom/akshaykadam/pixelboard/extension/rambler/" +
                "GboardRambler1803OfficialSelectionRuntime;->enterVoiceSettingsScope()V",
            RuntimeCallEmitter.invoke(
                RuntimeCallId.RAMBLER_RUNTIME_ENTER_VOICE_SETTINGS_SCOPE,
                registers = "",
            ),
        )
    }

    @Test
    fun `emits a static range invocation without exposing an opcode string`() {
        assertEquals(
            "invoke-static/range {v4 .. v4}, " +
                "Lcom/akshaykadam/pixelboard/extension/advancedvoice/" +
                "GboardAdvancedVoice1803Runtime;->seedApplicationContext(Landroid/content/Context;)V",
            RuntimeCallEmitter.invoke(
                RuntimeCallId.ADVANCED_VOICE_RUNTIME_SEED_APPLICATION_CONTEXT,
                registers = "v4 .. v4",
            ),
        )
    }

    @Test
    fun `selects range encoding when a contiguous expression cannot fit 35c`() {
        val abi = RuntimeAbi.decode("Lexample/Runtime;->call(IIIIII)V")

        assertEquals(
            "invoke-static/range {v12 .. v17}, Lexample/Runtime;->call(IIIIII)V",
            RuntimeCallEmitter.invoke(abi, "v12, v13, v14, v15, v16, v17"),
        )
    }

    @Test
    fun `rejects a register expression with the wrong argument word count`() {
        val abi = RuntimeAbi.decode("Lexample/Runtime;->call(JI)V")

        assertThrows(IllegalArgumentException::class.java) {
            RuntimeCallEmitter.invoke(abi, "v0, v1")
        }
    }

    @Test
    fun `rejects noncontiguous words for a wide argument`() {
        val abi = RuntimeAbi.decode("Lexample/Runtime;->call(JI)V")

        assertThrows(IllegalArgumentException::class.java) {
            RuntimeCallEmitter.invoke(abi, "v0, v2, v3")
        }
    }

    @Test
    fun `rejects a noncontiguous expression outside the 35c register limit`() {
        val abi = RuntimeAbi.decode("Lexample/Runtime;->call(II)V")

        assertThrows(IllegalArgumentException::class.java) {
            RuntimeCallEmitter.invoke(abi, "v16, v18")
        }
    }

    @Test
    fun `accepts explicit contiguous words for wide parameters`() {
        val abi = RuntimeAbi.decode("Lexample/Runtime;->call(JD)V")

        assertEquals(
            "invoke-static {v0, v1, v2, v3}, Lexample/Runtime;->call(JD)V",
            RuntimeCallEmitter.invoke(abi, "v0, v1, v2, v3"),
        )
    }
}
