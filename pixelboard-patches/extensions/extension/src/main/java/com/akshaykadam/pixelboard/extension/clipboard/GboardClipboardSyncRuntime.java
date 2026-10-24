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
    private static volatile Field contentObserverField;
    private static volatile Method syncMethod;
    private static volatile Method screenshotObserverMethod;
    private static volatile Method checkScreenshotMethod;

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
            triggerScreenshotCheck(handler);

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
            Method qMethod = resolveScreenshotObserverMethod(handler);
            if (qMethod != null) {
                qMethod.invoke(handler, Boolean.TRUE);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void triggerScreenshotCheck(Object handler) {
        // 1. Dispatch change through the ContentObserver (p) if initialized
        try {
            Field observerField = resolveContentObserverField(handler);
            if (observerField != null) {
                Object observer = observerField.get(handler);
                if (observer instanceof android.database.ContentObserver) {
                    try {
                        ((android.database.ContentObserver) observer).dispatchChange(false, null);
                    } catch (Throwable ignored) {
                        ((android.database.ContentObserver) observer).dispatchChange(false);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // 2. Directly invoke r() (check screenshot) on handler if available
        try {
            Method rMethod = resolveCheckScreenshotMethod(handler);
            if (rMethod != null) {
                rMethod.invoke(handler);
            }
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

    private static Method resolveScreenshotObserverMethod(Object handler) {
        if (screenshotObserverMethod == null) {
            Class<?> clazz = handler.getClass();
            while (clazz != null && clazz != Object.class) {
                try {
                    Method m = clazz.getDeclaredMethod("q", boolean.class);
                    m.setAccessible(true);
                    screenshotObserverMethod = m;
                    return screenshotObserverMethod;
                } catch (NoSuchMethodException ignored) {
                }
                clazz = clazz.getSuperclass();
            }
        }
        return screenshotObserverMethod;
    }

    private static Method resolveCheckScreenshotMethod(Object handler) {
        if (checkScreenshotMethod == null) {
            Class<?> clazz = handler.getClass();
            while (clazz != null && clazz != Object.class) {
                try {
                    Method m = clazz.getDeclaredMethod("r");
                    m.setAccessible(true);
                    checkScreenshotMethod = m;
                    return checkScreenshotMethod;
                } catch (NoSuchMethodException ignored) {
                }
                clazz = clazz.getSuperclass();
            }
        }
        return checkScreenshotMethod;
    }

    private static Field resolveContentObserverField(Object handler) {
        if (contentObserverField == null) {
            Class<?> clazz = handler.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (android.database.ContentObserver.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        contentObserverField = field;
                        return contentObserverField;
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }
        return contentObserverField;
    }
}
