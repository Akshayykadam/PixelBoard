package com.akshaykadam.pixelboard.extension.clipboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.preference.PreferenceManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class GboardClipboardRetentionTextRuntimeTest {
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        gboardPreferences().edit().clear().commit();
    }

    @Test
    public void unsetRetentionIsTheAdvertised24Hours() {
        assertEquals("24", GboardClipboardHistorySettings.DEFAULT_DURATION);
        assertEquals("24", GboardClipboardHistorySettings.readGboardDuration(context));
        assertFalse(GboardClipboardHistorySettings.isOneHourRetention(context));
    }

    @Test
    public void retentionIsReadFromGboardsDeviceProtectedPreferences() {
        setRetention("720");
        assertEquals("720", GboardClipboardHistorySettings.readGboardDuration(context));

        setRetention("1");
        assertTrue(GboardClipboardHistorySettings.isOneHourRetention(context));
    }

    @Test
    public void oneHourRetentionKeepsGooglesTexts() {
        setRetention("1");

        assertEquals(GboardClipboardRetentionTextRuntime.PIN_TIP,
                GboardClipboardRetentionTextRuntime.resolveText(
                        GboardClipboardRetentionTextRuntime.PIN_TIP));
        assertEquals(GboardClipboardRetentionTextRuntime.INTRO_CARD,
                GboardClipboardRetentionTextRuntime.resolveText(
                        GboardClipboardRetentionTextRuntime.INTRO_CARD));
        assertEquals(GboardClipboardRetentionTextRuntime.EMPTY_SUBTITLE,
                GboardClipboardRetentionTextRuntime.resolveText(
                        GboardClipboardRetentionTextRuntime.EMPTY_SUBTITLE));

        // 18.6.3 texts
        assertEquals(GboardClipboardRetentionTextRuntime.PIN_TIP_1863,
                GboardClipboardRetentionTextRuntime.resolveText(
                        GboardClipboardRetentionTextRuntime.PIN_TIP_1863));
        assertEquals(GboardClipboardRetentionTextRuntime.INTRO_CARD_1863,
                GboardClipboardRetentionTextRuntime.resolveText(
                        GboardClipboardRetentionTextRuntime.INTRO_CARD_1863));
        assertEquals(GboardClipboardRetentionTextRuntime.EMPTY_SUBTITLE_1863,
                GboardClipboardRetentionTextRuntime.resolveText(
                        GboardClipboardRetentionTextRuntime.EMPTY_SUBTITLE_1863));
    }

    @Test
    public void otherRetentionsSwapTheOneHourTexts() {
        setRetention("24");

        assertEquals(GboardClipboardRetentionTextRuntime.DESCRIPTION,
                GboardClipboardRetentionTextRuntime.resolveText(
                        GboardClipboardRetentionTextRuntime.INTRO_CARD));
        assertEquals(GboardClipboardRetentionTextRuntime.DESCRIPTION,
                GboardClipboardRetentionTextRuntime.resolveText(
                        GboardClipboardRetentionTextRuntime.EMPTY_SUBTITLE));
        // Without the patched-in tips (as in this test app), Google's tip stays.
        assertEquals(GboardClipboardRetentionTextRuntime.PIN_TIP,
                GboardClipboardRetentionTextRuntime.resolveText(
                        GboardClipboardRetentionTextRuntime.PIN_TIP));

        // 18.6.3 texts swapped
        assertEquals(GboardClipboardRetentionTextRuntime.DESCRIPTION_1863,
                GboardClipboardRetentionTextRuntime.resolveText(
                        GboardClipboardRetentionTextRuntime.INTRO_CARD_1863));
        assertEquals(GboardClipboardRetentionTextRuntime.DESCRIPTION_1863,
                GboardClipboardRetentionTextRuntime.resolveText(
                        GboardClipboardRetentionTextRuntime.EMPTY_SUBTITLE_1863));
        assertEquals(GboardClipboardRetentionTextRuntime.PIN_TIP_1863,
                GboardClipboardRetentionTextRuntime.resolveText(
                        GboardClipboardRetentionTextRuntime.PIN_TIP_1863));
        assertEquals(0x7f140000, GboardClipboardRetentionTextRuntime.resolveText(0x7f140000));
    }

    @Test
    public void pinTipNamesTheChosenDuration() {
        assertEquals("pixelboard_clipboard_pin_tip_24h",
                GboardClipboardRetentionTextRuntime.pinTipName("24"));
        assertEquals("pixelboard_clipboard_pin_tip_7d",
                GboardClipboardRetentionTextRuntime.pinTipName("168"));
        assertEquals("pixelboard_clipboard_pin_tip_30d",
                GboardClipboardRetentionTextRuntime.pinTipName("720"));
        // Never-deleted clips need no duration, only how to pin.
        assertEquals("pixelboard_clipboard_pin_tip_short",
                GboardClipboardRetentionTextRuntime.pinTipName("876000"));
    }

    private void setRetention(String hours) {
        gboardPreferences().edit()
                .putString(GboardClipboardHistorySettings.GBOARD_NATIVE_PREF_KEY, hours)
                .commit();
    }

    private android.content.SharedPreferences gboardPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(
                context.createDeviceProtectedStorageContext());
    }
}
