package com.akshaykadam.pixelboard.util

import com.akshaykadam.pixelboard.patches.shared.loadPatchesFromJar
import com.akshaykadam.pixelboard.patches.gboard.registry.GboardPublishedPatchCatalog
import java.io.File
import java.net.URLClassLoader
import java.util.jar.Manifest

internal fun main() {
    val patchBundle = resolvePatchBundleFile()
    val patchFiles = setOf(patchBundle)
    val loadedPatchNames = loadPatchesFromJar(patchFiles).mapNotNull { patch -> patch.name }.toSet()
    val catalogPatchNames = GboardPublishedPatchCatalog.publishedRegistrations
        .mapNotNull { patch -> patch.name }
        .toSet()
    require(loadedPatchNames == catalogPatchNames) {
        "Published catalog does not match published registrations in ${patchBundle.name}"
    }
    val patchClassLoader = URLClassLoader(patchFiles.map { it.toURI().toURL() }.toTypedArray())
    val manifest = patchClassLoader.getResources("META-INF/MANIFEST.MF")

    while (manifest.hasMoreElements()) {
        Manifest(manifest.nextElement().openStream())
            .mainAttributes
            .getValue("Version")
            ?.let {
                generatePatchList(it)
            }
    }
}

private fun resolvePatchBundleFile(): File {
    val buildLibs = File("build/libs/")
    val patchBundles = buildLibs.listFiles { file ->
        val fileName = file.name
        !fileName.contains("javadoc") &&
            !fileName.contains("sources") &&
            fileName.endsWith(".mpp")
    }?.toList().orEmpty()

    require(patchBundles.isNotEmpty()) { "No patch bundle found under ${buildLibs.path}" }

    val currentVersion = Regex("""(?m)^\s*version\s*=\s*([^\r\n#]+?)\s*$""")
        .find(File("../gradle.properties").readText())
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()

    return patchBundles.firstOrNull { bundle ->
        currentVersion != null && bundle.name == "patches-$currentVersion.mpp"
    } ?: patchBundles.maxByOrNull(File::lastModified)
    ?: error("Unable to resolve patch bundle from ${buildLibs.path}")
}

private fun generatePatchList(version: String) {
    val listJson = File("../patches-list.json")
    listJson.writeText(GboardPublishedPatchCatalog.publishedInventory(version))
}
