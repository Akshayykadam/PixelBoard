import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.Activity;
import android.app.Application;
import android.os.Looper;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;

import com.akshaykadam.pixelboard.extension.rambler.GboardMicBridge;
import com.akshaykadam.pixelboard.extension.rambler.GboardMicGestureRuntime;
import com.akshaykadam.pixelboard.extension.rambler.GboardMicGestureSettings;
import com.akshaykadam.pixelboard.extension.rambler.GboardRambler1803OfficialSelectionRuntime;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettings;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsContract;
import com.google.android.libraries.inputmethod.accesspoint.widget.AccessPointsBarItemSoftKeyView;
import com.google.android.libraries.inputmethod.module.ModuleManager;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

/**
 * Real UI -> real bridge -> unchanged selector runtime. Only proprietary native boundaries are
 * fixtures.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = 35, manifest = Config.NONE, application = Application.class)
public class GestureBridgeIntegrationTest {
    public static class Ime extends pbb {
        @Override
        protected owa ai() {
            return null;
        }
    }

    public enum Kind {
        PRESS
    }

    public static class Def {
        public Object[] f;

        Def(int code) {
            f = new Object[] {new Action(code)};
        }
    }

    public static class Action {
        public Kind c = Kind.PRESS;
        public Object[] d;

        Action(int code) {
            d = new Object[] {new Key(code)};
        }
    }

    public static class Key {
        public int c;
        public Object e = "";

        Key(int code) {
            c = code;
        }
    }

    Ime ime;
    FrameLayout root, original;
    AccessPointsBarItemSoftKeyView mic;

    void runtimeReset() throws Exception {
        Method m =
                GboardRambler1803OfficialSelectionRuntime.class.getDeclaredMethod("resetForTests");
        m.setAccessible(true);
        m.invoke(null);
    }

    void layout() {
        root.measure(
                View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 500, 1000);
    }

    void position() {
        layout();
        root.getViewTreeObserver().dispatchOnPreDraw();
        layout();
    }

    @Before
    public void setup() throws Exception {
        runtimeReset();
        ModuleManager.reset();
        ime = spy(Robolectric.buildService(Ime.class).create().get());
        doReturn("enable_jetson").when(ime).getString(0x7f140954);
        ime.connection = mock(InputConnection.class);
        ime.editor = new EditorInfo();
        ime.editor.inputType = InputType.TYPE_CLASS_TEXT;
        qou.reset(ime);
        qou.I(ime).seed(false);
        GboardPatchesSettings.preferences(ime)
                .edit()
                .clear()
                .putBoolean("pref_advanced_voice_typing_enabled", true)
                .putString(
                        GboardMicGestureSettings.PREF_KEY_MODE,
                        GboardMicGestureSettings.MODE_HYBRID)
                .commit();
        original = new FrameLayout(ime);
        mic = new AccessPointsBarItemSoftKeyView(ime);
        mic.e = new Def(-10042);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(50, 50);
        lp.leftMargin = 400;
        lp.topMargin = 700;
        original.addView(mic, lp);
        root = (FrameLayout) GboardMicGestureRuntime.wrap(ime, original);
        Robolectric.buildActivity(Activity.class).setup().get().setContentView(root);
        position();
    }

    @After
    public void teardown() throws Exception {
        GboardMicGestureRuntime.destroy(ime);
        runtimeReset();
    }

    void event(int action) {
        MotionEvent e =
                MotionEvent.obtain(0, android.os.SystemClock.uptimeMillis(), action, 425, 725, 0);
        assertTrue(root.dispatchTouchEvent(e));
        e.recycle();
    }

    void tap() {
        event(MotionEvent.ACTION_DOWN);
        event(MotionEvent.ACTION_UP);
    }

    void advance(long ms) {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));
    }

    void setMode(String mode) {
        assertTrue(GboardMicGestureSettings.writeMode(ime, mode));
    }

    void noActions() {
        assertTrue(ime.nativeEvents.isEmpty());
        assertTrue(ime.normalEvents.isEmpty());
    }

    @Test
    public void doubleTapUsesProvenMarkedNativeRouteOnSecondRelease() {
        tap();
        advance(299);
        noActions();
        tap();
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        assertEquals(Collections.singletonList(mnp.a), ime.nativeSources);
        assertEquals(Collections.singletonList(true), ime.savedModesAtDelivery);
        assertTrue(ime.normalEvents.isEmpty());
        assertTrue(GboardMicBridge.isRamblerListening(ime));
        advance(1000);
        assertTrue(ime.normalEvents.isEmpty());
        assertEquals(1, ime.nativeEvents.size());
    }

    @Test
    public void singleTapWaits300MsBeforeNativeStandard() {
        tap();
        advance(299);
        noActions();
        assertTrue(qou.I(ime).writes.isEmpty());
        advance(1);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        assertTrue(ime.nativeEvents.isEmpty());
        assertEquals(Collections.singletonList(false), qou.I(ime).writes);
    }

    @Test
    public void activeNativeRamblerFinishesInsideUpWithoutStandardOrTimer() {
        qou.I(ime).seed(true);
        ime.microphone.listening = true;
        showNativeTick();
        tap();
        assertFalse(GboardMicBridge.isRamblerListening(ime));
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        assertEquals(Collections.singletonList(mnp.a), ime.nativeSources);
        assertTrue(qou.I(ime).writes.isEmpty());
        assertTrue(ime.normalEvents.isEmpty());
        advance(1000);
        assertEquals(1, ime.nativeEvents.size());
        assertTrue(ime.normalEvents.isEmpty());
    }

    @Test
    public void standardNativeStopPassesThroughImmediatelyWithoutPendingTimer() {
        mic.e = new Def(-10066);
        mic.setOnTouchListener(
                (v, e) -> {
                    if (e.getActionMasked() == MotionEvent.ACTION_UP) ime.dG(oas.e(-10066, ""));
                    return true;
                });
        position();
        tap();
        assertEquals(Collections.singletonList(-10066), ime.normalEvents);
        assertTrue(qou.I(ime).writes.isEmpty());
        advance(1000);
        assertEquals(1, ime.normalEvents.size());
    }

    @Test
    public void savedTrueDoesNotMeanListeningAndActiveFinishNeverChangesSelection() {
        qou.I(ime).seed(true);
        tap();
        advance(299);
        noActions();
        advance(1);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        qou.I(ime).seed(true);
        ime.microphone.listening = true;
        showNativeTick();
        tap();
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        assertFalse(GboardMicBridge.isRamblerListening(ime));
    }

    @Test
    public void unhandledRamblerReportsFailureAndNeverFallsBackOrLeavesTimer() {
        ime.ramblerHandled = false;
        tap();
        advance(100);
        tap();
        advance(1000);
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        assertTrue(ime.normalEvents.isEmpty());
        assertFalse(qou.I(ime).x(0x7f140954, true));
    }

    @Test
    public void contractFailureCancelsIdleTapBeforeAnyModeMutation() {
        ime.F = new Object();
        tap();
        advance(1000);
        noActions();
        assertTrue(qou.I(ime).writes.isEmpty());
    }

    @Test
    public void fieldChangeDuringWindowCancelsProductionDispatch() {
        tap();
        ime.connection = mock(InputConnection.class);
        advance(1000);
        noActions();
        assertTrue(qou.I(ime).writes.isEmpty());
    }

    @Test
    public void realPreferenceCallbackCanInvalidateWithoutLosingRamblerDispatch() {
        qou.I(ime).onWrite = () -> GboardMicGestureRuntime.invalidate(ime);
        tap();
        advance(100);
        tap();
        MotionEvent orphan =
                MotionEvent.obtain(
                        0,
                        android.os.SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_UP,
                        425,
                        725,
                        0);
        root.dispatchTouchEvent(orphan);
        orphan.recycle();
        advance(1000);
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        assertTrue(ime.normalEvents.isEmpty());
    }

    @Test
    public void repeatedStartFinishThenSingleStandardDoNotCrossModes() {
        tap();
        advance(100);
        tap();
        tap();
        assertFalse(GboardMicBridge.isRamblerListening(ime));
        assertEquals(Arrays.asList(-10217, -10217), ime.nativeEvents);
        assertEquals(Collections.singletonList(true), qou.I(ime).writes);
        tap();
        advance(300);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        assertEquals(Arrays.asList(true, false), qou.I(ime).writes);
    }

    void assertCleanUi() {
        assertEquals(2, root.getChildCount());
        for (int i = 0; i < root.getChildCount(); i++)
            assertFalse(root.getChildAt(i) instanceof android.widget.TextView);
        assertEquals(0, org.robolectric.shadows.ShadowToast.shownToastCount());
    }

    @Test
    public void secondReleaseAfterSmallRelayoutReachesRealMarkedRambler() {
        tap();
        advance(100);
        event(MotionEvent.ACTION_DOWN);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mic.getLayoutParams();
        lp.leftMargin++;
        mic.setLayoutParams(lp);
        position();
        event(MotionEvent.ACTION_UP);
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        assertEquals(Collections.singletonList(mnp.a), ime.nativeSources);
        assertTrue(ime.normalEvents.isEmpty());
        assertCleanUi();
    }

    @Test
    public void nativeRejectionHasNoDiagnosticUiAndNoStandardFallback() {
        ime.ramblerHandled = false;
        tap();
        advance(100);
        tap();
        advance(1000);
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        assertFalse(qou.I(ime).x(0x7f140954, true));
        assertCleanUi();
        assertTrue(ime.normalEvents.isEmpty());
    }

    @Test
    public void standardTimerDispatchesWithoutDiagnosticUi() {
        tap();
        advance(300);
        assertCleanUi();
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
    }

    fdj lite(fgf stage, boolean recording) {
        ifs recorder = new ifs();
        recorder.running = recording;
        fdj loaded = new fdj(recorder);
        loaded.c.d.set(stage);
        ((obl) ime.F).wrapper.h = loaded;
        qou.I(ime).seed(true);
        ime.microphone.listening = false;
        return loaded;
    }

    @Test
    public void liteTickWithClassicListenZeroFinishesImmediatelyWithoutSelectingStandard() {
        fdj loaded = lite(fgf.LISTENING, true);
        showNativeTick();
        assertFalse(GboardMicBridge.isRamblerListening(ime));
        tap();
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        assertTrue(ime.normalEvents.isEmpty());
        assertTrue(qou.I(ime).writes.isEmpty());
        assertFalse(loaded.recorder().running);
        assertEquals(fgf.PROCESSING, loaded.c.h());
        assertCleanUi();
        advance(1000);
        assertTrue(ime.normalEvents.isEmpty());
        assertTrue(qou.I(ime).writes.isEmpty());
    }

    @Test
    public void directStandardRequestDuringLiteSessionNeverSelectsStandard() {
        lite(fgf.LISTENING, true);
        showNativeTick();
        GboardMicBridge.startGoogleStandard(ime);
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        assertTrue(ime.normalEvents.isEmpty());
        assertTrue(qou.I(ime).writes.isEmpty());
    }

    @Test
    public void liteSessionBecomingActiveDuringPendingStandardSuppressesTimer() {
        tap();
        lite(fgf.LISTENING, true);
        advance(300);
        assertTrue(ime.nativeEvents.isEmpty());
        assertTrue(ime.normalEvents.isEmpty());
        assertTrue(qou.I(ime).writes.isEmpty());
    }

    @Test
    public void processingLiteSessionCannotBeChangedToStandardByTap() {
        lite(fgf.PROCESSING, false);
        tap();
        advance(1000);
        assertTrue(ime.normalEvents.isEmpty());
        assertTrue(qou.I(ime).writes.isEmpty());
    }

    @Test
    public void busyFinishWaitsForNativeTranscriptThenAllowsStandardFromNativeIdle() {
        fdj loaded = lite(fgf.LISTENING, true);
        showNativeTick();
        tap();
        hideNativeTick();
        advance(100);
        tap();
        advance(500);
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        assertTrue(ime.normalEvents.isEmpty());
        assertTrue(qou.I(ime).writes.isEmpty());
        assertCleanUi();
        loaded.c.d.set(fgf.IDLING);
        tap();
        advance(300);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        assertEquals(Collections.singletonList(false), qou.I(ime).writes);
    }

    @Test
    public void inactiveLoadedLiteStageMustNotBeReportedAsIdleOrSelectStandard() {
        fdj loaded = lite(fgf.PROCESSING, false);
        ((obl) ime.F).wrapper.l = false;
        assertNull(((obl) ime.F).b(mvr.class));
        assertTrue(
                "An existing inactive wrapper still owns native processing state",
                GboardMicBridge.isRamblerActive(ime));
        tap();
        advance(1000);
        assertTrue(ime.normalEvents.isEmpty());
        assertTrue(qou.I(ime).writes.isEmpty());
        assertEquals(fgf.PROCESSING, loaded.c.h());
        assertCleanUi();
    }

    @Test
    public void consumedButNotStartedNeverRetriesAndStateRemainsReadableWithoutUi() {
        fdj loaded = lite(fgf.IDLING, false);
        ime.consumeWithoutStarting = true;
        tap();
        advance(100);
        tap();
        assertEquals(1, ime.nativeEvents.size());
        assertCleanUi();
        advance(499);
        assertCleanUi();
        advance(1);
        assertTrue(GboardMicBridge.describeRamblerState(ime).contains("STAGE=IDLING LITE_REC=0"));
        loaded.c.d.set(fgf.LISTENING);
        loaded.recorder().running = true;
        advance(1000);
        assertTrue(
                GboardMicBridge.describeRamblerState(ime).contains("STAGE=LISTENING LITE_REC=1"));
        assertCleanUi();
        assertEquals("Observation must not retry a toggle", 1, ime.nativeEvents.size());
        assertTrue(ime.normalEvents.isEmpty());
    }

    @Test
    public void activeNativeStartRequestFinishesWithoutRewritingSelection() {
        fdj loaded = lite(fgf.LISTENING, true);
        showNativeTick();
        GboardMicBridge.startGoogleRambler(ime);
        assertFalse(loaded.recorder().running);
        assertEquals(fgf.PROCESSING, loaded.c.h());
        assertTrue(qou.I(ime).writes.isEmpty());
        assertTrue(ime.normalEvents.isEmpty());
    }

    @Test
    public void staleBusyStartDoesNotDispatchToggleOrDiscardProcessingAndNextIdleStartWorks() {
        fdj loaded = lite(fgf.PROCESSING, false);
        ((obl) ime.F).wrapper.l = false;
        GboardMicBridge.startGoogleRambler(ime);
        assertTrue(ime.nativeEvents.isEmpty());
        assertTrue(qou.I(ime).writes.isEmpty());
        assertEquals(fgf.PROCESSING, loaded.c.h());
        loaded.c.d.set(fgf.IDLING);
        ((obl) ime.F).wrapper.l = true;
        GboardMicBridge.startGoogleRambler(ime);
        assertTrue(loaded.recorder().running);
        assertEquals(1, ime.nativeEvents.size());
    }

    @Test
    public void cleanReleaseNeverDispatchesAfterFieldChangesOrKeyboardDestroy() {
        lite(fgf.IDLING, false);
        ime.consumeWithoutStarting = true;
        tap();
        advance(100);
        tap();
        ime.connection = mock(InputConnection.class);
        advance(500);
        assertCleanUi();
        GboardMicGestureRuntime.destroy(ime);
        advance(1000);
        assertEquals(1, ime.nativeEvents.size());
        assertTrue(ime.normalEvents.isEmpty());
    }

    @Test
    public void nativeSessionAppearingDuringSelectionIsNotToggledOff() {
        fdj loaded = lite(fgf.IDLING, false);
        qou.I(ime).onWrite =
                () -> {
                    loaded.c.d.set(fgf.LISTENING);
                    loaded.recorder().running = true;
                };
        GboardMicBridge.startGoogleRambler(ime);
        assertTrue(loaded.recorder().running);
        assertTrue(ime.nativeEvents.isEmpty());
        assertTrue(ime.normalEvents.isEmpty());
        assertCleanUi();
        advance(1500);
        assertCleanUi();
    }

    @Test
    public void acceptedLaunchThatStaysIdleHasNoDiagnosticUiResetOrFallback() {
        lite(fgf.IDLING, false);
        ime.consumeWithoutStarting = true;
        tap();
        advance(100);
        tap();
        advance(1500);
        assertCleanUi();
        assertEquals(1, ime.nativeEvents.size());
        assertTrue(ime.normalEvents.isEmpty());
        assertEquals(Collections.singletonList(true), qou.I(ime).writes);
    }

    public static class Wrapped {
        public Kind b = Kind.PRESS;
        public Object c;

        Wrapped(Object point) {
            c = point;
        }
    }

    public static class Point {
        public String b = "push_to_ramble";
        public int c = 0x7f08040a;
        public java.util.Map<Kind, Action> j =
                Collections.singletonMap(Kind.PRESS, new Action(-10217));
        public java.util.Map<String, Object> r = Collections.singletonMap("highlighted", true);
    }

    void invisibleModule() {
        ime.F = new obl(null);
        ime.unobservableRambler = true;
    }

    void startInvisible() {
        invisibleModule();
        tap();
        advance(100);
        tap();
        assertTrue(ime.hiddenRamblerRecording);
        assertTrue(GboardMicBridge.describeRamblerState(ime).contains("STAGE=UNLOADED"));
    }

    @Test
    public void acceptedStartWithUnloadedProbesMustFinishImmediatelyWithoutStandard() {
        startInvisible();
        advance(1600);
        tap();
        assertFalse(
                "Tick must stop the native session even when probes see no module",
                ime.hiddenRamblerRecording);
        assertEquals(Arrays.asList(-10217, -10217), ime.nativeEvents);
        assertEquals(Collections.singletonList(true), qou.I(ime).writes);
        assertTrue(ime.normalEvents.isEmpty());
        assertCleanUi();
        advance(1000);
        assertTrue(ime.normalEvents.isEmpty());
    }

    @Test
    public void directStandardAfterAcceptedUnobservableStartFinishesWithoutSelectingStandard() {
        startInvisible();
        GboardMicBridge.startGoogleStandard(ime);
        assertFalse(ime.hiddenRamblerRecording);
        assertEquals(Arrays.asList(-10217, -10217), ime.nativeEvents);
        assertEquals(Collections.singletonList(true), qou.I(ime).writes);
        assertTrue(ime.normalEvents.isEmpty());
    }

    @Test
    public void actualHighlightedNativeToolbarTickWorksWithoutAnyAcceptedStartBackstop() {
        invisibleModule();
        ime.hiddenRamblerRecording = true;
        qou.I(ime).seed(true);
        Def tick = new Def(-40002);
        ((Key) ((Action) tick.f[0]).d[0]).e = new Wrapped(new Point());
        mic.e = tick;
        position();
        tap();
        assertFalse(ime.hiddenRamblerRecording);
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        assertTrue(qou.I(ime).writes.isEmpty());
        assertTrue(ime.normalEvents.isEmpty());
        assertCleanUi();
    }

    @Test
    public void nonHighlightedRamblerLaunchIsNotMistakenForFinishTick() {
        invisibleModule();
        Point point = new Point();
        point.r = Collections.singletonMap("highlighted", false);
        Def launch = new Def(-40002);
        ((Key) ((Action) launch.f[0]).d[0]).e = new Wrapped(point);
        mic.e = launch;
        position();
        tap();
        advance(300);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        assertTrue(ime.nativeEvents.isEmpty());
    }

    @Test
    public void fieldChangeExpiresAcceptedStartBackstop() {
        startInvisible();
        ime.connection = mock(InputConnection.class);
        ime.editor = new EditorInfo();
        ime.editor.inputType = InputType.TYPE_CLASS_TEXT;
        assertFalse(GboardMicBridge.isRamblerActive(ime));
        ime.hiddenRamblerRecording = false;
        tap();
        advance(300);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
    }

    @Test
    public void keyboardHiddenExpiresAcceptedStartBackstop() {
        startInvisible();
        original.setVisibility(View.GONE);
        position();
        assertFalse(GboardMicBridge.isRamblerActive(ime));
        original.setVisibility(View.VISIBLE);
        position();
        ime.hiddenRamblerRecording = false;
        tap();
        advance(300);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
    }

    @Test
    public void keyboardDetachExpiresBackstopEvenIfSameFieldReturns() {
        startInvisible();
        android.view.ViewGroup parent = (android.view.ViewGroup) root.getParent();
        parent.removeView(root);
        parent.addView(root);
        position();
        assertFalse(GboardMicBridge.isRamblerActive(ime));
    }

    @Test
    public void externalStandardStartClearsBackstop() {
        startInvisible();
        GboardMicBridge.beforeEvent(ime, oas.e(-10042, ""));
        assertFalse(GboardMicBridge.isRamblerActive(ime));
    }

    @Test
    public void safetyTimeoutClearsBackstopWithoutDispatchingAnything() {
        startInvisible();
        advance(3001);
        assertFalse(GboardMicBridge.isRamblerActive(ime));
        assertEquals(1, ime.nativeEvents.size());
        assertTrue(ime.normalEvents.isEmpty());
    }

    @Test
    public void destroyClearsAcceptedStateAndRejectedFinishDoesNotStartStandard() {
        startInvisible();
        ime.ramblerHandled = false;
        tap();
        advance(1000);
        assertEquals(2, ime.nativeEvents.size());
        assertTrue(ime.normalEvents.isEmpty());
        GboardMicGestureRuntime.destroy(ime);
        assertFalse(GboardMicBridge.isRamblerActive(ime));
    }

    @Test
    public void ordinaryNativeCancellationKeepsBackstopButLifecycleResetClearsIt() {
        startInvisible();
        GboardMicGestureRuntime.invalidate(ime);
        assertTrue(GboardMicBridge.isRamblerActive(ime));
        GboardMicGestureRuntime.resetSession(ime);
        assertFalse(GboardMicBridge.isRamblerActive(ime));
    }

    @Test
    public void brokenNativeProbeCannotVetoAcceptedStartFinish() {
        startInvisible();
        ime.F = new Object();
        tap();
        assertFalse(ime.hiddenRamblerRecording);
        assertEquals(2, ime.nativeEvents.size());
        assertTrue(ime.normalEvents.isEmpty());
        assertEquals(Collections.singletonList(true), qou.I(ime).writes);
    }

    @Test
    public void decoyBusyModuleCannotVetoTheAcceptedStartTick() {
        startInvisible();
        ifs recorder = new ifs();
        fdj decoy = new fdj(recorder);
        decoy.c.d.set(fgf.PROCESSING);
        ime.F = new obl(new obr(decoy));
        tap();
        assertFalse(ime.hiddenRamblerRecording);
        assertEquals(2, ime.nativeEvents.size());
        assertTrue(ime.normalEvents.isEmpty());
        assertEquals(Collections.singletonList(true), qou.I(ime).writes);
        assertEquals(fgf.PROCESSING, decoy.c.h());
    }

    @Test
    public void replacedInputViewClearsBackstop() {
        startInvisible();
        GboardMicGestureRuntime.wrap(ime, new FrameLayout(ime));
        assertFalse(GboardMicBridge.isRamblerActive(ime));
    }

    @Test
    public void sameFieldInputRestartPreservesBackstopAndActualFieldRestartClearsIt() {
        startInvisible();
        GboardMicGestureRuntime.checkSession(ime);
        assertTrue(GboardMicBridge.isRamblerActive(ime));
        ime.connection = mock(InputConnection.class);
        GboardMicGestureRuntime.checkSession(ime);
        assertFalse(GboardMicBridge.isRamblerActive(ime));
    }

    void showNativeTick() {
        Def tick = new Def(-40002);
        ((Key) ((Action) tick.f[0]).d[0]).e = new Wrapped(new Point());
        mic.e = tick;
        position();
    }

    void hideNativeTick() {
        mic.e = new Def(-10042);
        position();
    }

    @Test
    public void automaticEndWithoutAnyVisibleTickExpiresLeaseAndSingleTapStartsStandard() {
        startInvisible();
        ime.hiddenRamblerRecording = false;
        advance(3100);
        tap();
        assertEquals(
                "Idle single tap must wait, never toggle Rambler back on",
                1,
                ime.nativeEvents.size());
        assertTrue(ime.normalEvents.isEmpty());
        advance(300);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        assertEquals(Arrays.asList(true, false), qou.I(ime).writes);
        assertFalse(ime.hiddenRamblerRecording);
        assertCleanUi();
    }

    @Test
    public void automaticEndThenDoubleTapMakesExactlyOneNewRamblerStart() {
        startInvisible();
        ime.hiddenRamblerRecording = false;
        advance(3100);
        tap();
        assertEquals(1, ime.nativeEvents.size());
        advance(100);
        tap();
        assertEquals(Arrays.asList(-10217, -10217), ime.nativeEvents);
        assertTrue(ime.hiddenRamblerRecording);
        assertEquals(Arrays.asList(true, true), qou.I(ime).writes);
        advance(1000);
        assertTrue(ime.normalEvents.isEmpty());
        assertCleanUi();
    }

    @Test
    public void observedTickRetiresLeaseSoEarlyAutomaticEndDoesNotRestartRambler() {
        startInvisible();
        showNativeTick();
        advance(100);
        ime.hiddenRamblerRecording = false;
        hideNativeTick();
        tap();
        assertEquals(1, ime.nativeEvents.size());
        advance(300);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        assertEquals(Arrays.asList(true, false), qou.I(ime).writes);
    }

    @Test
    public void lateVisibleTickStillFinishesAfterTheStartupLeaseHasExpired() {
        startInvisible();
        advance(3100);
        showNativeTick();
        tap();
        assertFalse(ime.hiddenRamblerRecording);
        assertEquals(2, ime.nativeEvents.size());
        assertTrue(ime.normalEvents.isEmpty());
        assertEquals(Collections.singletonList(true), qou.I(ime).writes);
        assertCleanUi();
    }

    @Test
    public void directStandardAfterAutomaticEndDoesNotToggleRamblerInstead() {
        startInvisible();
        ime.hiddenRamblerRecording = false;
        advance(3100);
        GboardMicBridge.startGoogleStandard(ime);
        assertEquals(1, ime.nativeEvents.size());
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        assertFalse(ime.hiddenRamblerRecording);
        assertEquals(Arrays.asList(true, false), qou.I(ime).writes);
    }

    @Test
    public void observedNativeRecorderThenSameModuleIdleRetiresFreshLease() {
        tap();
        advance(100);
        tap();
        assertTrue(ime.microphone.listening);
        assertTrue(GboardMicBridge.isRamblerActive(ime));
        ime.microphone.listening = false;
        advance(100);
        tap();
        assertEquals(1, ime.nativeEvents.size());
        advance(300);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        assertEquals(Arrays.asList(true, false), qou.I(ime).writes);
    }

    @Test
    public void directFinishWithOnlyNativeStateMustNotSendAToggle() {
        lite(fgf.LISTENING, true);
        GboardMicBridge.finishGoogleRambler(ime);
        assertTrue(ime.nativeEvents.isEmpty());
        assertTrue(ime.normalEvents.isEmpty());
        assertTrue(qou.I(ime).writes.isEmpty());
    }

    @Test
    public void defaultOffPreservesNativeSingleTapWithNoDelayOrModeWrite() {
        GboardPatchesSettings.preferences(ime)
                .edit()
                .remove(GboardMicGestureSettings.PREF_KEY_MODE)
                .commit();
        assertEquals(
                GboardMicGestureSettings.MODE_DEFAULT,
                GboardMicGestureSettings.readMode(GboardPatchesSettings.preferences(ime)));
        position();
        assertEquals(1, root.getChildCount());
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO, mic.getImportantForAccessibility());
        qou.I(ime).seed(true);
        mic.setOnTouchListener(
                (v, e) -> {
                    if (e.getActionMasked() == MotionEvent.ACTION_UP) ime.dG(oas.e(-10217, ""));
                    return true;
                });
        tap();
        assertEquals(Collections.singletonList(-10217), ime.normalEvents);
        assertTrue(qou.I(ime).writes.isEmpty());
        advance(1000);
        assertEquals(1, ime.normalEvents.size());
    }

    @Test
    public void offCancelsPendingStandardEvenWithoutAnotherDraw() {
        tap();
        setMode(GboardMicGestureSettings.MODE_DEFAULT);
        advance(1000);
        noActions();
        assertTrue(qou.I(ime).writes.isEmpty());
    }

    @Test
    public void offRestoresNativeAccessibilityAndDoesNotInterceptDoubleTap() {
        setMode(GboardMicGestureSettings.MODE_DEFAULT);
        position();
        final int[] releases = {0};
        mic.setOnTouchListener(
                (v, e) -> {
                    if (e.getActionMasked() == MotionEvent.ACTION_UP) releases[0]++;
                    return true;
                });
        tap();
        advance(100);
        position();
        tap();
        assertEquals(2, releases[0]);
        advance(1000);
        noActions();
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO, mic.getImportantForAccessibility());
    }

    @Test
    public void offDirectStartsCannotSelectOrDispatch() {
        setMode(GboardMicGestureSettings.MODE_DEFAULT);
        GboardMicBridge.startGoogleStandard(ime);
        GboardMicBridge.startGoogleRambler(ime);
        GboardMicBridge.finishGoogleRambler(ime);
        GboardMicBridge.switchGoogleMode(ime);
        GboardMicBridge.startGoogleSelected(ime);
        noActions();
        assertTrue(qou.I(ime).writes.isEmpty());
    }

    @Test
    public void turningOffRetiresFreshLeaseBeforeReenable() {
        ime.unobservableRambler = true;
        ((obl) ime.F).wrapper.h = null;
        tap();
        advance(100);
        tap();
        setMode(GboardMicGestureSettings.MODE_DEFAULT);
        position();
        setMode(GboardMicGestureSettings.MODE_HYBRID);
        position();
        tap();
        advance(300);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        assertEquals(1, ime.nativeEvents.size());
    }

    @Test
    public void advancedVoiceDisabledAlsoRestoresNativeRoute() {
        GboardPatchesSettings.preferences(ime)
                .edit()
                .putBoolean("pref_advanced_voice_typing_enabled", false)
                .commit();
        position();
        assertEquals(1, root.getChildCount());
        GboardMicBridge.startGoogleStandard(ime);
        noActions();
    }

    @Test
    public void unknownModeFallsBackToDefault() {
        GboardPatchesSettings.preferences(ime)
                .edit()
                .putString(GboardMicGestureSettings.PREF_KEY_MODE, "rambler")
                .commit();
        position();
        assertFalse(GboardMicGestureSettings.isEnabled(ime));
        assertEquals(1, root.getChildCount());
    }

    @Test
    public void quickOffOnCannotRevivePendingSingleTap() {
        tap();
        setMode(GboardMicGestureSettings.MODE_DEFAULT);
        setMode(GboardMicGestureSettings.MODE_HYBRID);
        advance(1000);
        noActions();
        assertTrue(qou.I(ime).writes.isEmpty());
    }

    GboardPatchesSettingsContract.SelectorRow micTapRow(GboardPatchesSettingsContract.Host host) {
        GboardPatchesSettingsContract.Screen screen =
                new com.akshaykadam.pixelboard.extension.advancedvoice
                                .GboardAdvancedVoiceSettingsFeature(ime)
                        .buildScreen(host);
        for (GboardPatchesSettingsContract.Row row : screen.getRows()) {
            if (row instanceof GboardPatchesSettingsContract.SelectorRow
                    && "Microphone tap".equals(row.getTitle())) {
                return (GboardPatchesSettingsContract.SelectorRow) row;
            }
        }
        throw new AssertionError("Microphone tap row missing");
    }

    @Test
    public void settingsScreenChoiceUsesEnglishFallbackAndSavesEachMode() {
        GboardPatchesSettings.preferences(ime)
                .edit()
                .remove(GboardMicGestureSettings.PREF_KEY_MODE)
                .commit();
        GboardPatchesSettingsContract.Host host = mock(GboardPatchesSettingsContract.Host.class);
        when(host.getContext()).thenReturn(ime);
        doCallRealMethod().when(host).submit(any());
        GboardPatchesSettingsContract.SelectorRow row = micTapRow(host);
        assertEquals("Default", row.getCurrentValue());
        assertTrue(row.isEnabled());
        assertTrue(row.getSummary().contains("300 ms"));

        row.getAction().run();
        ArgumentCaptor<String[]> labels = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String[]> values = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<GboardPatchesSettingsContract.StringValueConsumer> save =
                ArgumentCaptor.forClass(GboardPatchesSettingsContract.StringValueConsumer.class);
        verify(host)
                .showChoiceDialog(
                        eq("Microphone tap"),
                        labels.capture(),
                        values.capture(),
                        eq(GboardMicGestureSettings.MODE_DEFAULT),
                        any(),
                        any(),
                        save.capture());
        assertArrayEquals(new String[] {"Default", "Hybrid", "Quick switch"}, labels.getValue());
        assertArrayEquals(
                new String[] {
                    GboardMicGestureSettings.MODE_DEFAULT,
                    GboardMicGestureSettings.MODE_HYBRID,
                    GboardMicGestureSettings.MODE_QUICK_SWITCH
                },
                values.getValue());

        save.getValue().accept(GboardMicGestureSettings.MODE_QUICK_SWITCH);
        assertEquals(
                GboardMicGestureSettings.MODE_QUICK_SWITCH, GboardMicGestureSettings.mode(ime));
        assertEquals("Quick switch", micTapRow(host).getCurrentValue());
        save.getValue().accept(GboardMicGestureSettings.MODE_HYBRID);
        assertEquals("Hybrid", micTapRow(host).getCurrentValue());
        save.getValue().accept(GboardMicGestureSettings.MODE_DEFAULT);
        assertFalse(GboardMicGestureSettings.isEnabled(ime));
        assertEquals("Default", micTapRow(host).getCurrentValue());
    }

    @Test
    public void earlierOnOffSwitchCarriesOverAsHybridOrDefault() {
        android.content.SharedPreferences preferences = GboardPatchesSettings.preferences(ime);
        preferences
                .edit()
                .remove(GboardMicGestureSettings.PREF_KEY_MODE)
                .putBoolean("pref_rambler_double_tap_enabled", true)
                .commit();
        assertEquals(GboardMicGestureSettings.MODE_HYBRID, GboardMicGestureSettings.mode(ime));
        position();
        tap();
        advance(300);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);

        preferences.edit().putBoolean("pref_rambler_double_tap_enabled", false).commit();
        assertEquals(GboardMicGestureSettings.MODE_DEFAULT, GboardMicGestureSettings.mode(ime));

        preferences.edit().putBoolean("pref_rambler_double_tap_enabled", true).commit();
        setMode(GboardMicGestureSettings.MODE_QUICK_SWITCH);
        assertFalse(preferences.contains("pref_rambler_double_tap_enabled"));
        assertEquals(
                GboardMicGestureSettings.MODE_QUICK_SWITCH, GboardMicGestureSettings.mode(ime));
    }

    /** A keyboard process that started with Standard selected: Rambler is not loaded yet. */
    ModuleManager.ModuleInitListener standardLastRamblerModule(boolean[] loadedBeforeDispatch) {
        obr wrapper = ((obl) ime.F).wrapper;
        Object module = wrapper.h;
        wrapper.h = null;
        return ModuleManager.getInstance(ime)
                .register(
                        mvr.class,
                        GboardRambler1803OfficialSelectionRuntime::shouldEnableAgenticDictation,
                        () -> {
                            loadedBeforeDispatch[0] = ime.nativeEvents.isEmpty();
                            wrapper.h = module;
                        });
    }

    @Test
    public void standardLastRestartRefreshesRamblerReadinessBeforeDoubleTapStart() {
        boolean[] loadedBeforeDispatch = {false};
        ModuleManager.ModuleInitListener rambler = standardLastRamblerModule(loadedBeforeDispatch);
        tap();
        advance(100);
        tap();
        assertEquals(1, rambler.refreshes);
        assertTrue("Rambler must load before -10217 is sent", loadedBeforeDispatch[0]);
        assertNotNull(((obl) ime.F).wrapper.h);
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        assertTrue(ime.microphone.listening);
        assertTrue(ime.normalEvents.isEmpty());
    }

    @Test
    public void standardStartNeverRefreshesRamblerReadiness() {
        ModuleManager.ModuleInitListener rambler = standardLastRamblerModule(new boolean[1]);
        tap();
        advance(300);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        assertEquals(0, rambler.refreshes);
        assertNull(((obl) ime.F).wrapper.h);
    }

    @Test
    public void quickSwitchDoubleTapOnlySwitchesThenTapStartsTheSelectedMode() {
        setMode(GboardMicGestureSettings.MODE_QUICK_SWITCH);
        boolean[] loadedBeforeDispatch = {false};
        ModuleManager.ModuleInitListener rambler = standardLastRamblerModule(loadedBeforeDispatch);
        tap();
        advance(100);
        tap();
        advance(1000);
        noActions();
        assertEquals(Collections.singletonList(true), qou.I(ime).writes);
        assertEquals("Switching to Rambler refreshes its readiness", 1, rambler.refreshes);
        assertTrue(loadedBeforeDispatch[0]);

        tap();
        advance(299);
        noActions();
        advance(1);
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        assertEquals(Collections.singletonList(mnp.a), ime.nativeSources);
        assertTrue(ime.normalEvents.isEmpty());
        assertTrue(ime.microphone.listening);

        showNativeTick();
        tap();
        assertFalse(ime.microphone.listening);
        assertEquals(Arrays.asList(-10217, -10217), ime.nativeEvents);

        hideNativeTick();
        tap();
        advance(100);
        tap();
        advance(1000);
        assertEquals(Arrays.asList(true, true, false), qou.I(ime).writes);
        assertTrue(ime.normalEvents.isEmpty());
        assertEquals(2, ime.nativeEvents.size());

        tap();
        advance(300);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        assertEquals(2, ime.nativeEvents.size());
        assertCleanUi();
    }

    @Test
    public void quickSwitchNeedsBothTapsInTheSameField() {
        setMode(GboardMicGestureSettings.MODE_QUICK_SWITCH);
        tap();
        advance(100);
        ime.connection = mock(InputConnection.class);
        tap();
        advance(1000);
        assertEquals(
                "The second tap is a new single tap, not a switch",
                Collections.singletonList(false),
                qou.I(ime).writes);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        assertTrue(ime.nativeEvents.isEmpty());
    }

    @Test
    public void quickSwitchWritesRamblerAfterTheRefreshSoTheNewExtensionHearsIt() {
        setMode(GboardMicGestureSettings.MODE_QUICK_SWITCH);
        ModuleManager.ModuleInitListener rambler = standardLastRamblerModule(new boolean[1]);
        boolean[] heard = {false};
        qou.I(ime).onWrite = () -> heard[0] = ((obl) ime.F).wrapper.h != null;
        tap();
        advance(100);
        tap();
        advance(1000);
        assertEquals(1, rambler.refreshes);
        assertEquals(Collections.singletonList(true), qou.I(ime).writes);
        assertTrue("The preference write must come after Rambler loads", heard[0]);
        noActions();
    }

    @Test
    public void holdOnActiveRamblerTickIsJustATapOnRelease() {
        qou.I(ime).seed(true);
        ime.microphone.listening = true;
        showNativeTick();
        event(MotionEvent.ACTION_DOWN);
        advance(1000);
        assertTrue("Nothing happens while the finger is down", ime.microphone.listening);
        noActions();
        event(MotionEvent.ACTION_UP);
        assertFalse(ime.microphone.listening);
        assertEquals(Collections.singletonList(-10217), ime.nativeEvents);
        verify(ime, never()).startActivity(any());
    }

    @Test
    public void holdOnIdleMicIsJustATapOnRelease() {
        event(MotionEvent.ACTION_DOWN);
        advance(1000);
        noActions();
        event(MotionEvent.ACTION_UP);
        advance(299);
        noActions();
        advance(1);
        assertEquals(Collections.singletonList(-10042), ime.normalEvents);
        verify(ime, never()).startActivity(any());
    }
}
