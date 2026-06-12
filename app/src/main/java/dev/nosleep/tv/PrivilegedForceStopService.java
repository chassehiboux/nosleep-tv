package dev.nosleep.tv;

import android.os.RemoteException;
import android.util.Log;

import java.io.InputStream;
import java.util.regex.Pattern;

public class PrivilegedForceStopService extends INoSleepPrivilegedService.Stub {
    private static final String TAG = "NoSleepPrivileged";
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+");
    private static final int RESULT_OK = 0;
    private static final int RESULT_INVALID_PACKAGE = 64;
    private static final int RESULT_EXCEPTION = 70;

    public PrivilegedForceStopService() {
    }

    @Override
    public void destroy() {
        System.exit(0);
    }

    @Override
    public int forceStopPackage(String packageName) throws RemoteException {
        if (packageName == null || !PACKAGE_PATTERN.matcher(packageName).matches()) {
            return RESULT_INVALID_PACKAGE;
        }

        try {
            java.lang.Process process = new ProcessBuilder(
                    "/system/bin/cmd",
                    "activity",
                    "force-stop",
                    packageName)
                    .redirectErrorStream(true)
                    .start();
            drainOutput(process.getInputStream());
            return process.waitFor();
        } catch (Exception exception) {
            Log.w(TAG, "force-stop failed for " + packageName, exception);
            return RESULT_EXCEPTION;
        }
    }

    private void drainOutput(InputStream inputStream) {
        byte[] buffer = new byte[1024];
        try (InputStream input = inputStream) {
            while (input.read(buffer) != -1) {
                // Drain command output so the process cannot block on a full pipe.
            }
        } catch (Exception ignored) {
        }
    }
}
