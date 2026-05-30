package dev.nosleep.tv;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
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
import android.widget.FrameLayout;
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

    private LinearLayout setupScreen;
    private LinearLayout mainScreen;
    private TextView statusBadge;
    private TextView accessibilityStatus;
    private TextView overlayStatus;
    private TextView accessibilityButton;
    private TextView overlayButton;
    private TextView selectedCount;
    private TextView updateStatus;
    private TextView installUpdateButton;
    private TextView releasePageButton;
    private GridView appsGrid;
    private AppGridAdapter appAdapter;
    private UpdateChecker.ReleaseInfo availableRelease;
    private boolean updateCheckRunning;
    private boolean setupReady;

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
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BACKGROUND);
        setContentView(root);

        mainScreen = new LinearLayout(this);
        mainScreen.setOrientation(LinearLayout.VERTICAL);
        mainScreen.setPadding(dp(40), dp(24), dp(40), dp(28));
        root.addView(mainScreen, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        buildMainScreen(mainScreen);

        setupScreen = new LinearLayout(this);
        setupScreen.setGravity(Gravity.CENTER);
        setupScreen.setPadding(dp(48), dp(36), dp(48), dp(36));
        root.addView(setupScreen, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        buildSetupScreen(setupScreen);
    }

    private void buildMainScreen(LinearLayout root) {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.nosleep_icon_source);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        header.addView(logo, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setPadding(dp(16), 0, dp(12), 0);
        header.addView(titleBlock, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = text(getString(R.string.app_name), 26, TEXT, Typeface.BOLD);
        title.setSingleLine(true);
        titleBlock.addView(title);

        TextView subtitle = text(getString(R.string.tagline), 14, MUTED, Typeface.NORMAL);
        subtitle.setMaxLines(2);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        subtitle.setLineSpacing(dp(1), 1f);
        subtitle.setPadding(0, dp(3), 0, 0);
        titleBlock.addView(subtitle);

        statusBadge = badge("");
        header.addView(statusBadge, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Space headerGap = new Space(this);
        header.addView(headerGap, new LinearLayout.LayoutParams(dp(12), 1));

        TextView checkButton = actionButton(getString(R.string.check_updates), false);
        checkButton.setOnClickListener(v -> checkForUpdates(true));
        header.addView(checkButton, new LinearLayout.LayoutParams(dp(190), dp(48)));

        LinearLayout updates = new LinearLayout(this);
        updates.setGravity(Gravity.CENTER_VERTICAL);
        updates.setOrientation(LinearLayout.HORIZONTAL);
        updates.setPadding(dp(16), dp(10), dp(16), dp(10));
        setRoundedBackground(updates, PANEL, Color.TRANSPARENT, dp(8), 0);
        LinearLayout.LayoutParams updatesParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        updatesParams.setMargins(0, dp(16), 0, dp(16));
        root.addView(updates, updatesParams);
        buildUpdatesBar(updates);

        LinearLayout appsPanel = panel();
        root.addView(appsPanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        buildAppsPanel(appsPanel);
    }

    private void buildSetupScreen(LinearLayout root) {
        LinearLayout card = panel();
        card.setPadding(dp(30), dp(28), dp(30), dp(28));
        root.addView(card, new LinearLayout.LayoutParams(dp(720),
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(titleRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.nosleep_icon_source);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        titleRow.addView(logo, new LinearLayout.LayoutParams(dp(64), dp(64)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(18), 0, 0, 0);
        titleRow.addView(copy, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = text(getString(R.string.setup_title), 25, TEXT, Typeface.BOLD);
        copy.addView(title);

        TextView body = text(getString(R.string.setup_body), 15, MUTED, Typeface.NORMAL);
        body.setMaxLines(3);
        body.setLineSpacing(dp(2), 1f);
        body.setPadding(0, dp(6), 0, 0);
        copy.addView(body);

        accessibilityStatus = text("", 14, TEXT, Typeface.BOLD);
        accessibilityStatus.setPadding(0, dp(24), 0, dp(8));
        card.addView(accessibilityStatus);

        accessibilityButton = actionButton(getString(R.string.open_accessibility), true);
        accessibilityButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        card.addView(accessibilityButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        overlayStatus = text("", 14, TEXT, Typeface.BOLD);
        overlayStatus.setPadding(0, dp(18), 0, dp(8));
        card.addView(overlayStatus);

        overlayButton = actionButton(getString(R.string.open_overlay), false);
        overlayButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        card.addView(overlayButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
    }

    private void buildUpdatesBar(LinearLayout bar) {
        updateStatus = text(getString(R.string.checking_updates), 13, MUTED, Typeface.NORMAL);
        updateStatus.setSingleLine(true);
        updateStatus.setEllipsize(TextUtils.TruncateAt.END);
        bar.addView(updateStatus, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        installUpdateButton = actionButton(getString(R.string.install_update), true);
        installUpdateButton.setVisibility(View.GONE);
        installUpdateButton.setOnClickListener(v -> installAvailableUpdate());
        bar.addView(installUpdateButton, new LinearLayout.LayoutParams(dp(190), dp(46)));

        Space gap = new Space(this);
        bar.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));

        releasePageButton = actionButton(getString(R.string.manual_release), false);
        releasePageButton.setVisibility(View.GONE);
        releasePageButton.setOnClickListener(v -> openReleasePage());
        bar.addView(releasePageButton, new LinearLayout.LayoutParams(dp(180), dp(46)));
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

        TextView title = text(getString(R.string.apps_title), 23, TEXT, Typeface.BOLD);
        copy.addView(title);

        TextView subtitle = text(getString(R.string.apps_subtitle), 14, MUTED, Typeface.NORMAL);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        subtitle.setPadding(0, dp(4), 0, 0);
        copy.addView(subtitle);

        selectedCount = badge("");
        titleRow.addView(selectedCount, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        appsGrid = new GridView(this);
        appsGrid.setNumColumns(4);
        appsGrid.setHorizontalSpacing(dp(12));
        appsGrid.setVerticalSpacing(dp(12));
        appsGrid.setPadding(0, dp(18), 0, dp(2));
        appsGrid.setClipToPadding(false);
        appsGrid.setSelector(android.R.color.transparent);
        appsGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        appsGrid.setGravity(Gravity.TOP);
        appsGrid.setFocusable(true);
        appsGrid.setFocusableInTouchMode(false);
        appsGrid.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        appsGrid.setOnItemClickListener(this::onAppClicked);
        appsGrid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (appAdapter != null) {
                    appAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (appAdapter != null) {
                    appAdapter.notifyDataSetChanged();
                }
            }
        });
        appsGrid.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && appsGrid.getSelectedItemPosition() == AdapterView.INVALID_POSITION
                    && appAdapter != null && appAdapter.getCount() > 0) {
                appsGrid.setSelection(0);
            }
            if (appAdapter != null) {
                appAdapter.notifyDataSetChanged();
            }
        });
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
        if (!entries.isEmpty()) {
            appsGrid.setSelection(0);
        }
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
            ApplicationInfo appInfo = resolveInfo.activityInfo.applicationInfo;
            if (appInfo == null || isSystemApp(appInfo)) {
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

    private boolean isSystemApp(ApplicationInfo appInfo) {
        int systemFlags = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        return (appInfo.flags & systemFlags) != 0;
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
        setupReady = accessibilityEnabled && overlayEnabled;

        setupScreen.setVisibility(setupReady ? View.GONE : View.VISIBLE);
        mainScreen.setVisibility(setupReady ? View.VISIBLE : View.GONE);

        statusBadge.setText(getString(setupReady ? R.string.status_ready : R.string.status_setup_needed));
        setBadgeColor(statusBadge, setupReady ? TEAL : AMBER);

        accessibilityStatus.setText(getString(accessibilityEnabled
                ? R.string.accessibility_granted
                : R.string.accessibility_missing));
        accessibilityStatus.setTextColor(accessibilityEnabled ? TEAL : AMBER);
        accessibilityButton.setVisibility(accessibilityEnabled ? View.GONE : View.VISIBLE);

        overlayStatus.setText(getString(overlayEnabled
                ? R.string.overlay_granted
                : R.string.overlay_missing));
        overlayStatus.setTextColor(overlayEnabled ? TEAL : AMBER);
        overlayButton.setVisibility(overlayEnabled ? View.GONE : View.VISIBLE);

        if (setupReady) {
            focusAppsGridIfNeeded();
        } else {
            focusSetupAction(accessibilityEnabled, overlayEnabled);
        }
    }

    private void focusSetupAction(boolean accessibilityEnabled, boolean overlayEnabled) {
        View target = !accessibilityEnabled ? accessibilityButton : (!overlayEnabled ? overlayButton : null);
        if (target != null) {
            target.post(target::requestFocus);
        }
    }

    private void focusAppsGridIfNeeded() {
        View currentFocus = getCurrentFocus();
        if (currentFocus == null || isDescendantOf(setupScreen, currentFocus)) {
            appsGrid.post(() -> {
                if (appAdapter != null && appAdapter.getCount() > 0
                        && appsGrid.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
                    appsGrid.setSelection(0);
                }
                appsGrid.requestFocus();
            });
        }
    }

    private boolean isDescendantOf(ViewGroup parent, View child) {
        if (child == null) {
            return false;
        }
        View current = child;
        while (current != null) {
            if (current == parent) {
                return true;
            }
            if (!(current.getParent() instanceof View)) {
                return false;
            }
            current = (View) current.getParent();
        }
        return false;
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
        setBadgeColor(selectedCount, count > 0 ? TEAL : AMBER);
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
                if (releaseInfo.apkUrl.isEmpty()) {
                    message.append(" · ").append(getString(R.string.apk_asset_missing));
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
        panel.setPadding(dp(20), dp(18), dp(20), dp(18));
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

    private TextView badge(String value) {
        TextView badge = text(value, 13, BACKGROUND, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(16), dp(8), dp(16), dp(8));
        setBadgeColor(badge, AMBER);
        return badge;
    }

    private void setBadgeColor(TextView badge, int color) {
        badge.setTextColor(BACKGROUND);
        setRoundedBackground(badge, color, color, dp(999), 0);
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
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(116)));
            }
            boolean highlighted = appsGrid.hasFocus()
                    && position == appsGrid.getSelectedItemPosition();
            tile.bind(entries.get(position), highlighted);
            return tile;
        }
    }

    private final class AppTileView extends LinearLayout {
        private final ImageView icon;
        private final TextView label;
        private final TextView packageName;
        private AppEntry entry;
        private boolean highlighted;

        AppTileView(android.content.Context context) {
            super(context);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(dp(14), dp(14), dp(14), dp(14));
            setFocusable(false);
            setClickable(false);

            icon = new ImageView(context);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));

            LinearLayout copy = new LinearLayout(context);
            copy.setOrientation(VERTICAL);
            copy.setPadding(dp(12), 0, 0, 0);
            addView(copy, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            label = text("", 15, TEXT, Typeface.BOLD);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            copy.addView(label);

            packageName = text("", 11, MUTED, Typeface.NORMAL);
            packageName.setSingleLine(true);
            packageName.setEllipsize(TextUtils.TruncateAt.END);
            packageName.setPadding(0, dp(4), 0, 0);
            copy.addView(packageName);
        }

        void bind(AppEntry entry, boolean highlighted) {
            this.entry = entry;
            this.highlighted = highlighted;
            icon.setImageDrawable(entry.icon);
            label.setText(entry.label);
            packageName.setText(entry.packageName);
            paintTile();
        }

        private void paintTile() {
            if (entry == null) {
                return;
            }
            int fill = entry.selected ? Color.rgb(14, 47, 45) : PANEL_ALT;
            int stroke = entry.selected ? TEAL : Color.rgb(43, 59, 66);
            int strokeWidth = entry.selected ? dp(2) : dp(1);
            if (highlighted) {
                fill = entry.selected ? Color.rgb(24, 70, 65) : Color.rgb(35, 51, 59);
                stroke = AMBER;
                strokeWidth = dp(2);
            }
            setRoundedBackground(this, fill, stroke, dp(8), strokeWidth);
        }
    }
}
