package dev.nosleep.tv;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;

public class AppMonitorAccessibilityService extends AccessibilityService {
    private final WakeKeeper wakeKeeper = new WakeKeeper();
    private String guardedPackage;

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

        String packageName = event.getPackageName().toString();
        if (packageName.equals(getPackageName())) {
            setGuardedPackage(null);
            return;
        }

        if (Prefs.isPackageSelected(this, packageName)) {
            setGuardedPackage(packageName);
        } else {
            setGuardedPackage(null);
        }
    }

    @Override
    public void onInterrupt() {
        setGuardedPackage(null);
    }

    @Override
    public void onDestroy() {
        setGuardedPackage(null);
        super.onDestroy();
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
