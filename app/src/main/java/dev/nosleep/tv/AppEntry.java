package dev.nosleep.tv;

import android.graphics.drawable.Drawable;

final class AppEntry {
    final String label;
    final String packageName;
    final Drawable icon;
    boolean keepAwakeEnabled;
    boolean backgroundUnloadEnabled;
    long backgroundUnloadIntervalMs;

    AppEntry(String label,
             String packageName,
             Drawable icon,
             boolean keepAwakeEnabled,
             boolean backgroundUnloadEnabled,
             long backgroundUnloadIntervalMs) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
        this.keepAwakeEnabled = keepAwakeEnabled;
        this.backgroundUnloadEnabled = backgroundUnloadEnabled;
        this.backgroundUnloadIntervalMs = backgroundUnloadIntervalMs;
    }

    boolean hasAnySettings() {
        return keepAwakeEnabled || backgroundUnloadEnabled;
    }
}
