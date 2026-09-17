package com.akshaykadam.pixelboard.extension.settings;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public final class GboardPatchesSettingsOrchestratorTest {
    @Test
    public void navigationOwnsNestedBackStackAndOnlyExitsFromRoot() {
        GboardPatchesSettingsOrchestrator<String, Object> orchestrator =
                new GboardPatchesSettingsOrchestrator<String, Object>();

        orchestrator.accept(GboardPatchesSettingsOrchestrator.Event.open("keyboard"));
        orchestrator.accept(GboardPatchesSettingsOrchestrator.Event.open("voice"));

        GboardPatchesSettingsOrchestrator.Transition<String, Object> firstBack =
                orchestrator.accept(GboardPatchesSettingsOrchestrator.Event.back());
        Assert.assertEquals("keyboard", firstBack.getState().getCurrent());
        Assert.assertEquals(Arrays.asList(), firstBack.getState().getBackStack());
        assertEffects(firstBack,
                "REFRESH_DEPENDENCIES", "SCROLL_TO_TOP", "REQUEST_RENDER");

        GboardPatchesSettingsOrchestrator.Transition<String, Object> secondBack =
                orchestrator.accept(GboardPatchesSettingsOrchestrator.Event.back());
        Assert.assertNull(secondBack.getState().getCurrent());
        assertEffects(secondBack,
                "REFRESH_DEPENDENCIES", "SCROLL_TO_TOP", "REQUEST_RENDER");

        GboardPatchesSettingsOrchestrator.Transition<String, Object> rootBack =
                orchestrator.accept(GboardPatchesSettingsOrchestrator.Event.back());
        assertEffects(rootBack, "EXIT");
    }

    @Test
    public void onlyLatestRenderGenerationCanBeAppliedAndPauseInvalidatesPendingWork() {
        GboardPatchesSettingsOrchestrator<String, Object> orchestrator =
                new GboardPatchesSettingsOrchestrator<String, Object>();

        int first = renderGeneration(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.requestRender()));
        int second = renderGeneration(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.requestRender()));

        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.renderReady(first, 500L)));
        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.renderReady(second, 500L)),
                "APPLY_RENDER", "CANCEL_REFRESH");

        int third = renderGeneration(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.requestRender()));
        orchestrator.accept(GboardPatchesSettingsOrchestrator.Event.pause());

        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.renderReady(third, 500L)));
    }

    @Test
    public void periodicRefreshRunsOnlyWhileResumedAndNoModalIsVisible() {
        GboardPatchesSettingsOrchestrator<String, Object> orchestrator =
                new GboardPatchesSettingsOrchestrator<String, Object>();
        GboardPatchesSettingsOrchestrator.Transition<String, Object> resumed =
                orchestrator.accept(GboardPatchesSettingsOrchestrator.Event.resume());
        int generation = renderGeneration(resumed);

        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.renderReady(generation, 750L)),
                "APPLY_RENDER", "CANCEL_REFRESH", "SCHEDULE_REFRESH");
        Assert.assertEquals(750L, lastEffect(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.modalShown()),
                "CANCEL_REFRESH").getDelayMs());

        orchestrator.accept(GboardPatchesSettingsOrchestrator.Event.modalShown());
        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.modalDismissed()));
        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.modalDismissed()),
                "SCHEDULE_REFRESH");

        orchestrator.accept(GboardPatchesSettingsOrchestrator.Event.pause());
        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.modalShown()),
                "CANCEL_REFRESH");
        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.modalDismissed()));
    }

    @Test
    public void renderFailureChoosesRecoveryBeforeFatalFallback() {
        GboardPatchesSettingsOrchestrator<String, Object> orchestrator =
                new GboardPatchesSettingsOrchestrator<String, Object>();
        int generation = renderGeneration(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.requestRender()));

        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.renderFailed(generation)),
                "APPLY_RECOVERY");
        GboardPatchesSettingsOrchestrator.Transition<String, Object> fatal = orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.recoveryFailed(generation));
        Assert.assertTrue(fatal.getState().isFatal());
        assertEffects(fatal, "CANCEL_REFRESH", "SHOW_FATAL");

        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.requestRender()));
    }

    @Test
    public void recoveryScreenReplacesFailedScreensRefreshPolicy() {
        GboardPatchesSettingsOrchestrator<String, Object> orchestrator =
                new GboardPatchesSettingsOrchestrator<String, Object>();
        int generation = renderGeneration(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.resume()));
        orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.renderReady(generation, 750L));

        GboardPatchesSettingsOrchestrator.Transition<String, Object> recovery = orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.recoveryReady(generation, 0L));

        Assert.assertEquals(0L, recovery.getState().getRefreshIntervalMs());
        assertEffects(recovery, "APPLY_RECOVERY", "CANCEL_REFRESH");
    }

    @Test
    public void noRefreshScreenCancelsPreviouslyScheduledPeriodicRefresh() {
        GboardPatchesSettingsOrchestrator<String, Object> orchestrator =
                new GboardPatchesSettingsOrchestrator<String, Object>();
        int firstGeneration = renderGeneration(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.resume()));
        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.renderReady(firstGeneration, 750L)),
                "APPLY_RENDER", "CANCEL_REFRESH", "SCHEDULE_REFRESH");

        int secondGeneration = renderGeneration(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.requestRender()));

        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.renderReady(secondGeneration, 0L)),
                "APPLY_RENDER", "CANCEL_REFRESH");
    }

    @Test
    public void directFatalFailurePermanentlyRejectsNewRenderRequests() {
        GboardPatchesSettingsOrchestrator<String, Object> orchestrator =
                new GboardPatchesSettingsOrchestrator<String, Object>();

        GboardPatchesSettingsOrchestrator.Transition<String, Object> fatal =
                orchestrator.accept(GboardPatchesSettingsOrchestrator.Event.fatal());

        Assert.assertTrue(fatal.getState().isFatal());
        assertEffects(fatal, "CANCEL_REFRESH", "SHOW_FATAL");
        assertEffects(orchestrator.accept(GboardPatchesSettingsOrchestrator.Event.resume()));
        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.requestRender()));
    }

    @Test
    public void featureIntentIsExecutedOnlyThroughAnOrchestrationEffect() {
        GboardPatchesSettingsOrchestrator<String, Runnable> orchestrator =
                new GboardPatchesSettingsOrchestrator<String, Runnable>();
        Runnable intent = () -> {
        };

        GboardPatchesSettingsOrchestrator.Transition<String, Runnable> transition =
                orchestrator.accept(
                        GboardPatchesSettingsOrchestrator.Event.featureIntent(intent));

        assertEffects(transition, "EXECUTE_INTENT");
        Runnable typedPayload = transition.getEffects().get(0).getPayload();
        Assert.assertSame(intent, typedPayload);

        orchestrator.accept(GboardPatchesSettingsOrchestrator.Event.fatal());
        assertEffects(orchestrator.accept(
                GboardPatchesSettingsOrchestrator.Event.featureIntent(intent)));
    }

    private static int renderGeneration(
            GboardPatchesSettingsOrchestrator.Transition<?, ?> transition) {
        return lastEffect(transition, "REQUEST_RENDER").getGeneration();
    }

    private static GboardPatchesSettingsOrchestrator.Effect<?, ?> lastEffect(
            GboardPatchesSettingsOrchestrator.Transition<?, ?> transition,
            String kind) {
        GboardPatchesSettingsOrchestrator.Effect<?, ?> match = null;
        for (GboardPatchesSettingsOrchestrator.Effect<?, ?> effect : transition.getEffects()) {
            if (kind.equals(effect.getKind().name())) {
                match = effect;
            }
        }
        Assert.assertNotNull("Missing effect " + kind, match);
        return match;
    }

    private static void assertEffects(
            GboardPatchesSettingsOrchestrator.Transition<?, ?> transition,
            String... expectedKinds) {
        List<GboardPatchesSettingsOrchestrator.Effect<?, ?>> effects =
                (List<GboardPatchesSettingsOrchestrator.Effect<?, ?>>) (List<?>) transition.getEffects();
        Assert.assertEquals("Unexpected effect count", expectedKinds.length, effects.size());
        for (int index = 0; index < expectedKinds.length; index++) {
            Assert.assertEquals(expectedKinds[index], effects.get(index).getKind().name());
        }
    }
}
