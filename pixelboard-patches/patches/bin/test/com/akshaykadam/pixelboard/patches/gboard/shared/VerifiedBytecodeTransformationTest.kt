package com.akshaykadam.pixelboard.patches.gboard.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifiedBytecodeTransformationTest {
    @Test
    fun `stock target is mutated exactly once and verified as patched`() {
        val target = FakeTarget(Shape.STOCK)

        val result = target.applyVerified(fakePlan())

        assertSame(target, result)
        assertEquals(Shape.PATCHED, target.shape)
        assertEquals(1, target.mutations)
    }

    @Test
    fun `already patched target is returned without mutation`() {
        val target = FakeTarget(Shape.PATCHED)

        val result = target.applyVerified(fakePlan())

        assertSame(target, result)
        assertEquals(0, target.mutations)
    }

    @Test
    fun `replacement target is post-verified and returned`() {
        val stock = FakeTarget(Shape.STOCK)
        val replacement = FakeTarget(Shape.PATCHED)
        val plan = VerifiedTransformationPlan(
            targetName = "fake target",
            classify = ::classifyFakeTarget,
            mutate = { target ->
                target.mutations += 1
                replacement
            },
        )

        val result = stock.applyVerified(plan)

        assertSame(replacement, result)
        assertEquals(1, stock.mutations)
        assertEquals(Shape.STOCK, stock.shape)
    }

    @Test
    fun `malformed target fails before mutation`() {
        val target = FakeTarget(Shape.PARTIAL)

        val failure = assertThrows(IllegalStateException::class.java) {
            target.applyVerified(fakePlan())
        }

        assertEquals(0, target.mutations)
        assertTrue(failure.message.orEmpty().contains("fake target"))
        assertTrue(failure.message.orEmpty().contains("MALFORMED"))
    }

    @Test
    fun `mutation output must classify as patched`() {
        val target = FakeTarget(Shape.STOCK)

        val failure = assertThrows(IllegalStateException::class.java) {
            target.applyVerified(fakePlan(output = Shape.PARTIAL))
        }

        assertEquals(1, target.mutations)
        assertTrue(failure.message.orEmpty().contains("post-mutation"))
        assertTrue(failure.message.orEmpty().contains("MALFORMED"))
    }

    @Test
    fun `mutation exception includes target and phase context`() {
        val target = FakeTarget(Shape.STOCK)
        val cause = UnsupportedOperationException("boom")

        val failure = assertThrows(IllegalStateException::class.java) {
            target.applyVerified(fakePlan(failure = cause))
        }

        assertEquals(1, target.mutations)
        assertSame(cause, failure.cause)
        assertTrue(failure.message.orEmpty().contains("fake target"))
        assertTrue(failure.message.orEmpty().contains("mutation"))
    }

    @Test
    fun `initial classification exception retains validator cause and phase`() {
        val target = FakeTarget(Shape.STOCK)
        val cause = IllegalArgumentException("stock fingerprint drift")
        val plan = VerifiedTransformationPlan(
            targetName = "fake target",
            classify = { throw cause },
            mutate = ::patchFakeTarget,
        )

        val failure = assertThrows(IllegalStateException::class.java) {
            target.applyVerified(plan)
        }

        assertEquals(0, target.mutations)
        assertSame(cause, failure.cause)
        assertTrue(failure.message.orEmpty().contains("fake target"))
        assertTrue(failure.message.orEmpty().contains("initial classification"))
    }

    @Test
    fun `post classification exception retains validator cause and phase`() {
        val target = FakeTarget(Shape.STOCK)
        val cause = IllegalArgumentException("patched fingerprint drift")
        var classifications = 0
        val plan = VerifiedTransformationPlan(
            targetName = "fake target",
            classify = { candidate ->
                classifications += 1
                if (classifications == 1) {
                    VerifiedTransformationState.STOCK
                } else {
                    assertSame(target, candidate)
                    throw cause
                }
            },
            mutate = ::patchFakeTarget,
        )

        val failure = assertThrows(IllegalStateException::class.java) {
            target.applyVerified(plan)
        }

        assertEquals(1, target.mutations)
        assertSame(cause, failure.cause)
        assertTrue(failure.message.orEmpty().contains("fake target"))
        assertTrue(failure.message.orEmpty().contains("post-mutation classification"))
    }

    private fun fakePlan(
        output: Shape = Shape.PATCHED,
        failure: RuntimeException? = null,
    ) = VerifiedTransformationPlan<FakeTarget>(
        targetName = "fake target",
        classify = ::classifyFakeTarget,
        mutate = { target ->
            target.mutations += 1
            failure?.let { throw it }
            target.shape = output
            target
        },
    )

    private data class FakeTarget(
        var shape: Shape,
        var mutations: Int = 0,
    )

    private enum class Shape {
        STOCK,
        PATCHED,
        PARTIAL,
    }

    private fun classifyFakeTarget(target: FakeTarget): VerifiedTransformationState =
        when (target.shape) {
            Shape.STOCK -> VerifiedTransformationState.STOCK
            Shape.PATCHED -> VerifiedTransformationState.PATCHED
            Shape.PARTIAL -> VerifiedTransformationState.MALFORMED
        }

    private fun patchFakeTarget(target: FakeTarget): FakeTarget = target.apply {
        mutations += 1
        shape = Shape.PATCHED
    }
}
