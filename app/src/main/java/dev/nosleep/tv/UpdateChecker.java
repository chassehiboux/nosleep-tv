package dev.nosleep.tv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class UpdateChecker {
    private static final String LATEST_RELEASE_URL =
            "https://api.github.com/repos/chassehiboux/nosleep-tv/releases/latest";

    interface Callback {
        void onUpdateAvailable(ReleaseInfo releaseInfo);

        void onNoUpdate();

        void onNoRelease();

        void onError(Exception exception);
    }

    static final class ReleaseInfo {
        final String tagName;
        final String name;
        final String body;
        final String htmlUrl;
        final String apkUrl;

        ReleaseInfo(String tagName, String name, String body, String htmlUrl, String apkUrl) {
            this.tagName = tagName;
            this.name = name;
            this.body = body;
            this.htmlUrl = htmlUrl;
            this.apkUrl = apkUrl;
        }
    }

    private UpdateChecker() {
    }

    static void check(Context context, Callback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                ReleaseInfo releaseInfo = fetchLatestRelease();
                boolean newer = compareVersions(releaseInfo.tagName, BuildConfig.VERSION_NAME) > 0;
                mainHandler.post(() -> {
                    if (newer) {
                        callback.onUpdateAvailable(releaseInfo);
                    } else {
                        callback.onNoUpdate();
                    }
                });
            } catch (NoReleaseException exception) {
                mainHandler.post(callback::onNoRelease);
            } catch (Exception exception) {
                mainHandler.post(() -> callback.onError(exception));
            }
        }, "NoSleep-update-check").start();
    }

    private static ReleaseInfo fetchLatestRelease() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE_URL).openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("User-Agent", "NoSleep-TV/" + BuildConfig.VERSION_NAME);

        int status = connection.getResponseCode();
        if (status == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new NoReleaseException();
        }
        if (status < 200 || status >= 300) {
            throw new IOException("GitHub returned HTTP " + status);
        }

        String json = readFully(connection.getInputStream());
        JSONObject root = new JSONObject(json);
        String tagName = root.optString("tag_name", "");
        String name = root.optString("name", tagName);
        String body = root.optString("body", "");
        String htmlUrl = root.optString("html_url", "");
        String apkUrl = "";

        JSONArray assets = root.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String assetName = asset.optString("name", "");
                String downloadUrl = asset.optString("browser_download_url", "");
                if (assetName.toLowerCase().endsWith(".apk") && !downloadUrl.isEmpty()) {
                    apkUrl = downloadUrl;
                    break;
                }
            }
        }

        return new ReleaseInfo(tagName, name, body, htmlUrl, apkUrl);
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

    private static int compareVersions(String latest, String current) {
        int[] latestParts = parseVersion(latest);
        int[] currentParts = parseVersion(current);
        for (int i = 0; i < Math.max(latestParts.length, currentParts.length); i++) {
            int latestPart = i < latestParts.length ? latestParts[i] : 0;
            int currentPart = i < currentParts.length ? currentParts[i] : 0;
            if (latestPart != currentPart) {
                return latestPart - currentPart;
            }
        }
        return normalizeVersion(latest).equals(normalizeVersion(current)) ? 0 : 1;
    }

    private static int[] parseVersion(String value) {
        String normalized = normalizeVersion(value);
        String[] chunks = normalized.split("[^0-9]+");
        int[] parts = new int[Math.min(chunks.length, 4)];
        for (int i = 0; i < parts.length; i++) {
            try {
                parts[i] = chunks[i].isEmpty() ? 0 : Integer.parseInt(chunks[i]);
            } catch (NumberFormatException ignored) {
                parts[i] = 0;
            }
        }
        return parts;
    }

    private static String normalizeVersion(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase();
        while (normalized.startsWith("v")) {
            normalized = normalized.substring(1);
        }
        int dash = normalized.indexOf('-');
        return dash >= 0 ? normalized.substring(0, dash) : normalized;
    }

    private static final class NoReleaseException extends Exception {
    }
}
