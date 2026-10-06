/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.writingtools;

import android.os.Build;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hardware and model eligibility detector for Gboard on-device AI Writing Tools
 * (AICore / Astrea Private Inference via Gemini Nano).
 *
 * On-device "Suggested" style chips and freeform "Describe your edit" instructions
 * require Google Tensor hardware with sufficient RAM (Pixel 8 Pro, Pixel 9 series,
 * and Pixel 10+ series).
 *
 * @author Akshay Kadam (@Akshayykadam) - PixelBoard Project
 */
public final class GboardAiWritingToolsDeviceSupport {
    private static final Pattern PIXEL_NUMBER_PATTERN =
            Pattern.compile("(?i).*pixel\\s+(\\d+).*");

    private static final Set<String> KNOWN_SUPPORTED_DEVICES =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                    // Pixel 8 Pro (Tensor G3 with 12GB RAM)
                    "husky",
                    // Pixel 9 family (Tensor G4)
                    "tokay",    // Pixel 9
                    "caiman",   // Pixel 9 Pro
                    "komodo",   // Pixel 9 Pro XL
                    "comet",    // Pixel 9 Pro Fold
                    // Pixel 10 family (Tensor G5)
                    "frankel",
                    "blazer",
                    "mustang",
                    "rango"
            )));

    private static volatile Boolean forcedDeviceSupportForTesting = null;

    private GboardAiWritingToolsDeviceSupport() {
    }

    /**
     * Checks if the current hardware device is officially eligible for on-device
     * Gemini Nano / AICore Writing Tools capabilities.
     *
     * @return true if the device is a Pixel 8 Pro, Pixel 9+, or Pixel 10+ device.
     */
    public static boolean isSupportedDevice() {
        if (forcedDeviceSupportForTesting != null) {
            return forcedDeviceSupportForTesting.booleanValue();
        }
        return isSupportedDevice(Build.MANUFACTURER, Build.MODEL, Build.DEVICE);
    }

    /**
     * Evaluates device manufacturer, model, and device codename eligibility.
     */
    public static boolean isSupportedDevice(String manufacturer, String model, String device) {
        if (device != null) {
            String lowerDevice = device.trim().toLowerCase(Locale.US);
            if (KNOWN_SUPPORTED_DEVICES.contains(lowerDevice)) {
                return true;
            }
        }

        if (model != null) {
            String trimmedModel = model.trim();
            // Pixel 8 Pro has 12GB RAM and is officially supported for AICore / Gemini Nano
            // (Standard Pixel 8 'shiba' and 8a 'akita' only have 8GB RAM).
            if (trimmedModel.equalsIgnoreCase("Pixel 8 Pro")
                    || trimmedModel.toLowerCase(Locale.US).startsWith("pixel 8 pro")) {
                return true;
            }

            // Pixel 9, Pixel 9 Pro, Pixel 9 Pro XL, Pixel 9 Pro Fold, Pixel 10 Pro, etc.
            Matcher matcher = PIXEL_NUMBER_PATTERN.matcher(trimmedModel);
            if (matcher.matches()) {
                try {
                    int generation = Integer.parseInt(matcher.group(1));
                    if (generation >= 9) {
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return false;
    }

    /**
     * Testing hook to force device support check without mocking Android Build classes.
     */
    public static void setForcedDeviceSupportForTesting(Boolean supported) {
        forcedDeviceSupportForTesting = supported;
    }
}
