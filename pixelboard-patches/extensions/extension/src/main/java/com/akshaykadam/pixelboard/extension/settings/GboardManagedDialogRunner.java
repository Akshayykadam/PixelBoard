package com.akshaykadam.pixelboard.extension.settings;

import java.util.concurrent.atomic.AtomicBoolean;

final class GboardManagedDialogRunner {
    private GboardManagedDialogRunner() {
    }

    static Throwable run(GboardPatchesSettingsContract.ManagedDialogAction action,
            Runnable onShown, Runnable onDismissed) {
        if (action == null) {
            return null;
        }
        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable completeOnce = () -> {
            if (completed.compareAndSet(false, true) && onDismissed != null) {
                try {
                    onDismissed.run();
                } catch (Throwable ignored) {
                    // Asynchronous dialog lifecycle callbacks must not affect the host app.
                }
            }
        };
        try {
            if (onShown != null) {
                onShown.run();
            }
            if (!action.show(completeOnce)) {
                completeOnce.run();
            }
            return null;
        } catch (Throwable throwable) {
            completeOnce.run();
            return throwable;
        }
    }
}
