package com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi

internal fun interface RuntimeArtifactInventory {
    fun methods(owner: String, name: String): List<RuntimeArtifactMethod>
}

internal data class RuntimeArtifactMethod(
    val owner: String,
    val name: String,
    val descriptor: String,
    val isStatic: Boolean,
)

internal sealed interface RuntimeAbiMismatch {
    data class MissingMethod(val expected: RuntimeAbi) : RuntimeAbiMismatch

    data class WrongDescriptor(
        val expected: RuntimeAbi,
        val actualDescriptors: List<String>,
    ) : RuntimeAbiMismatch

    data class WrongStaticAccess(
        val expected: RuntimeAbi,
        val actualIsStatic: Boolean,
    ) : RuntimeAbiMismatch
}

internal object RuntimeAbiVerifier {
    fun verify(
        calls: Iterable<RuntimeCallId>,
        inventory: RuntimeArtifactInventory,
    ): List<RuntimeAbiMismatch> = calls.mapNotNull { call ->
        val expected = RuntimeAbiCatalog.abi(call)
        val candidates = inventory.methods(expected.owner, expected.name)
        val descriptorMatches = candidates.filter { method -> method.descriptor == expected.descriptor }
        if (candidates.isEmpty()) {
            RuntimeAbiMismatch.MissingMethod(expected)
        } else if (descriptorMatches.isEmpty()) {
            RuntimeAbiMismatch.WrongDescriptor(
                expected = expected,
                actualDescriptors = candidates.map { method -> method.descriptor }.distinct().sorted(),
            )
        } else if (descriptorMatches.none { method -> method.isStatic == expected.isStatic }) {
            RuntimeAbiMismatch.WrongStaticAccess(
                expected = expected,
                actualIsStatic = descriptorMatches.first().isStatic,
            )
        } else {
            null
        }
    }
}
