package com.akshaykadam.pixelboard.patches.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction as pixelboardAddInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions as pixelboardAddInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels as pixelboardAddInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction as pixelboardReplaceInstruction
import app.morphe.patcher.patch.AppTarget as PixelBoardAppTarget
import app.morphe.patcher.patch.BytecodePatch as PixelBoardBytecodePatch
import app.morphe.patcher.patch.BytecodePatchBuilder as PixelBoardBytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext as PixelBoardBytecodePatchContext
import app.morphe.patcher.patch.Compatibility as PixelBoardCompatibility
import app.morphe.patcher.patch.Option as PixelBoardOption
import app.morphe.patcher.patch.Patch as PixelBoardPatch
import app.morphe.patcher.patch.PatchBuilder as PixelBoardPatchBuilder
import app.morphe.patcher.patch.ResourcePatch as PixelBoardResourcePatch
import app.morphe.patcher.patch.ResourcePatchBuilder as PixelBoardResourcePatchBuilder
import app.morphe.patcher.patch.ResourcePatchContext as PixelBoardResourcePatchContext
import app.morphe.patcher.patch.bytecodePatch as pixelboardBytecodePatch
import app.morphe.patcher.patch.loadPatchesFromJar as pixelboardLoadPatchesFromJar
import app.morphe.patcher.patch.resourcePatch as pixelboardResourcePatch
import app.morphe.patcher.patch.stringOption as pixelboardStringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass as PixelBoardMutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableField as PixelBoardMutableField
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable as pixelboardFieldToMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod as PixelBoardMutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable as pixelboardMethodToMutable
import app.morphe.patcher.util.smali.ExternalLabel as PixelBoardExternalLabel
import com.android.tools.smali.dexlib2.iface.Field as DexlibField
import com.android.tools.smali.dexlib2.iface.Method as DexlibMethod
import java.io.File

typealias Patch<T> = PixelBoardPatch<T>
typealias BytecodePatch = PixelBoardBytecodePatch
typealias BytecodePatchBuilder = PixelBoardBytecodePatchBuilder
typealias BytecodePatchContext = PixelBoardBytecodePatchContext
typealias ResourcePatch = PixelBoardResourcePatch
typealias ResourcePatchBuilder = PixelBoardResourcePatchBuilder
typealias ResourcePatchContext = PixelBoardResourcePatchContext
typealias Compatibility = PixelBoardCompatibility
typealias AppTarget = PixelBoardAppTarget
typealias MutableClass = PixelBoardMutableClass
typealias MutableField = PixelBoardMutableField
typealias MutableMethod = PixelBoardMutableMethod
typealias ExternalLabel = PixelBoardExternalLabel
typealias Option<T> = PixelBoardOption<T>
typealias PatchBuilder<T> = PixelBoardPatchBuilder<T>

inline fun resourcePatch(
    name: String? = null,
    description: String? = null,
    default: Boolean = false,
    noinline block: ResourcePatchBuilder.() -> Unit = {},
): ResourcePatch = pixelboardResourcePatch(name, description, default, block)

inline fun bytecodePatch(
    name: String? = null,
    description: String? = null,
    default: Boolean = false,
    noinline block: BytecodePatchBuilder.() -> Unit = {},
): BytecodePatch = pixelboardBytecodePatch(name, description, default, block)

inline fun PatchBuilder<*>.stringOption(
    key: String,
    default: String? = null,
    values: Map<String, String>? = null,
    title: String? = null,
    description: String? = null,
    required: Boolean = false,
    noinline validator: Option<String>.(String?) -> Boolean = { true },
): Option<String> = pixelboardStringOption(key, default, values, title, description, required, validator)

inline fun MutableMethod.addInstruction(index: Int, instruction: String) =
    pixelboardAddInstruction(index, instruction)

inline fun MutableMethod.addInstructions(index: Int, instructions: String) =
    pixelboardAddInstructions(index, instructions)

inline fun MutableMethod.addInstructionsWithLabels(index: Int, instructions: String, vararg labels: ExternalLabel) =
    pixelboardAddInstructionsWithLabels(index, instructions, *labels)

inline fun MutableMethod.replaceInstruction(index: Int, instruction: String) =
    pixelboardReplaceInstruction(index, instruction)

inline fun DexlibField.toMutable(): MutableField =
    pixelboardFieldToMutable()

inline fun DexlibMethod.toMutable(): MutableMethod =
    pixelboardMethodToMutable()

inline fun loadPatchesFromJar(patchFiles: Set<File>) =
    pixelboardLoadPatchesFromJar(patchFiles)
