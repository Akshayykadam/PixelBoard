package com.akshaykadam.pixelboard.extension.settings;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public final class GboardManagedDialogRunnerCrashBoundaryTest {
    @Test
    public void shownCallbackFailureIsReturnedInsteadOfEscaping() {
        RuntimeException expected = new RuntimeException("shown");

        Throwable actual = GboardManagedDialogRunner.run(
                onDismiss -> true,
                () -> { throw expected; },
                () -> { });

        Assert.assertSame(expected, actual);
    }

    @Test
    public void asynchronousDismissCallbackFailureDoesNotEscape() {
        AtomicInteger dismissed = new AtomicInteger();
        Runnable[] completion = new Runnable[1];
        Throwable result = GboardManagedDialogRunner.run(
                onDismiss -> {
                    completion[0] = onDismiss;
                    return true;
                },
                () -> { },
                () -> {
                    dismissed.incrementAndGet();
                    throw new RuntimeException("dismissed");
                });

        Assert.assertNull(result);
        completion[0].run();
        Assert.assertEquals(1, dismissed.get());
    }
}
