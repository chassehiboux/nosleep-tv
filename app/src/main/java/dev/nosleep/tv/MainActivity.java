package dev.nosleep.tv;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(7, 16, 19);
    private static final int PANEL = Color.rgb(17, 26, 32);
    private static final int PANEL_ALT = Color.rgb(23, 36, 43);
    private static final int TEXT = Color.rgb(244, 250, 248);
    private static final int MUTED = Color.rgb(168, 182, 184);
    private static final int TEAL = Color.rgb(45, 224, 194);
    private static final int AMBER = Color.rgb(255, 196, 77);
    private static final int DANGER = Color.rgb(255, 107, 107);

    private LinearLayout setupPanel;
    private TextView statusBadge;
    private TextView accessibilityStatus;
    private TextView overlayStatus;
    private TextView selectedCount;
    private TextView updateStatus;
    private TextView installUpdateButton;
    private TextView releasePageButton;
    private GridView appsGrid;
    private AppGridAdapter appAdapter;
    private UpdateChecker.ReleaseInfo availableRelease;
    private boolean updateCheckRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        buildUi();
        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRequirementState();
        refreshSelectionCount();
        checkForUpdates(false);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(48), dp(36), dp(48), dp(36));
        root.setBackgroundColor(BACKGROUND);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(88)));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.nosleep_icon_source);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        header.addView(logo, new LinearLayout.LayoutParams(dp(64), dp(64)));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setPadding(dp(18), 0, 0, 0);
        header.addView(titleBlock, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = text(getString(R.string.app_full_name), 28, TEXT, Typeface.BOLD);
        titleBlock.addView(title);

        TextView subtitle = text(getString(R.string.tagline), 15, MUTED, Typeface.NORMAL);
        subtitle.setPadding(0, dp(6), 0, 0);
        titleBlock.addView(subtitle);

        statusBadge = text("", 14, BACKGROUND, Typeface.BOLD);
        statusBadge.setGravity(Gravity.CENTER);
        statusBadge.setPadding(dp(18), dp(10), dp(18), dp(10));
        header.addView(statusBadge, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Space headerGap = new Space(this);
        header.addView(headerGap, new LinearLayout.LayoutParams(dp(16), 1));

        TextView checkButton = actionButton(getString(R.string.check_updates), false);
        checkButton.setOnClickListener(v -> checkForUpdates(true));
        header.addView(checkButton, new LinearLayout.LayoutParams(dp(210), dp(52)));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.HORIZONTAL);
        main.setGravity(Gravity.TOP);
        root.addView(main, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        main.addView(side, new LinearLayout.LayoutParams(dp(390),
                ViewGroup.LayoutParams.MATCH_PARENT));

        setupPanel = panel();
        side.addView(setupPanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buildSetupPanel(setupPanel);

        Space sideGap = new Space(this);
        side.addView(sideGap, new LinearLayout.LayoutParams(1, dp(18)));

        LinearLayout updates = panel();
        side.addView(updates, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buildUpdatesPanel(updates);

        Space columnGap = new Space(this);
        main.addView(columnGap, new LinearLayout.LayoutParams(dp(26), 1));

        LinearLayout appsPanel = panel();
        main.addView(appsPanel, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        buildAppsPanel(appsPanel);
    }

    private void buildSetupPanel(LinearLayout panel) {
        TextView title = text(getString(R.string.setup_title), 21, TEXT, Typeface.BOLD);
        panel.addView(title);

        TextView body = text(getString(R.string.setup_body), 14, MUTED, Typeface.NORMAL);
        body.setPadding(0, dp(10), 0, dp(18));
        body.setLineSpacing(dp(2), 1f);
        panel.addView(body);

        accessibilityStatus = text("", 14, TEXT, Typeface.BOLD);
        accessibilityStatus.setPadding(0, 0, 0, dp(8));
        panel.addView(accessibilityStatus);

        TextView accessibilityButton = actionButton(getString(R.string.open_accessibility), true);
        accessibilityButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        panel.addView(accessibilityButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));

        overlayStatus = text("", 14, TEXT, Typeface.BOLD);
        overlayStatus.setPadding(0, dp(18), 0, dp(8));
        panel.addView(overlayStatus);

        TextView overlayButton = actionButton(getString(R.string.open_overlay), false);
        overlayButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        panel.addView(overlayButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
    }

    private void buildUpdatesPanel(LinearLayout panel) {
        TextView title = text(getString(R.string.updates_title), 21, TEXT, Typeface.BOLD);
        panel.addView(title);

        updateStatus = text(getString(R.string.checking_updates), 14, MUTED, Typeface.NORMAL);
        updateStatus.setPadding(0, dp(10), 0, dp(18));
        updateStatus.setLineSpacing(dp(2), 1f);
        panel.addView(updateStatus);

        installUpdateButton = actionButton(getString(R.string.install_update), true);
        installUpdateButton.setVisibility(View.GONE);
        installUpdateButton.setOnClickListener(v -> installAvailableUpdate());
        panel.addView(installUpdateButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));

        Space gap = new Space(this);
        panel.addView(gap, new LinearLayout.LayoutParams(1, dp(10)));

        releasePageButton = actionButton(getString(R.string.manual_release), false);
        releasePageButton.setVisibility(View.GONE);
        releasePageButton.setOnClickListener(v -> openReleasePage());
        panel.addView(releasePageButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
    }

    private void buildAppsPanel(LinearLayout panel) {
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        panel.addView(titleRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        titleRow.addView(copy, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = text(getString(R.string.apps_title), 24, TEXT, Typeface.BOLD);
        copy.addView(title);

        TextView subtitle = text(getString(R.string.apps_subtitle), 14, MUTED, Typeface.NORMAL);
        subtitle.setPadding(0, dp(6), 0, 0);
        copy.addView(subtitle);

        selectedCount = text("", 14, BACKGROUND, Typeface.BOLD);
        selectedCount.setGravity(Gravity.CENTER);
        selectedCount.setPadding(dp(18), dp(10), dp(18), dp(10));
        setRoundedBackground(selectedCount, AMBER, AMBER, dp(999), 0);
        titleRow.addView(selectedCount, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        appsGrid = new GridView(this);
        appsGrid.setNumColumns(3);
        appsGrid.setHorizontalSpacing(dp(14));
        appsGrid.setVerticalSpacing(dp(14));
        appsGrid.setPadding(0, dp(22), 0, 0);
        appsGrid.setClipToPadding(false);
        appsGrid.setSelector(android.R.color.transparent);
        appsGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        appsGrid.setGravity(Gravity.TOP);
        appsGrid.setOnItemClickListener(this::onAppClicked);
        panel.addView(appsGrid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    private void loadApps() {
        PackageManager packageManager = getPackageManager();
        Set<String> selectedPackages = Prefs.getSelectedPackages(this);
        Map<String, AppEntry> apps = new LinkedHashMap<>();
        collectApps(packageManager, Intent.CATEGORY_LEANBACK_LAUNCHER, selectedPackages, apps);
        collectApps(packageManager, Intent.CATEGORY_LAUNCHER, selectedPackages, apps);

        List<AppEntry> entries = new ArrayList<>(apps.values());
        entries.sort(Comparator.comparing(app -> app.label.toLowerCase(Locale.getDefault())));
        appAdapter = new AppGridAdapter(entries);
        appsGrid.setAdapter(appAdapter);
        refreshSelectionCount();
    }

    private void collectApps(PackageManager packageManager,
                             String category,
                             Set<String> selectedPackages,
                             Map<String, AppEntry> out) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(category);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo == null) {
                continue;
            }
            String packageName = resolveInfo.activityInfo.packageName;
            if (getPackageName().equals(packageName) || out.containsKey(packageName)) {
                continue;
            }
            CharSequence label = resolveInfo.loadLabel(packageManager);
            out.put(packageName, new AppEntry(
                    label == null ? packageName : label.toString(),
                    packageName,
                    resolveInfo.loadIcon(packageManager),
                    selectedPackages.contains(packageName)));
        }
    }

    private void onAppClicked(AdapterView<?> parent, View view, int position, long id) {
        AppEntry entry = (AppEntry) appAdapter.getItem(position);
        entry.selected = !entry.selected;
        Prefs.setPackageSelected(this, entry.packageName, entry.selected);
        appAdapter.notifyDataSetChanged();
        refreshSelectionCount();
    }

    private void refreshRequirementState() {
        boolean accessibilityEnabled = isAccessibilityServiceEnabled();
        boolean overlayEnabled = WakeKeeper.hasOverlayPermission(this);
        boolean ready = accessibilityEnabled && overlayEnabled;

        setupPanel.setVisibility(ready ? View.GONE : View.VISIBLE);
        statusBadge.setText(getString(ready ? R.string.status_ready : R.string.status_setup_needed));
        setRoundedBackground(statusBadge, ready ? TEAL : AMBER, ready ? TEAL : AMBER, dp(999), 0);

        accessibilityStatus.setText(getString(accessibilityEnabled
                ? R.string.accessibility_granted
                : R.string.accessibility_missing));
        accessibilityStatus.setTextColor(accessibilityEnabled ? TEAL : AMBER);

        overlayStatus.setText(getString(overlayEnabled
                ? R.string.overlay_granted
                : R.string.overlay_missing));
        overlayStatus.setTextColor(overlayEnabled ? TEAL : AMBER);
    }

    private boolean isAccessibilityServiceEnabled() {
        ComponentName expected = new ComponentName(this, AppMonitorAccessibilityService.class);
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices == null) {
            return false;
        }

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            ComponentName enabled = ComponentName.unflattenFromString(splitter.next());
            if (expected.equals(enabled)) {
                return true;
            }
        }
        return false;
    }

    private void refreshSelectionCount() {
        int count = 0;
        if (appAdapter != null) {
            for (AppEntry entry : appAdapter.entries) {
                if (entry.selected) {
                    count++;
                }
            }
        }
        selectedCount.setText(getString(R.string.selected_count, count));
    }

    private void checkForUpdates(boolean userInitiated) {
        if (updateCheckRunning) {
            return;
        }
        updateCheckRunning = true;
        availableRelease = null;
        installUpdateButton.setVisibility(View.GONE);
        releasePageButton.setVisibility(View.GONE);
        updateStatus.setText(getString(R.string.checking_updates));
        updateStatus.setTextColor(MUTED);

        UpdateChecker.check(this, new UpdateChecker.Callback() {
            @Override
            public void onUpdateAvailable(UpdateChecker.ReleaseInfo releaseInfo) {
                updateCheckRunning = false;
                availableRelease = releaseInfo;
                StringBuilder message = new StringBuilder(
                        getString(R.string.update_available, releaseInfo.tagName));
                if (!releaseInfo.body.trim().isEmpty()) {
                    message.append("\n").append(releaseInfo.body.trim());
                }
                if (releaseInfo.apkUrl.isEmpty()) {
                    message.append("\n").append(getString(R.string.apk_asset_missing));
                }
                updateStatus.setText(message.toString());
                updateStatus.setTextColor(TEXT);
                installUpdateButton.setVisibility(releaseInfo.apkUrl.isEmpty() ? View.GONE : View.VISIBLE);
                releasePageButton.setVisibility(releaseInfo.htmlUrl.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onNoUpdate() {
                updateCheckRunning = false;
                updateStatus.setText(getString(R.string.no_updates));
                updateStatus.setTextColor(TEAL);
            }

            @Override
            public void onNoRelease() {
                updateCheckRunning = false;
                updateStatus.setText(getString(R.string.no_releases));
                updateStatus.setTextColor(MUTED);
            }

            @Override
            public void onError(Exception exception) {
                updateCheckRunning = false;
                updateStatus.setText(getString(R.string.update_check_failed));
                updateStatus.setTextColor(userInitiated ? DANGER : MUTED);
            }
        });
    }

    private void installAvailableUpdate() {
        if (availableRelease == null || availableRelease.apkUrl.isEmpty()) {
            return;
        }
        if (!UpdateInstaller.canRequestPackageInstalls(this)) {
            updateStatus.setText(getString(R.string.install_permission_needed));
            updateStatus.setTextColor(AMBER);
            UpdateInstaller.openInstallPermission(this);
            return;
        }

        installUpdateButton.setVisibility(View.GONE);
        releasePageButton.setVisibility(View.GONE);
        updateStatus.setText(getString(R.string.downloading_update));
        updateStatus.setTextColor(MUTED);
        UpdateInstaller.downloadAndInstall(this, availableRelease, new UpdateInstaller.Callback() {
            @Override
            public void onProgress(int progress) {
                updateStatus.setText(getString(R.string.download_progress, progress));
            }

            @Override
            public void onReadyToInstall() {
                updateStatus.setText(getString(R.string.install_update));
                updateStatus.setTextColor(TEAL);
            }

            @Override
            public void onError(Exception exception) {
                updateStatus.setText(getString(R.string.download_failed));
                updateStatus.setTextColor(DANGER);
                installUpdateButton.setVisibility(View.VISIBLE);
                releasePageButton.setVisibility(availableRelease.htmlUrl.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void openReleasePage() {
        if (availableRelease == null || availableRelease.htmlUrl.isEmpty()) {
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(availableRelease.htmlUrl)));
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(20), dp(22), dp(20));
        setRoundedBackground(panel, PANEL, Color.TRANSPARENT, dp(8), 0);
        return panel;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextColor(color);
        textView.setTextSize(sp);
        textView.setTypeface(Typeface.DEFAULT, style);
        textView.setIncludeFontPadding(true);
        return textView;
    }

    private TextView actionButton(String value, boolean primary) {
        TextView button = text(value, 14, primary ? BACKGROUND : TEXT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setFocusable(true);
        button.setClickable(true);
        button.setPadding(dp(14), 0, dp(14), 0);
        paintActionButton(button, primary, false);
        button.setOnFocusChangeListener((v, hasFocus) -> paintActionButton(button, primary, hasFocus));
        return button;
    }

    private void paintActionButton(TextView button, boolean primary, boolean focused) {
        int fill = primary ? TEAL : PANEL_ALT;
        int stroke = focused ? AMBER : (primary ? TEAL : Color.rgb(49, 68, 76));
        int textColor = primary ? BACKGROUND : TEXT;
        if (focused) {
            fill = primary ? AMBER : Color.rgb(33, 50, 58);
            textColor = BACKGROUND;
        }
        button.setTextColor(textColor);
        setRoundedBackground(button, fill, stroke, dp(8), focused ? dp(2) : dp(1));
    }

    private void setRoundedBackground(View view, int fill, int stroke, int radius, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, stroke);
        }
        view.setBackground(drawable);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class AppGridAdapter extends BaseAdapter {
        private final List<AppEntry> entries;

        AppGridAdapter(List<AppEntry> entries) {
            this.entries = entries;
        }

        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Object getItem(int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppTileView tile;
            if (convertView instanceof AppTileView) {
                tile = (AppTileView) convertView;
            } else {
                tile = new AppTileView(parent.getContext());
                tile.setLayoutParams(new GridView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(132)));
            }
            tile.bind(entries.get(position));
            return tile;
        }
    }

    private final class AppTileView extends LinearLayout {
        private final ImageView icon;
        private final TextView label;
        private final TextView packageName;
        private AppEntry entry;

        AppTileView(android.content.Context context) {
            super(context);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(dp(16), dp(16), dp(16), dp(16));
            setFocusable(true);
            setClickable(true);

            icon = new ImageView(context);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            addView(icon, new LinearLayout.LayoutParams(dp(58), dp(58)));

            LinearLayout copy = new LinearLayout(context);
            copy.setOrientation(VERTICAL);
            copy.setPadding(dp(14), 0, 0, 0);
            addView(copy, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            label = text("", 16, TEXT, Typeface.BOLD);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            copy.addView(label);

            packageName = text("", 12, MUTED, Typeface.NORMAL);
            packageName.setSingleLine(true);
            packageName.setEllipsize(TextUtils.TruncateAt.END);
            packageName.setPadding(0, dp(6), 0, 0);
            copy.addView(packageName);

            setOnFocusChangeListener((v, hasFocus) -> paintTile(hasFocus));
        }

        void bind(AppEntry entry) {
            this.entry = entry;
            icon.setImageDrawable(entry.icon);
            label.setText(entry.label);
            packageName.setText(entry.packageName);
            paintTile(isFocused());
        }

        private void paintTile(boolean focused) {
            if (entry == null) {
                return;
            }
            int fill = entry.selected ? Color.rgb(14, 47, 45) : PANEL_ALT;
            int stroke = entry.selected ? TEAL : Color.rgb(43, 59, 66);
            int strokeWidth = entry.selected ? dp(2) : dp(1);
            if (focused) {
                fill = entry.selected ? Color.rgb(24, 70, 65) : Color.rgb(35, 51, 59);
                stroke = AMBER;
                strokeWidth = dp(2);
            }
            setRoundedBackground(this, fill, stroke, dp(8), strokeWidth);
        }
    }
}
