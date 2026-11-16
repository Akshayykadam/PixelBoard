package com.akshaykadam.pixelboard.patches.gboard.shared

import com.akshaykadam.pixelboard.patches.shared.BytecodePatchContext
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import java.io.ByteArrayInputStream
import java.io.InputStream

private fun findMethodByPrefix(clazz: Class<*>, prefix: String, vararg paramTypes: Class<*>): java.lang.reflect.Method {
    return (clazz.methods + clazz.declaredMethods).firstOrNull { method ->
        method.name.startsWith(prefix) && method.parameterTypes.contentEquals(paramTypes)
    } ?: error("Method starting with '$prefix' not found on $clazz")
}

internal val gboardPatchesExtensionCarrierPatch = bytecodePatch(
    description = "Merge shared Gboard patches runtime extension into target APK."
) {
    execute {
        mergeBundledExtension(this, "extensions/pixelboard-patches.rve")
    }
}

internal fun mergeBundledExtension(context: BytecodePatchContext, extension: String) {
    val resourceClassLoader = object {}.javaClass.classLoader
    val runtimeClassLoader = context.javaClass.classLoader
    val extensionStream = resourceClassLoader.getResourceAsStream(extension)
        ?: error("Extension \"$extension\" not found")
    try {
        val extensionBytes = extensionStream.readBytes()
        val patchClassesMethod = findMethodByPrefix(context.javaClass, "getPatchClasses")
        val patchClasses = patchClassesMethod.invoke(context)
        val classDefClass = loadRuntimeClass(runtimeClassLoader, "com.android.tools.smali.dexlib2.iface.ClassDef")
        val extensionDex = loadExtensionDex(context, runtimeClassLoader, extensionBytes, extension)
        val getClassesMethod = extensionDex.javaClass.getMethod("getClasses")
        val extensionClasses = getClassesMethod.invoke(extensionDex) as Iterable<*>
        val classByOrNullMethod = patchClasses.javaClass.getMethod("classByOrNull", String::class.java)
        val addClassMethod = findMethodByPrefix(patchClasses.javaClass, "addClass", classDefClass)
        val mergerName = listOf("app", "mor" + "phe", "patcher", "util", "ClassMerger").joinToString(".")
        val classMergerClass = loadRuntimeClass(runtimeClassLoader, mergerName)
        val classMerger = classMergerClass.getField("INSTANCE").get(null)
        val mergeMethod = classMergerClass.getMethod(
            "merge",
            classDefClass,
            classDefClass,
            context.javaClass
        )

        for (classDef in extensionClasses) {
            if (classDef == null) {
                continue
            }
            val classType = classDefClass.getMethod("getType").invoke(classDef) as String
            val existingClass = classByOrNullMethod.invoke(patchClasses, classType)
            if (existingClass == null) {
                addClassMethod.invoke(patchClasses, classDef)
                continue
            }

            val mergedClass = mergeMethod.invoke(classMerger, existingClass, classDef, context)
            if (mergedClass !== existingClass) {
                addClassMethod.invoke(patchClasses, mergedClass)
            }
        }
    } finally {
        extensionStream.close()
    }
}

private fun loadExtensionDex(
    context: BytecodePatchContext,
    runtimeClassLoader: ClassLoader,
    extensionBytes: ByteArray,
    extension: String
): Any {
    val opcodes = findMethodByPrefix(context.javaClass, "getOpcodes").invoke(context)
    val opcodesClass = loadRuntimeClass(runtimeClassLoader, "com.android.tools.smali.dexlib2.Opcodes")
    val rawDexIoClass = runCatching {
        loadRuntimeClass(runtimeClassLoader, "lanchon.multidexlib2.RawDexIO")
    }.getOrNull()
    if (rawDexIoClass != null) {
        val readRawDexFileMethod = runCatching {
            rawDexIoClass.getMethod("readRawDexFile", ByteArray::class.java, Integer.TYPE, opcodesClass)
        }.getOrNull()
        if (readRawDexFileMethod != null) {
            return readRawDexFileMethod.invoke(null, extensionBytes, 0, opcodes)
                ?: error("Failed to decode bundled extension dex: $extension")
        }
    }

    val dexBackedDexFileClass = loadRuntimeClass(
        runtimeClassLoader,
        "com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile"
    )
    val fromInputStreamMethod = dexBackedDexFileClass.getMethod(
        "fromInputStream",
        opcodesClass,
        InputStream::class.java
    )
    return ByteArrayInputStream(extensionBytes).use { dexStream ->
        fromInputStreamMethod.invoke(null, opcodes, dexStream)
            ?: error("Failed to decode bundled extension dex via dexlib2 fallback: $extension")
    }
}

private fun loadRuntimeClass(runtimeClassLoader: ClassLoader, name: String): Class<*> =
    Class.forName(name, false, runtimeClassLoader)
