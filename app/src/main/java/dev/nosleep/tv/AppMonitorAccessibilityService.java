package dev.nosleep.tv;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashMap;
import java.util.Map;

public class AppMonitorAccessibilityService extends AccessibilityService {
    private static final String TAG = "NoSleepMonitor";
    private final WakeKeeper wakeKeeper = new WakeKeeper();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Runnable> pendingBackgroundUnloads = new HashMap<>();
    private String guardedPackage;
    private String foregroundPackage;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOWS_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) {
            return;
        }

        setForegroundPackage(event.getPackageName().toString());
    }

    @Override
    public void onInterrupt() {
        setGuardedPackage(null);
    }

    @Override
    public void onDestroy() {
        setGuardedPackage(null);
        clearPendingBackgroundUnloads();
        super.onDestroy();
    }

    private void setForegroundPackage(String packageName) {
        if (packageName.equals(foregroundPackage)) {
            updateWakeGuard(packageName);
            return;
        }

        String previousPackage = foregroundPackage;
        foregroundPackage = packageName;
        cancelBackgroundUnload(packageName);

        if (previousPackage != null && !previousPackage.equals(packageName)) {
            scheduleBackgroundUnloadIfNeeded(previousPackage);
        }

        updateWakeGuard(packageName);
    }

    private void updateWakeGuard(String packageName) {
        if (packageName.equals(getPackageName())) {
            setGuardedPackage(null);
            return;
        }

        if (Prefs.isKeepAwakeEnabled(this, packageName)) {
            setGuardedPackage(packageName);
        } else {
            setGuardedPackage(null);
        }
    }

    private void scheduleBackgroundUnloadIfNeeded(String packageName) {
        if (packageName.equals(getPackageName())
                || !Prefs.isBackgroundUnloadEnabled(this, packageName)) {
            cancelBackgroundUnload(packageName);
            return;
        }

        cancelBackgroundUnload(packageName);
        long intervalMs = Prefs.getBackgroundUnloadIntervalMs(this, packageName);
        Runnable task = () -> {
            pendingBackgroundUnloads.remove(packageName);
            if (packageName.equals(foregroundPackage)
                    || !Prefs.isBackgroundUnloadEnabled(this, packageName)) {
                return;
            }
            ShizukuForceStopper.forceStop(this, packageName, (success, message) ->
                    Log.i(TAG, "force-stop " + packageName + ": " + success + " (" + message + ")"));
        };
        pendingBackgroundUnloads.put(packageName, task);
        handler.postDelayed(task, intervalMs);
    }

    private void cancelBackgroundUnload(String packageName) {
        Runnable task = pendingBackgroundUnloads.remove(packageName);
        if (task != null) {
            handler.removeCallbacks(task);
        }
    }

    private void clearPendingBackgroundUnloads() {
        for (Runnable task : pendingBackgroundUnloads.values()) {
            handler.removeCallbacks(task);
        }
        pendingBackgroundUnloads.clear();
    }

    private void setGuardedPackage(String packageName) {
        if (packageName != null && packageName.equals(guardedPackage)) {
            return;
        }
        if (packageName == null && guardedPackage == null) {
            return;
        }
        guardedPackage = packageName;
        if (guardedPackage == null) {
            wakeKeeper.disable();
        } else {
            wakeKeeper.enable(this);
        }
    }
}
