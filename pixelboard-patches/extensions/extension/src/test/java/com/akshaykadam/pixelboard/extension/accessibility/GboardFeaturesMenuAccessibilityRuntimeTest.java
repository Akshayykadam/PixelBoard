package com.akshaykadam.pixelboard.extension.accessibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GboardFeaturesMenuAccessibilityRuntimeTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        GboardFeaturesMenuAccessibilityRuntime.resetForTesting();
    }

    @After
    public void tearDown() {
        GboardFeaturesMenuAccessibilityRuntime.resetForTesting();
    }

    @Test
    public void testOnAccessPointsPanelInflated_nullSafe() {
        GboardFeaturesMenuAccessibilityRuntime.onAccessPointsPanelInflated(null);
    }

    @Test
    public void testOnAccessPointsPanelInflated_setsPaneTitleAndImportance() {
        FrameLayout panel = new FrameLayout(context);
        GboardFeaturesMenuAccessibilityRuntime.onAccessPointsPanelInflated(panel);

        assertEquals("Features menu", panel.getAccessibilityPaneTitle());
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES, panel.getImportantForAccessibility());
    }

    @Test
    public void testFindFirstAccessibleAction_emptyHierarchyReturnsNull() {
        FrameLayout panel = new FrameLayout(context);
        assertNull(GboardFeaturesMenuAccessibilityRuntime.findFirstAccessibleAction(panel));
    }

    @Test
    public void testFindFirstAccessibleAction_findsFirstActionableChild() {
        FrameLayout panel = new FrameLayout(context);
        LinearLayout row = new LinearLayout(context);
        panel.addView(row);

        Button button1 = new Button(context);
        button1.setText("Text Editing");
        button1.setClickable(true);

        Button button2 = new Button(context);
        button2.setText("Translate");
        button2.setClickable(true);

        row.addView(button1);
        row.addView(button2);

        View found = GboardFeaturesMenuAccessibilityRuntime.findFirstAccessibleAction(panel);
        assertNotNull(found);
        assertEquals(button1, found);
    }

    @Test
    public void testFindFirstAccessibleAction_skipsHiddenAndUnimportantViews() {
        FrameLayout panel = new FrameLayout(context);
        LinearLayout row = new LinearLayout(context);
        panel.addView(row);

        Button hiddenButton = new Button(context);
        hiddenButton.setText("Hidden");
        hiddenButton.setVisibility(View.GONE);

        Button ignoredButton = new Button(context);
        ignoredButton.setText("Ignored");
        ignoredButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        View normalAction = new View(context);
        normalAction.setContentDescription("Settings");
        normalAction.setClickable(true);

        row.addView(hiddenButton);
        row.addView(ignoredButton);
        row.addView(normalAction);

        View found = GboardFeaturesMenuAccessibilityRuntime.findFirstAccessibleAction(panel);
        assertNotNull(found);
        assertEquals(normalAction, found);
    }

    @Test
    public void testIsAccessibleAction() {
        assertFalse(GboardFeaturesMenuAccessibilityRuntime.isAccessibleAction(null));

        FrameLayout container = new FrameLayout(context);
        assertFalse(GboardFeaturesMenuAccessibilityRuntime.isAccessibleAction(container));

        TextView textWithoutClick = new TextView(context);
        textWithoutClick.setText("Sample");
        textWithoutClick.setClickable(false);
        textWithoutClick.setFocusable(false);
        assertFalse(GboardFeaturesMenuAccessibilityRuntime.isAccessibleAction(textWithoutClick));

        Button button = new Button(context);
        assertTrue(GboardFeaturesMenuAccessibilityRuntime.isAccessibleAction(button));

        View customAction = new View(context);
        customAction.setClickable(true);
        customAction.setContentDescription("Action");
        assertTrue(GboardFeaturesMenuAccessibilityRuntime.isAccessibleAction(customAction));
    }

    @Test
    public void testOnAccessPointsPanelVisibilityChanged_triggersWhenEnabled() {
        GboardFeaturesMenuAccessibilityRuntime.forceAccessibilityEnabledForTesting = true;

        FrameLayout panel = new FrameLayout(context);
        Button button = new Button(context);
        button.setText("Clipboard");
        panel.addView(button);

        GboardFeaturesMenuAccessibilityRuntime.onAccessPointsPanelVisibilityChanged(panel);

        assertEquals("Features menu", panel.getAccessibilityPaneTitle());
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES, panel.getImportantForAccessibility());
    }

    @Test
    public void testOnAccessPointsPanelVisibilityChanged_debouncesRapidCalls() {
        GboardFeaturesMenuAccessibilityRuntime.forceAccessibilityEnabledForTesting = true;

        FrameLayout panel = new FrameLayout(context);
        GboardFeaturesMenuAccessibilityRuntime.onAccessPointsPanelVisibilityChanged(panel);

        long firstTimestamp = GboardFeaturesMenuAccessibilityRuntime.lastVisibilityEventTime;
        assertTrue(firstTimestamp > 0);

        // Immediate second call should be debounced
        GboardFeaturesMenuAccessibilityRuntime.onAccessPointsPanelVisibilityChanged(panel);
        assertEquals(firstTimestamp, GboardFeaturesMenuAccessibilityRuntime.lastVisibilityEventTime);
    }
}
