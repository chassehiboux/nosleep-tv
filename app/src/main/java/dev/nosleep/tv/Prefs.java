package dev.nosleep.tv;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class Prefs {
    private static final String PREFS = "nosleep_settings";
    private static final String KEY_SELECTED_PACKAGES = "selected_packages";

    private Prefs() {
    }

    static Set<String> getSelectedPackages(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(KEY_SELECTED_PACKAGES, Collections.emptySet()));
    }

    static boolean isPackageSelected(Context context, String packageName) {
        return getSelectedPackages(context).contains(packageName);
    }

    static void setPackageSelected(Context context, String packageName, boolean selected) {
        Set<String> packages = getSelectedPackages(context);
        if (selected) {
            packages.add(packageName);
        } else {
            packages.remove(packageName);
        }
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_SELECTED_PACKAGES, packages)
                .apply();
    }
}
