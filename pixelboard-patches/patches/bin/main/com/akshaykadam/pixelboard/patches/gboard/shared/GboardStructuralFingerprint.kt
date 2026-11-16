package com.akshaykadam.pixelboard.patches.gboard.shared

import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.android.tools.smali.dexlib2.iface.instruction.DualReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FieldOffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.InlineIndexInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.SwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.VerificationErrorInstruction
import com.android.tools.smali.dexlib2.iface.instruction.VtableIndexInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.ArrayPayload
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal fun MutableMethod.gboardStructuralFingerprint(
    startInstructionIndex: Int = 0,
): String {
    val implementation = implementation ?: error("No instructions in $definingClass->$name")
    val instructions = implementation.instructions
    require(startInstructionIndex in 0..instructions.size) {
        "Invalid fingerprint start index $startInstructionIndex for $definingClass->$name"
    }
    val prefixCodeUnits = instructions.take(startInstructionIndex).sumOf { it.codeUnits }
    val canonical = buildString {
        append("registers=").append(implementation.registerCount)
        instructions.drop(startInstructionIndex).forEachIndexed { index, instruction ->
            append('\n').append(index).append(':').append(instruction.opcode.name)
            when (instruction) {
                is RegisterRangeInstruction -> append("|range=")
                    .append(instruction.startRegister).append(',').append(instruction.registerCount)
                is FiveRegisterInstruction -> append("|registers=")
                    .append(instruction.registerCount).append(',')
                    .append(instruction.registerC).append(',').append(instruction.registerD)
                    .append(',').append(instruction.registerE).append(',')
                    .append(instruction.registerF).append(',').append(instruction.registerG)
                is ThreeRegisterInstruction -> append("|registers=")
                    .append(instruction.registerA).append(',').append(instruction.registerB)
                    .append(',').append(instruction.registerC)
                is TwoRegisterInstruction -> append("|registers=")
                    .append(instruction.registerA).append(',').append(instruction.registerB)
                is OneRegisterInstruction -> append("|register=").append(instruction.registerA)
            }
            if (instruction is ReferenceInstruction) {
                append("|reference=").append(instruction.reference)
            }
            if (instruction is DualReferenceInstruction) {
                append("|reference2=").append(instruction.reference2)
            }
            if (instruction is WideLiteralInstruction) {
                append("|literal=").append(instruction.wideLiteral)
            }
            if (instruction is OffsetInstruction) {
                append("|offset=").append(instruction.codeOffset)
            }
            if (instruction is VerificationErrorInstruction) {
                append("|verification=").append(instruction.verificationError)
            }
            if (instruction is FieldOffsetInstruction) {
                append("|fieldOffset=").append(instruction.fieldOffset)
            }
            if (instruction is VtableIndexInstruction) {
                append("|vtableIndex=").append(instruction.vtableIndex)
            }
            if (instruction is InlineIndexInstruction) {
                append("|inlineIndex=").append(instruction.inlineIndex)
            }
            if (instruction is SwitchPayload) {
                append("|switch=")
                instruction.switchElements.forEach { element ->
                    append(element.key).append('@').append(element.offset).append(',')
                }
            }
            if (instruction is ArrayPayload) {
                append("|array=").append(instruction.elementWidth).append(':')
                instruction.arrayElements.forEach { element -> append(element).append(',') }
            }
        }
        implementation.tryBlocks
            .filter { it.startCodeAddress >= prefixCodeUnits }
            .forEach { tryBlock ->
            append("\ntry=").append(tryBlock.startCodeAddress - prefixCodeUnits).append(',')
                .append(tryBlock.codeUnitCount).append(':')
            tryBlock.exceptionHandlers.forEach { handler ->
                append(handler.exceptionType).append('@')
                    .append(handler.handlerCodeAddress - prefixCodeUnits)
                    .append(',')
            }
        }
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}
