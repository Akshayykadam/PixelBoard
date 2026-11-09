package com.akshaykadam.pixelboard.extension.clipboard;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GboardClipboardHistoryRuntimeTest {
    /** Stands in for Gboard's LabelSummarySliderPreference, which keeps its "Use default" index in P. */
    static final class FakeSlider {
        CharSequence[] K;
        CharSequence[] L;
        private final int P;
        int b;
        String saved = "";

        FakeSlider(int stockDefaultIndex) {
            P = stockDefaultIndex;
        }

        String w(String fallback) {
            return saved.isEmpty() ? fallback : saved;
        }
    }

    @Test
    public void useDefaultMovesTheClipboardSliderTo24Hours() throws Exception {
        FakeSlider slider = new FakeSlider(0);

        GboardClipboardHistoryRuntime.configureClipboardHistorySlider(slider, null);

        assertEquals("24", GboardClipboardHistoryRuntime.RETENTION_VALUES[GboardClipboardHistoryRuntime.DEFAULT_INDEX]);
        assertEquals(GboardClipboardHistoryRuntime.DEFAULT_INDEX, readP(slider));
        assertEquals(GboardClipboardHistoryRuntime.RETENTION_VALUES.length, slider.L.length);
    }

    private static int readP(FakeSlider slider) throws Exception {
        java.lang.reflect.Field field = FakeSlider.class.getDeclaredField("P");
        field.setAccessible(true);
        return field.getInt(slider);
    }
}
