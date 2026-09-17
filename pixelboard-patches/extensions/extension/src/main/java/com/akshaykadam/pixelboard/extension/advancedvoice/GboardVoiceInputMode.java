package com.akshaykadam.pixelboard.extension.advancedvoice;

/** Resolves the one effective voice backend selected by the Patches settings. */
public enum GboardVoiceInputMode {
    STANDARD,
    ADVANCED,
    RAMBLER;

    public static GboardVoiceInputMode resolve(boolean enhancedVoiceEnabled,
            String selectedBackend) {
        if (!enhancedVoiceEnabled) {
            return STANDARD;
        }
        if (GboardAdvancedVoiceSettings.BACKEND_RAMBLER.equals(selectedBackend)) {
            return RAMBLER;
        }
        return ADVANCED;
    }
}
