package dev.nosleep.tv;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class Prefs {
    private static final String PREFS = "nosleep_settings";
    private static final String KEY_KEEP_AWAKE_PACKAGES = "selected_packages";
    private static final String KEY_BACKGROUND_UNLOAD_PACKAGES = "background_unload_packages";
    private static final String KEY_BACKGROUND_UNLOAD_INTERVAL_PREFIX = "background_unload_interval_";

    static final long FIFTEEN_MINUTES_MS = 15L * 60L * 1000L;
    static final long THIRTY_MINUTES_MS = 30L * 60L * 1000L;
    static final long ONE_HOUR_MS = 60L * 60L * 1000L;
    static final long TWO_HOURS_MS = 2L * ONE_HOUR_MS;
    static final long FOUR_HOURS_MS = 4L * ONE_HOUR_MS;
    static final long EIGHT_HOURS_MS = 8L * ONE_HOUR_MS;
    static final long DEFAULT_BACKGROUND_UNLOAD_INTERVAL_MS = ONE_HOUR_MS;
    private static final long[] BACKGROUND_UNLOAD_INTERVALS_MS = {
            FIFTEEN_MINUTES_MS,
            THIRTY_MINUTES_MS,
            ONE_HOUR_MS,
            TWO_HOURS_MS,
            FOUR_HOURS_MS,
            EIGHT_HOURS_MS
    };

    private Prefs() {
    }

    static long[] getBackgroundUnloadIntervalsMs() {
        return BACKGROUND_UNLOAD_INTERVALS_MS.clone();
    }

    static Set<String> getKeepAwakePackages(Context context) {
        return getPackageSet(context, KEY_KEEP_AWAKE_PACKAGES);
    }

    static Set<String> getSelectedPackages(Context context) {
        return getKeepAwakePackages(context);
    }

    static Set<String> getBackgroundUnloadPackages(Context context) {
        return getPackageSet(context, KEY_BACKGROUND_UNLOAD_PACKAGES);
    }

    static boolean isKeepAwakeEnabled(Context context, String packageName) {
        return getKeepAwakePackages(context).contains(packageName);
    }

    static boolean isPackageSelected(Context context, String packageName) {
        return isKeepAwakeEnabled(context, packageName);
    }

    static void setKeepAwakeEnabled(Context context, String packageName, boolean enabled) {
        setPackageInSet(context, KEY_KEEP_AWAKE_PACKAGES, packageName, enabled);
    }

    static void setPackageSelected(Context context, String packageName, boolean selected) {
        setKeepAwakeEnabled(context, packageName, selected);
    }

    static boolean isBackgroundUnloadEnabled(Context context, String packageName) {
        return getBackgroundUnloadPackages(context).contains(packageName);
    }

    static void setBackgroundUnloadEnabled(Context context, String packageName, boolean enabled) {
        setPackageInSet(context, KEY_BACKGROUND_UNLOAD_PACKAGES, packageName, enabled);
    }

    static long getBackgroundUnloadIntervalMs(Context context, String packageName) {
        SharedPreferences prefs = preferences(context);
        long intervalMs = prefs.getLong(
                backgroundUnloadIntervalKey(packageName),
                DEFAULT_BACKGROUND_UNLOAD_INTERVAL_MS);
        return isSupportedBackgroundUnloadInterval(intervalMs)
                ? intervalMs
                : DEFAULT_BACKGROUND_UNLOAD_INTERVAL_MS;
    }

    static void setBackgroundUnloadIntervalMs(Context context, String packageName, long intervalMs) {
        long safeIntervalMs = isSupportedBackgroundUnloadInterval(intervalMs)
                ? intervalMs
                : DEFAULT_BACKGROUND_UNLOAD_INTERVAL_MS;
        preferences(context)
                .edit()
                .putLong(backgroundUnloadIntervalKey(packageName), safeIntervalMs)
                .apply();
    }

    private static Set<String> getPackageSet(Context context, String key) {
        SharedPreferences prefs = preferences(context);
        return new HashSet<>(prefs.getStringSet(key, Collections.emptySet()));
    }

    private static void setPackageInSet(Context context,
                                        String key,
                                        String packageName,
                                        boolean enabled) {
        Set<String> packages = getPackageSet(context, key);
        if (enabled) {
            packages.add(packageName);
        } else {
            packages.remove(packageName);
        }
        preferences(context)
                .edit()
                .putStringSet(key, packages)
                .apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String backgroundUnloadIntervalKey(String packageName) {
        return KEY_BACKGROUND_UNLOAD_INTERVAL_PREFIX + packageName;
    }

    private static boolean isSupportedBackgroundUnloadInterval(long intervalMs) {
        for (long supportedIntervalMs : BACKGROUND_UNLOAD_INTERVALS_MS) {
            if (supportedIntervalMs == intervalMs) {
                return true;
            }
        }
        return false;
    }
}
