package com.akshaykadam.pixelboard.extension.rambler;

import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class GboardRamblerRotationGraceListenerTest {
    // Other Robolectric tests read flags through the policy, which starts tracking on the
    // shared application, so start each test from a clean state.
    @Before
    @After
    public void reset() {
        GboardRamblerRotationGrace.resetForTests();
    }

    @Test
    public void firstRotationAfterTrackingStartsKeepsTheSession() {
        long[] now = {1_000L};
        GboardRamblerRotationGrace.setClockForTests(() -> now[0]);
        CapturingContext context = new CapturingContext(RuntimeEnvironment.getApplication());

        // Tracking starts on an early flag read, before Gboard ever reads the delay.
        GboardRamblerRotationGrace.ensureListening(context);
        Assert.assertNotNull(context.callbacks);
        Assert.assertEquals(0L, GboardRamblerRotationGrace.deactivationDelayMs());

        now[0] = 5_000L;
        context.callbacks.onConfigurationChanged(rotated(context.getResources().getConfiguration()));
        now[0] = 5_016L;
        Assert.assertEquals(GboardRamblerRotationGrace.DEACTIVATION_GRACE_MS,
                GboardRamblerRotationGrace.deactivationDelayMs());
    }

    @Test
    public void trackingStartsOnlyOnce() {
        CapturingContext context = new CapturingContext(RuntimeEnvironment.getApplication());
        GboardRamblerRotationGrace.ensureListening(context);
        GboardRamblerRotationGrace.ensureListening(context);
        Assert.assertEquals(1, context.registrations);
    }

    @Test
    public void failedRegistrationIsNotRetriedOnEveryFlagRead() {
        CapturingContext context = new CapturingContext(RuntimeEnvironment.getApplication());
        context.failRegistration = true;
        GboardRamblerRotationGrace.ensureListening(context);
        GboardRamblerRotationGrace.ensureListening(context);
        Assert.assertEquals(1, context.registrations);
        Assert.assertEquals(0L, GboardRamblerRotationGrace.deactivationDelayMs(5_000L));
    }

    private static Configuration rotated(Configuration current) {
        Configuration rotated = new Configuration(current);
        rotated.orientation = current.orientation == Configuration.ORIENTATION_LANDSCAPE
                ? Configuration.ORIENTATION_PORTRAIT
                : Configuration.ORIENTATION_LANDSCAPE;
        rotated.screenWidthDp = current.screenHeightDp;
        rotated.screenHeightDp = current.screenWidthDp;
        return rotated;
    }

    private static final class CapturingContext extends ContextWrapper {
        ComponentCallbacks callbacks;
        int registrations;
        boolean failRegistration;

        CapturingContext(Context base) {
            super(base);
        }

        @Override
        public void registerComponentCallbacks(ComponentCallbacks componentCallbacks) {
            registrations++;
            if (failRegistration) {
                throw new IllegalStateException("registration refused");
            }
            callbacks = componentCallbacks;
        }
    }
}
