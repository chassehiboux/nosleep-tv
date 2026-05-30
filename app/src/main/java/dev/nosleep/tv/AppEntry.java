package dev.nosleep.tv;

import android.graphics.drawable.Drawable;

final class AppEntry {
    final String label;
    final String packageName;
    final Drawable icon;
    boolean selected;

    AppEntry(String label, String packageName, Drawable icon, boolean selected) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
        this.selected = selected;
    }
}
