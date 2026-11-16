package com.akshaykadam.pixelboard.patches.gboard.shared

import com.akshaykadam.pixelboard.patches.shared.BytecodePatchContext
import com.akshaykadam.pixelboard.patches.shared.MutableClass
import com.akshaykadam.pixelboard.patches.shared.MutableField
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal class GboardMethodTarget(
    classType: String,
    val name: String,
    parameterTypes: List<String>,
    returnType: String,
) {
    private val owner = classType
    val parameterTypes: List<String> = parameterTypes.toList()
    val returnType: String = returnType
    val descriptor: String = "(${this.parameterTypes.joinToString("")})${this.returnType}"
    val reference: String = "$owner->$name$descriptor"
    val ownerDescriptor: String get() = owner

    fun resolve(context: BytecodePatchContext): MutableMethod =
        resolve(context.mutableClass(owner))

    fun resolveOrNull(context: BytecodePatchContext): MutableMethod? {
        val ownerClass = context.mutableClassDefByOrNull(owner) ?: return null
        return ownerClass.methods.firstOrNull(::matches)
    }

    fun resolve(ownerClass: MutableClass): MutableMethod = ownerClass.methods.firstOrNull(::matches)
        ?: error("Could not find $reference")

    fun ownerClass(context: BytecodePatchContext): MutableClass = context.mutableClass(owner)

    fun matches(method: MethodReference): Boolean =
        method.definingClass == owner &&
            method.name == name &&
            method.parameterTypes == parameterTypes &&
            method.returnType == returnType

    fun referenceNamed(replacementName: String): String = "$owner->$replacementName$descriptor"

    fun installHelper(
        context: BytecodePatchContext,
        name: String,
        accessFlags: Int,
        registerCount: Int,
        body: String,
    ) {
        context.addHelperMethodIfMissing(
            classType = owner,
            name = name,
            parameterTypes = parameterTypes,
            returnType = returnType,
            accessFlags = accessFlags,
            registerCount = registerCount,
            body = body,
        )
    }

}

internal class GboardFieldTarget(
    classType: String,
    val name: String,
    val type: String,
) {
    private val owner = classType
    val reference: String = "$owner->$name:$type"
    val ownerDescriptor: String get() = owner

    fun resolve(context: BytecodePatchContext): MutableField =
        context.mutableClass(owner).fields.firstOrNull(::matches)
            ?: error("Could not find $reference")

    fun resolveOrNull(context: BytecodePatchContext): MutableField? {
        val ownerClass = context.mutableClassDefByOrNull(owner) ?: return null
        return ownerClass.fields.firstOrNull(::matches)
    }

    fun matches(field: FieldReference): Boolean =
        field.definingClass == owner && field.name == name && field.type == type

}

@JvmInline
internal value class GboardTypeTarget(val descriptor: String)
