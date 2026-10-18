package com.akshaykadam.pixelboard.extension.clipboard;

import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Runtime helper for clipboard history synchronization.
 *
 * Resolves Issue #17 where clipboard history is always empty on Samsung One UI
 * and Vivo OriginOS/FuntouchOS devices due to OEM suppression of background
 * OnPrimaryClipChangedListener broadcasts to third-party keyboards.
 *
 * When Gboard gains input focus in the foreground (onStartInputView), this helper
 * ensures the ClipboardManager is initialized and triggers clipboard synchronization,
 * allowing Gboard to capture the primary clip and persist it to SQLite history.
 */
public final class GboardClipboardSyncRuntime {

    private static final String TAG = "PixelBoardClipboard";
    private static volatile Field clipboardManagerField;
    private static volatile Field contextField;
    private static volatile Method syncMethod;

    private GboardClipboardSyncRuntime() {
    }

    /**
     * Synchronizes clipboard contents when input starts.
     *
     * @param handler the Gboard ClipboardDataHandler instance (Lfmg)
     */
    public static void syncOnStartInputView(Object handler) {
        if (handler == null) {
            return;
        }
        try {
            ensureClipboardManager(handler);
            ensureScreenshotObserver(handler);

            // Gboard's ClipboardDataHandler implements ClipboardManager.OnPrimaryClipChangedListener.
            // Invoking onPrimaryClipChanged() evaluates the user's opt-in setting and asynchronously
            // reads the foreground primary clip and writes it to SQLite database history.
            if (handler instanceof ClipboardManager.OnPrimaryClipChangedListener) {
                ((ClipboardManager.OnPrimaryClipChangedListener) handler).onPrimaryClipChanged();
                return;
            }

            // Fallback via reflection if needed
            Method sync = resolveSyncMethod(handler);
            if (sync != null) {
                sync.invoke(handler);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to sync clipboard onStartInputView", t);
        }
    }

    private static void ensureScreenshotObserver(Object handler) {
        try {
            Method qMethod = handler.getClass().getDeclaredMethod("q", boolean.class);
            qMethod.setAccessible(true);
            qMethod.invoke(handler, Boolean.TRUE);
        } catch (Throwable ignored) {
        }
    }

    private static void ensureClipboardManager(Object handler) {
        try {
            Field cmField = resolveClipboardManagerField(handler);
            if (cmField != null && cmField.get(handler) == null) {
                Field ctxField = resolveContextField(handler);
                if (ctxField != null) {
                    Context context = (Context) ctxField.get(handler);
                    if (context != null) {
                        ClipboardManager cm =
                                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) {
                            cmField.set(handler, cm);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Field resolveClipboardManagerField(Object handler) {
        if (clipboardManagerField == null) {
            for (Field field : handler.getClass().getDeclaredFields()) {
                if (ClipboardManager.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    clipboardManagerField = field;
                    break;
                }
            }
        }
        return clipboardManagerField;
    }

    private static Field resolveContextField(Object handler) {
        if (contextField == null) {
            for (Field field : handler.getClass().getDeclaredFields()) {
                if (Context.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    contextField = field;
                    break;
                }
            }
        }
        return contextField;
    }

    private static Method resolveSyncMethod(Object handler) {
        if (syncMethod == null) {
            try {
                Method m = handler.getClass().getDeclaredMethod("f");
                m.setAccessible(true);
                syncMethod = m;
            } catch (Throwable ignored) {
            }
        }
        return syncMethod;
    }
}
