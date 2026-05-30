package dev.nosleep.tv;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

final class WakeKeeper {
    private WindowManager windowManager;
    private View overlayView;
    private PowerManager.WakeLock wakeLock;
    private boolean enabled;

    static boolean hasOverlayPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    synchronized void enable(Context context) {
        if (enabled) {
            return;
        }

        Context appContext = context.getApplicationContext();
        acquireWakeLock(appContext);
        addOverlayIfAllowed(appContext);
        enabled = true;
    }

    synchronized void disable() {
        removeOverlay();
        releaseWakeLock();
        enabled = false;
    }

    @SuppressWarnings("deprecation")
    private void acquireWakeLock(Context context) {
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                return;
            }
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,
                        "NoSleep:ScreenAwake");
                wakeLock.setReferenceCounted(false);
            }
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        } catch (RuntimeException ignored) {
            // The overlay path is the primary keep-screen-on mechanism.
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void addOverlayIfAllowed(Context context) {
        if (!hasOverlayPermission(context) || overlayView != null) {
            return;
        }

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            return;
        }

        overlayView = new View(context);
        overlayView.setBackgroundColor(Color.TRANSPARENT);
        overlayView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        int flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1,
                1,
                type,
                flags,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.START | Gravity.TOP;
        params.x = 0;
        params.y = 0;
        params.alpha = 0f;
        params.setTitle("NoSleep keep-awake");

        try {
            windowManager.addView(overlayView, params);
        } catch (RuntimeException ignored) {
            overlayView = null;
        }
    }

    private void removeOverlay() {
        if (windowManager == null || overlayView == null) {
            overlayView = null;
            return;
        }
        try {
            windowManager.removeView(overlayView);
        } catch (RuntimeException ignored) {
        } finally {
            overlayView = null;
        }
    }
}
