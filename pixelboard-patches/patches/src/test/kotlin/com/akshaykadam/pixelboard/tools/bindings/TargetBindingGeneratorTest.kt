package com.akshaykadam.pixelboard.tools.bindings

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TargetBindingGeneratorTest {
    @Test
    fun `failure while backing up second output preserves both originals`() {
        assertOriginalPairSurvives(failOnMove = 2)
    }

    @Test
    fun `failure while installing second output restores both originals`() {
        assertOriginalPairSurvives(failOnMove = 4)
    }

    @Test
    fun `failure while restoring original retains identifiable recovery backup`() {
        val directory = Files.createTempDirectory("gboard-binding-pair")
        val bindings = directory.resolve("GboardVersionBindings.kt")
        val admission = directory.resolve("GboardTargetAdmission.kt")
        Files.writeString(bindings, OLD_BINDINGS, StandardCharsets.UTF_8)
        Files.writeString(admission, OLD_ADMISSION, StandardCharsets.UTF_8)
        var moveCount = 0
        val failingMove = TargetBindingGenerator.AtomicMove { source, target, options ->
            moveCount += 1
            if (moveCount == 4 || moveCount == 5) {
                throw IOException("Injected move failure #$moveCount")
            }
            Files.move(source, target, *options)
        }

        val failure = assertThrows(IOException::class.java) {
            TargetBindingGenerator.writePairAtomically(
                outputs = listOf(bindings to NEW_BINDINGS, admission to NEW_ADMISSION),
                atomicMove = failingMove,
            )
        }

        assertEquals("Injected move failure #4", failure.message)
        assertEquals(listOf("Injected move failure #5"), failure.suppressed.map { it.message })
        assertEquals(OLD_ADMISSION, Files.readString(admission, StandardCharsets.UTF_8))
        val recoveryBackups = regularFiles(directory).filter { path ->
            path.fileName.toString().startsWith(bindings.fileName.toString()) &&
                path.fileName.toString().endsWith(".backup")
        }
        assertEquals(1, recoveryBackups.size)
        assertEquals(OLD_BINDINGS, Files.readString(recoveryBackups.single(), StandardCharsets.UTF_8))
    }

    private fun assertOriginalPairSurvives(failOnMove: Int) {
        val directory = Files.createTempDirectory("gboard-binding-pair")
        val bindings = directory.resolve("GboardVersionBindings.kt")
        val admission = directory.resolve("GboardTargetAdmission.kt")
        Files.writeString(bindings, OLD_BINDINGS, StandardCharsets.UTF_8)
        Files.writeString(admission, OLD_ADMISSION, StandardCharsets.UTF_8)
        var moveCount = 0
        val failingMove = TargetBindingGenerator.AtomicMove { source, target, options ->
            moveCount += 1
            if (moveCount == failOnMove) throw IOException("Injected move failure #$moveCount")
            Files.move(source, target, *options)
        }

        assertThrows(IOException::class.java) {
            TargetBindingGenerator.writePairAtomically(
                outputs = listOf(bindings to NEW_BINDINGS, admission to NEW_ADMISSION),
                atomicMove = failingMove,
            )
        }

        assertEquals(OLD_BINDINGS, Files.readString(bindings, StandardCharsets.UTF_8))
        assertEquals(OLD_ADMISSION, Files.readString(admission, StandardCharsets.UTF_8))
        assertEquals(setOf(bindings, admission), regularFiles(directory))
    }

    private fun regularFiles(directory: Path): Set<Path> = Files.list(directory).use { paths ->
        paths.filter(Files::isRegularFile).collect(java.util.stream.Collectors.toSet())
    }

    private companion object {
        const val OLD_BINDINGS = "old bindings"
        const val OLD_ADMISSION = "old admission"
        const val NEW_BINDINGS = "new bindings"
        const val NEW_ADMISSION = "new admission"
    }
}
