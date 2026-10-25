package com.akshaykadam.pixelboard.extension.bengalivoice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en")
public final class GboardBengaliVoiceSettingsUiTest {
    private Activity activity;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        GboardBengaliVoiceSettings.writeMode(activity, "bn-BD", GboardBengaliVoicePolicy.STANDARD);
        GboardBengaliVoiceSettings.writeMode(activity, "bn-IN", GboardBengaliVoicePolicy.STANDARD);
    }

    @Test
    public void editedBengaliLanguageOwnsRowAndRepeatedRebuildDoesNotDuplicateIt() throws Exception {
        FakeGroup group = new FakeGroup(activity);
        FakePreference nativeRow = new FakePreference(activity);
        nativeRow.P("native_multilingual");
        group.an(nativeRow);
        FakeFragment fragment = new FakeFragment("bn-BD", group);

        attach(fragment);
        attach(fragment);

        assertEquals(2, group.rows.size());
        assertSame(nativeRow, group.rows.get(0));
        assertEquals("Voice typing", group.rows.get(1).title);
        assertTrue(group.rows.get(1).summary.toString().startsWith("Standard"));
        assertFalse(group.rows.get(1).persistent);
        assertTrue(group.z);

        group.rows.clear(); // The native aU() method clears this group before rebuilding it.
        fragment.am = "bn-IN";
        attach(fragment);
        assertEquals(1, group.rows.size());
    }

    @Test
    public void otherOrMissingEditedLocaleCannotInheritBengaliSelector() throws Exception {
        FakeGroup group = new FakeGroup(activity);
        group.z = false;
        FakeFragment fragment = new FakeFragment(null, group);
        attach(fragment);
        assertTrue(group.rows.isEmpty());
        assertFalse(group.z);

        fragment.am = "bn-BD";
        attach(fragment);
        assertEquals(1, group.rows.size());
        fragment.am = "en-US";
        attach(fragment);
        assertTrue(group.rows.isEmpty());
        assertTrue("Non-Bengali attachment preserves host category visibility", group.z);
    }

    @Test
    public void newNonBengaliFragmentLeavesNativeRowsAndVisibilityUntouched() throws Exception {
        FakeGroup group = new FakeGroup(activity);
        FakePreference nativeRow = new FakePreference(activity);
        nativeRow.P("native_multilingual");
        group.an(nativeRow);
        group.z = false;

        attach(new FakeFragment("en-US", group));

        assertEquals(1, group.rows.size());
        assertSame(nativeRow, group.rows.get(0));
        assertFalse(group.z);
    }

    @Test
    public void hostResourceIdCollisionsCannotReplaceSelectorLabels() throws Exception {
        Resources hostResources = activity.getResources();
        Resources collidingResources = new Resources(hostResources.getAssets(),
                hostResources.getDisplayMetrics(), hostResources.getConfiguration()) {
            @Override
            public String getString(int resource) { return "unrelated host resource"; }
            @Override
            public String getString(int resource, Object... arguments) {
                return "unrelated formatted host resource";
            }
        };
        Context hostContext = new ContextWrapper(activity) {
            @Override
            public Resources getResources() { return collidingResources; }
        };
        FakeGroup group = new FakeGroup(hostContext);

        attach(new FakeFragment("bn-BD", group));

        assertEquals("Voice typing", group.rows.get(0).title);
        assertTrue(group.rows.get(0).summary.toString().startsWith("Standard"));
    }

    @Test
    public void nativeChoicesPersistOnlyTheEditedBengaliLocaleAndRefreshSummary() throws Exception {
        FakeGroup group = new FakeGroup(activity);
        attach(new FakeFragment("bn-BD", group));
        FakePreference row = group.rows.get(0);

        assertTrue(row.o.b(row));
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(2, dialog.getListView().getAdapter().getCount());
        assertEquals(0, dialog.getListView().getCheckedItemPosition());
        dialog.getListView().performItemClick(null, 1, 1);

        assertEquals(GboardBengaliVoicePolicy.RAMBLER,
                GboardBengaliVoiceSettings.readMode(activity, "bn-BD"));
        assertEquals(GboardBengaliVoicePolicy.STANDARD,
                GboardBengaliVoiceSettings.readMode(activity, "bn-IN"));
        assertTrue(row.summary.toString().startsWith("Rambler"));
        assertFalse(dialog.isShowing());
    }

    @Test
    public void cancelLeavesSavedModeUnchanged() throws Exception {
        FakeGroup group = new FakeGroup(activity);
        attach(new FakeFragment("bn-BD", group));
        FakePreference row = group.rows.get(0);
        row.o.b(row);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        assertEquals(GboardBengaliVoicePolicy.STANDARD,
                GboardBengaliVoiceSettings.readMode(activity, "bn-BD"));
    }

    @Test
    public void persistenceFailureDoesNotReportTheUnsavedChoiceAsCurrent() throws Exception {
        Context unavailableStorage = new ContextWrapper(activity) {
            @Override
            public Context getApplicationContext() { return this; }
            @Override
            public boolean isDeviceProtectedStorage() { return true; }
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                throw new IllegalStateException("Simulated unavailable storage");
            }
        };
        FakeGroup group = new FakeGroup(unavailableStorage);
        attach(new FakeFragment("bn-BD", group));
        FakePreference row = group.rows.get(0);
        CharSequence previousSummary = row.summary;
        row.o.b(row);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();

        dialog.getListView().performItemClick(null, 1, 1);

        assertEquals(previousSummary, row.summary);
        assertEquals(GboardBengaliVoicePolicy.STANDARD,
                GboardBengaliVoiceSettings.readMode(unavailableStorage, "bn-BD"));
        assertFalse(dialog.isShowing());
    }

    @Test
    public void failedNativeAttachmentRestoresPreviousRowAndVisibility() throws Exception {
        FakeGroup group = new FakeGroup(activity);
        FakeFragment fragment = new FakeFragment("bn-BD", group);
        attach(fragment);
        FakePreference previous = group.rows.get(0);
        group.z = false;
        group.failAfterAdd = true;

        try {
            attach(fragment);
            fail("Expected native attachment failure");
        } catch (InvocationTargetException expected) {
            assertTrue(expected.getCause() instanceof IllegalStateException);
        }

        assertEquals(1, group.rows.size());
        assertSame(previous, group.rows.get(0));
        assertFalse(group.z);
    }

    private static void attach(FakeFragment fragment) throws ReflectiveOperationException {
        GboardBengaliVoiceSettingsUi.attach(fragment, FakePreference.class, FakeGroup.class);
    }

    public static final class FakeFragment {
        Object am;
        FakeGroup ar;

        FakeFragment(String editedTag, FakeGroup group) {
            am = editedTag;
            ar = group;
        }
    }

    public interface ClickListener {
        boolean b(FakePreference preference);
    }

    public static class FakePreference {
        public Context j;
        public ClickListener o;
        public boolean z = true;
        String key;
        CharSequence title;
        CharSequence summary;
        boolean persistent = true;

        public FakePreference(Context context) { j = context; }
        public void P(String value) { key = value; }
        public void V(CharSequence value) { title = value; }
        public void n(CharSequence value) { summary = value; }
        public void Q(int value) { }
        public void O(boolean value) { persistent = value; }
        public void W(boolean value) { z = value; }
    }

    public static final class FakeGroup extends FakePreference {
        final List<FakePreference> rows = new ArrayList<>();
        boolean failAfterAdd;

        public FakeGroup(Context context) { super(context); }
        public FakePreference l(CharSequence key) {
            for (FakePreference row : rows) {
                if (key.equals(row.key)) { return row; }
            }
            return null;
        }
        public void an(FakePreference preference) {
            rows.add(preference);
            if (failAfterAdd) {
                failAfterAdd = false;
                throw new IllegalStateException("Simulated native add failure");
            }
        }
        public void ao(FakePreference preference) { rows.remove(preference); }
    }
}
