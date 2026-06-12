package dev.nosleep.tv;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

final class ShizukuForceStopper {
    static final int REQUEST_PERMISSION_CODE = 2001;
    private static final Object LOCK = new Object();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final List<PendingRequest> PENDING_REQUESTS = new ArrayList<>();

    private static INoSleepPrivilegedService service;
    private static boolean binding;

    interface Callback {
        void onResult(boolean success, String message);
    }

    private ShizukuForceStopper() {
    }

    static boolean isAvailable() {
        try {
            return Shizuku.pingBinder() && !Shizuku.isPreV11();
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean hasPermission() {
        try {
            return isAvailable() && Shizuku.checkSelfPermission() == PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean shouldShowPermissionRationale() {
        try {
            return isAvailable() && Shizuku.shouldShowRequestPermissionRationale();
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean requestPermissionIfNeeded() {
        if (hasPermission()) {
            return true;
        }
        if (!isAvailable() || shouldShowPermissionRationale()) {
            return false;
        }
        try {
            Shizuku.requestPermission(REQUEST_PERMISSION_CODE);
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static int getUid() {
        try {
            return isAvailable() ? Shizuku.getUid() : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    static void forceStop(Context context, String packageName, Callback callback) {
        Context appContext = context.getApplicationContext();
        if (!hasPermission()) {
            notifyResult(callback, false, "Shizuku is not ready or permission is missing");
            return;
        }

        synchronized (LOCK) {
            if (service != null) {
                executeForceStop(service, packageName, callback);
                return;
            }

            PENDING_REQUESTS.add(new PendingRequest(packageName, callback));
            if (!binding) {
                binding = true;
                bindUserService(appContext);
            }
        }
    }

    private static void bindUserService(Context context) {
        try {
            Shizuku.bindUserService(userServiceArgs(context), CONNECTION);
        } catch (Throwable throwable) {
            failPending("Could not bind Shizuku user service: " + throwable.getClass().getSimpleName());
        }
    }

    private static Shizuku.UserServiceArgs userServiceArgs(Context context) {
        return new Shizuku.UserServiceArgs(new ComponentName(
                context.getPackageName(),
                PrivilegedForceStopService.class.getName()))
                .daemon(false)
                .processNameSuffix("force_stop")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE);
    }

    private static final ServiceConnection CONNECTION = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            INoSleepPrivilegedService connectedService =
                    INoSleepPrivilegedService.Stub.asInterface(binder);
            if (connectedService == null) {
                failPending("Invalid Shizuku user service binder");
                return;
            }
            List<PendingRequest> requests;
            synchronized (LOCK) {
                service = connectedService;
                binding = false;
                requests = new ArrayList<>(PENDING_REQUESTS);
                PENDING_REQUESTS.clear();
            }
            for (PendingRequest request : requests) {
                executeForceStop(connectedService, request.packageName, request.callback);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (LOCK) {
                service = null;
                binding = false;
            }
        }
    };

    private static void executeForceStop(INoSleepPrivilegedService service,
                                         String packageName,
                                         Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                int result = service.forceStopPackage(packageName);
                boolean success = result == 0;
                notifyResult(callback, success, "force-stop exit code " + result);
            } catch (RemoteException exception) {
                synchronized (LOCK) {
                    ShizukuForceStopper.service = null;
                }
                notifyResult(callback, false, "Shizuku service call failed");
            }
        });
    }

    private static void failPending(String message) {
        List<PendingRequest> requests;
        synchronized (LOCK) {
            binding = false;
            service = null;
            requests = new ArrayList<>(PENDING_REQUESTS);
            PENDING_REQUESTS.clear();
        }
        for (PendingRequest request : requests) {
            notifyResult(request.callback, false, message);
        }
    }

    private static void notifyResult(Callback callback, boolean success, String message) {
        if (callback == null) {
            return;
        }
        MAIN.post(() -> callback.onResult(success, message));
    }

    private static final class PendingRequest {
        final String packageName;
        final Callback callback;

        PendingRequest(String packageName, Callback callback) {
            this.packageName = packageName;
            this.callback = callback;
        }
    }
}
