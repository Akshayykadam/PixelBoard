package com.akshaykadam.pixelboard.tools.bindings

import java.nio.charset.StandardCharsets
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

object TargetBindingGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 5) {
            "Expected bindings, catalog, optional PortBundle, bindings output, and admission output"
        }
        val profile = Path.of(args[0])
        val productCatalog = Path.of(args[1])
        val portBundle = args[2].takeUnless { it == "-" }?.let(Path::of)
        val bindingsOutput = Path.of(args[3])
        val admissionOutput = Path.of(args[4])
        val compiled = GboardTargetBuildCompiler.compile(
            catalogText = Files.readString(productCatalog, StandardCharsets.UTF_8),
            portBundleText = portBundle?.let { Files.readString(it, StandardCharsets.UTF_8) },
            reviewedBindingsText = Files.readString(profile, StandardCharsets.UTF_8),
        )
        Files.createDirectories(bindingsOutput.parent)
        Files.createDirectories(admissionOutput.parent)
        writePairAtomically(
            listOf(
                bindingsOutput to compiled.bindingsKotlinSource,
                admissionOutput to compiled.admissionKotlinSource,
            ),
        )
    }

    fun writePairAtomically(
        outputs: List<Pair<Path, String>>,
        atomicMove: AtomicMove = NIO_ATOMIC_MOVE,
    ) {
        val staged = outputs.associate { (output, source) ->
            output to Files.createTempFile(output.parent, output.fileName.toString(), ".staged").also {
                Files.writeString(it, source, StandardCharsets.UTF_8)
            }
        }
        val backups = mutableMapOf<Path, Path>()
        val installed = mutableSetOf<Path>()
        val retainedBackups = mutableSetOf<Path>()
        try {
            outputs.forEach { (output, _) ->
                if (Files.exists(output)) {
                    val backup = Files.createTempFile(output.parent, output.fileName.toString(), ".backup")
                    Files.delete(backup)
                    atomicMove.move(output, backup, ATOMIC_MOVE)
                    backups[output] = backup
                }
            }
            outputs.forEach { (output, _) ->
                atomicMove.move(staged.getValue(output), output, ATOMIC_MOVE, REPLACE_EXISTING)
                installed.add(output)
            }
        } catch (error: Exception) {
            installed.forEach { output ->
                try {
                    Files.deleteIfExists(output)
                } catch (rollbackError: Exception) {
                    error.addSuppressed(rollbackError)
                }
            }
            backups.forEach { (output, backup) ->
                if (Files.exists(backup)) {
                    try {
                        atomicMove.move(backup, output, ATOMIC_MOVE, REPLACE_EXISTING)
                    } catch (rollbackError: Exception) {
                        retainedBackups.add(backup)
                        error.addSuppressed(rollbackError)
                    }
                }
            }
            throw error
        } finally {
            staged.values.forEach(Files::deleteIfExists)
            backups.values.filterNot(retainedBackups::contains).forEach(Files::deleteIfExists)
        }
    }

    fun interface AtomicMove {
        fun move(source: Path, target: Path, vararg options: CopyOption)
    }

    private val NIO_ATOMIC_MOVE = AtomicMove { source, target, options ->
        Files.move(source, target, *options)
    }
}
