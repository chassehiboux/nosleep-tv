package dev.nosleep.tv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class UpdateInstaller {
    interface Callback {
        void onProgress(int progress);

        void onReadyToInstall();

        void onError(Exception exception);
    }

    private UpdateInstaller() {
    }

    static boolean canRequestPackageInstalls(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || context.getPackageManager().canRequestPackageInstalls();
    }

    static void openInstallPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        }
    }

    static void downloadAndInstall(Activity activity,
                                   UpdateChecker.ReleaseInfo releaseInfo,
                                   Callback callback) {
        new Thread(() -> {
            try {
                File apk = downloadApk(activity, releaseInfo, callback);
                activity.runOnUiThread(() -> {
                    callback.onReadyToInstall();
                    installApk(activity, apk);
                });
            } catch (Exception exception) {
                activity.runOnUiThread(() -> callback.onError(exception));
            }
        }, "NoSleep-update-download").start();
    }

    private static File downloadApk(Activity activity,
                                    UpdateChecker.ReleaseInfo releaseInfo,
                                    Callback callback) throws Exception {
        File dir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) {
            dir = new File(activity.getCacheDir(), "updates");
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Could not create update directory");
        }

        String safeTag = releaseInfo.tagName.replaceAll("[^A-Za-z0-9._-]", "_");
        File apk = new File(dir, "NoSleep-" + safeTag + ".apk");

        HttpURLConnection connection = (HttpURLConnection) new URL(releaseInfo.apkUrl).openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(30_000);
        connection.setRequestProperty("User-Agent", "NoSleep-TV/" + BuildConfig.VERSION_NAME);
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("Download returned HTTP " + status);
        }

        int contentLength = connection.getContentLength();
        try (InputStream input = connection.getInputStream();
             FileOutputStream output = new FileOutputStream(apk)) {
            byte[] buffer = new byte[16 * 1024];
            long total = 0;
            int read;
            int lastProgress = -1;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                total += read;
                if (contentLength > 0) {
                    int progress = (int) Math.min(100, (total * 100) / contentLength);
                    if (progress != lastProgress) {
                        lastProgress = progress;
                        activity.runOnUiThread(() -> callback.onProgress(progress));
                    }
                }
            }
        }
        return apk;
    }

    private static void installApk(Activity activity, File apk) {
        Uri uri = FileProvider.getUriForFile(
                activity,
                activity.getPackageName() + ".fileprovider",
                apk);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse(apk.getParentFile().toURI().toString()));
            activity.startActivity(fallback);
        }
    }
}
