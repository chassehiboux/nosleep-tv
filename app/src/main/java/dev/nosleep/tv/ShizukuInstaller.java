package dev.nosleep.tv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class ShizukuInstaller {
    static final String PACKAGE_NAME = "moe.shizuku.privileged.api";
    private static final String LATEST_RELEASE_URL =
            "https://api.github.com/repos/RikkaApps/Shizuku/releases/latest";
    private static final String RELEASES_URL =
            "https://github.com/RikkaApps/Shizuku/releases/latest";

    interface Callback {
        void onProgress(int progress);

        void onReadyToInstall();

        void onError(Exception exception);
    }

    private ShizukuInstaller() {
    }

    static boolean isInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
            return true;
        } catch (PackageManager.NameNotFoundException exception) {
            return false;
        }
    }

    static boolean openInstalled(Activity activity) {
        Intent intent = activity.getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME);
        if (intent == null) {
            return false;
        }
        activity.startActivity(intent);
        return true;
    }

    static void openReleasePage(Activity activity) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)));
    }

    static void downloadAndInstall(Activity activity, Callback callback) {
        new Thread(() -> {
            try {
                ReleaseAsset asset = fetchLatestApkAsset();
                File apk = downloadApk(activity, asset, callback);
                activity.runOnUiThread(() -> {
                    callback.onReadyToInstall();
                    installApk(activity, apk);
                });
            } catch (Exception exception) {
                activity.runOnUiThread(() -> callback.onError(exception));
            }
        }, "NoSleep-shizuku-download").start();
    }

    private static ReleaseAsset fetchLatestApkAsset() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE_URL).openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("User-Agent", "NoSleep-TV/" + BuildConfig.VERSION_NAME);

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IOException("GitHub returned HTTP " + status);
        }

        JSONObject root = new JSONObject(readFully(connection.getInputStream()));
        String tagName = root.optString("tag_name", "latest");
        JSONArray assets = root.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String name = asset.optString("name", "");
                String url = asset.optString("browser_download_url", "");
                String lowerName = name.toLowerCase(Locale.ROOT);
                if (lowerName.endsWith(".apk") && !url.isEmpty()) {
                    return new ReleaseAsset(tagName, url);
                }
            }
        }
        throw new IOException("No Shizuku APK asset found");
    }

    private static File downloadApk(Activity activity,
                                    ReleaseAsset asset,
                                    Callback callback) throws Exception {
        File dir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) {
            dir = new File(activity.getCacheDir(), "downloads");
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Could not create download directory");
        }

        String safeTag = asset.tagName.replaceAll("[^A-Za-z0-9._-]", "_");
        File apk = new File(dir, "Shizuku-" + safeTag + ".apk");

        HttpURLConnection connection = (HttpURLConnection) new URL(asset.downloadUrl).openConnection();
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
            openReleasePage(activity);
        }
    }

    private static String readFully(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static final class ReleaseAsset {
        final String tagName;
        final String downloadUrl;

        ReleaseAsset(String tagName, String downloadUrl) {
            this.tagName = tagName;
            this.downloadUrl = downloadUrl;
        }
    }
}
