package com.akshaykadam.pixelboard.patches.gboard.shared

internal enum class VerifiedTransformationState {
    STOCK,
    PATCHED,
    MALFORMED,
}

internal class VerifiedTransformationPlan<T>(
    internal val targetName: String,
    internal val classify: (T) -> VerifiedTransformationState,
    internal val mutate: (T) -> T,
)

internal fun <T> T.applyVerified(plan: VerifiedTransformationPlan<T>): T {
    return when (val initialState = plan.classifyWithContext(this, "initial classification")) {
        VerifiedTransformationState.PATCHED -> this
        VerifiedTransformationState.MALFORMED -> error(
            "Refusing ${plan.targetName} in MALFORMED state before mutation",
        )
        VerifiedTransformationState.STOCK -> {
            val transformed = try {
                plan.mutate(this)
            } catch (failure: Exception) {
                throw IllegalStateException(
                    "Verified transformation mutation failed for ${plan.targetName}: " +
                        failure.message,
                    failure,
                )
            }
            val finalState = plan.classifyWithContext(
                transformed,
                "post-mutation classification",
            )
            check(finalState == VerifiedTransformationState.PATCHED) {
                "Verified transformation post-mutation classification for " +
                    "${plan.targetName} was $finalState; expected PATCHED"
            }
            transformed
        }
    }
}

private fun <T> VerifiedTransformationPlan<T>.classifyWithContext(
    target: T,
    phase: String,
): VerifiedTransformationState = try {
    classify(target)
} catch (failure: Exception) {
    throw IllegalStateException(
        "Verified transformation $phase failed for $targetName: ${failure.message}",
        failure,
    )
}
