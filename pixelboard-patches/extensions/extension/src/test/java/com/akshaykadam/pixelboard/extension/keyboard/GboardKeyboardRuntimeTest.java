package com.akshaykadam.pixelboard.extension.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GboardKeyboardRuntimeTest {
    private enum TestAction {
        PRESS,
        UP,
        DOWN,
        LONG_PRESS,
        SLIDE_UP,
        SLIDE_DOWN
    }

    @Test
    public void nextSelectionCaseText_returnsNullWhenEmptyOrUncased() {
        assertNull(GboardKeyboardRuntime.nextSelectionCaseText(null));
        assertNull(GboardKeyboardRuntime.nextSelectionCaseText(""));
        assertNull(GboardKeyboardRuntime.nextSelectionCaseText("   "));
        assertNull(GboardKeyboardRuntime.nextSelectionCaseText("12345 !@#"));
    }

    @Test
    public void nextSelectionCaseText_cyclesMultiWordLowerTitleUpper() {
        assertEquals("Hello World", GboardKeyboardRuntime.nextSelectionCaseText("hello world"));
        assertEquals("HELLO WORLD", GboardKeyboardRuntime.nextSelectionCaseText("Hello World"));
        assertEquals("hello world", GboardKeyboardRuntime.nextSelectionCaseText("HELLO WORLD"));
    }

    @Test
    public void nextSelectionCaseText_handlesMixedCaseMultiWordSelection() {
        String step1 = GboardKeyboardRuntime.nextSelectionCaseText("ig the igwuTeh the t you yuo yuo");
        assertEquals("Ig The Igwuteh The T You Yuo Yuo", step1);

        String step2 = GboardKeyboardRuntime.nextSelectionCaseText(step1);
        assertEquals("IG THE IGWUTEH THE T YOU YUO YUO", step2);

        String step3 = GboardKeyboardRuntime.nextSelectionCaseText(step2);
        assertEquals("ig the igwuteh the t you yuo yuo", step3);
    }

    @Test
    public void nextSelectionCaseText_preservesApostrophesAndSingleLetterWords() {
        assertEquals("Don't Stop Now", GboardKeyboardRuntime.nextSelectionCaseText("don't stop now"));
        assertEquals("DON'T STOP NOW", GboardKeyboardRuntime.nextSelectionCaseText("Don't Stop Now"));
        assertEquals("don't stop now", GboardKeyboardRuntime.nextSelectionCaseText("DON'T STOP NOW"));

        assertEquals("i", GboardKeyboardRuntime.nextSelectionCaseText("I"));
        assertEquals("I", GboardKeyboardRuntime.nextSelectionCaseText("i"));
    }

    @Test
    public void nextSelectionCaseText_handlesHyphensNewlinesAndNumbers() {
        assertEquals(
                "Multi-Word\n2nd Line",
                GboardKeyboardRuntime.nextSelectionCaseText("multi-word\n2nd line"));
    }

    @Test
    public void nextSelectionCaseForKeyEvent_handlesSoftAndHardwareShiftEvents() {
        assertEquals(
                "HELLO WORLD",
                GboardKeyboardRuntime.nextSelectionCaseForKeyEvent(
                        -10033, TestAction.UP, 3, "Hello World"));
        assertEquals(
                "hello world",
                GboardKeyboardRuntime.nextSelectionCaseForKeyEvent(
                        -10013, TestAction.UP, 2, "HELLO WORLD"));
        assertEquals(
                "Hello World",
                GboardKeyboardRuntime.nextSelectionCaseForKeyEvent(
                        -10012, TestAction.PRESS, 2, "hello world"));
        assertEquals(
                "HELLO WORLD",
                GboardKeyboardRuntime.nextSelectionCaseForKeyEvent(
                        59, TestAction.PRESS, 7, "Hello World"));
        assertEquals(
                "hello world",
                GboardKeyboardRuntime.nextSelectionCaseForKeyEvent(
                        115, null, 9, "HELLO WORLD"));

        assertNull(
                GboardKeyboardRuntime.nextSelectionCaseForKeyEvent(
                        -10032, TestAction.DOWN, 3, "hello world"));
        assertNull(
                GboardKeyboardRuntime.nextSelectionCaseForKeyEvent(
                        -10032, TestAction.UP, 3, "hello world"));
        assertNull(
                GboardKeyboardRuntime.nextSelectionCaseForKeyEvent(
                        65, TestAction.PRESS, 2, "hello world"));
        assertNull(
                GboardKeyboardRuntime.nextSelectionCaseForKeyEvent(
                        -10033, TestAction.LONG_PRESS, 3, "hello world"));
    }

    @Test
    public void shouldShowKeyPopup_blocksAllPressPopupsWhenSettingOff() {
        assertFalse(GboardKeyboardRuntime.shouldShowKeyPopup(false, TestAction.PRESS, false, 1));
        assertFalse(GboardKeyboardRuntime.shouldShowKeyPopup(false, TestAction.PRESS, true, 1));
        assertFalse(GboardKeyboardRuntime.shouldShowKeyPopup(false, TestAction.PRESS, false, 3));
        assertTrue(GboardKeyboardRuntime.isPressPopupBlocked(false, TestAction.PRESS));
    }

    @Test
    public void shouldShowKeyPopup_preservesPopupWhenEnabledAndAllowsNonPressPopups() {
        assertTrue(GboardKeyboardRuntime.shouldShowKeyPopup(true, TestAction.PRESS, false, 1));
        assertFalse(GboardKeyboardRuntime.isPressPopupBlocked(true, TestAction.PRESS));

        assertFalse(GboardKeyboardRuntime.shouldShowKeyPopup(false, TestAction.LONG_PRESS, false, 1));
        assertFalse(GboardKeyboardRuntime.shouldShowKeyPopup(true, TestAction.LONG_PRESS, false, 1));
        assertFalse(GboardKeyboardRuntime.isPressPopupBlocked(false, TestAction.LONG_PRESS));

        assertTrue(GboardKeyboardRuntime.shouldShowKeyPopup(false, TestAction.SLIDE_UP, true, 1));
        assertTrue(GboardKeyboardRuntime.shouldShowKeyPopup(false, TestAction.SLIDE_DOWN, false, 2));
        assertFalse(GboardKeyboardRuntime.shouldShowKeyPopup(false, TestAction.SLIDE_UP, false, 1));
    }
}
