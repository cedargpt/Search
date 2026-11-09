package org.nofeed.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private Appearance.Palette colors;
    private String appearanceMode;
    private int paper, ink, muted;
    private LocalStore store;
    private Typeface uiFont;
    private LinearLayout root, content, dots, header;
    private PagedAppGrid grid;
    private EditText query;
    private TextView count;
    private SearchTarget selected;
    private String screen = "home", draft = "";

    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(Appearance.wrap(base));
    }

    @Override public void onCreate(Bundle state) {
        Appearance.apply(this, false);
        super.onCreate(state);
        colors = Appearance.palette(this); paper = colors.paper; ink = colors.ink; muted = colors.muted;
        appearanceMode = Appearance.mode(this);
        // The UI follows the phone's own font instead of bundling a CJK serif.
        uiFont = Typeface.SANS_SERIF;
        store = new LocalStore(this);
        AppCatalog.migrate(this);
        // Removed preferences must not affect future launches after an upgrade.
        android.content.SharedPreferences.Editor cleanup = store.prefs.edit();
        for (String key : store.prefs.getAll().keySet())
            if (key.startsWith("alias_") || key.startsWith("route_")) cleanup.remove(key);
        cleanup.apply();
        SearchWidget.initialize(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (state != null) {
            draft = state.getString("draft", "");
            selected = target(state.getString("selected"));
        } else selected = target(store.prefs.getString("default_target", ""));
        if (state == null) consumeIntent(getIntent());
        String previous = state == null ? "home" : state.getString("screen", "home");
        if ("settings".equals(previous)) showSettings();
        else if ("历史记录".equals(previous)) showHistory();
        else showHome();
    }

    private SearchTarget target(String name) {
        try { return SearchTarget.valueOf(name); } catch (Exception ignored) { return null; }
    }
    private void consumeIntent(Intent intent) {
        if (intent.hasExtra("target")) selected = target(intent.getStringExtra("target"));
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null) draft = text.substring(0, Math.min(text.length(), 500));
        }
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); consumeIntent(intent); showHome(); }
    @Override protected void onResume() {
        super.onResume();
        boolean renderedDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        if (renderedDark != Appearance.isDark(this) || !appearanceMode.equals(Appearance.mode(this))) { recreate(); return; }
        SearchWidget.refresh();
        if ("settings".equals(screen)) showSettings();
        else if ("home".equals(screen) && grid != null) {
            grid.setTargets(HomeApps.visible(this), selected);
            // Coming back from an app or the settings page should not cost a tap.
            showKeyboard();
        }
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("draft", query != null ? query.getText().toString() : draft);
        out.putString("selected", selected == null ? "" : selected.name());
        out.putString("screen", screen);
    }
    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable background(int color, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(dp(radius));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }
    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(uiFont);
        view.setFontFeatureSettings("kern"); view.setIncludeFontPadding(false); return view;
    }
    private TextView action(String value, Runnable click) {
        TextView view = text(value, 15, ink); view.setGravity(Gravity.CENTER); view.setMinHeight(dp(48));
        view.setPadding(dp(12), dp(8), dp(12), dp(8)); view.setBackground(background(Color.TRANSPARENT, 14, 0));
        view.setOnClickListener(v -> click.run()); view.setFocusable(true); return view;
    }
    private void space(LinearLayout parent, int height) { View v = new View(this); parent.addView(v, new LinearLayout.LayoutParams(1, dp(height))); }
    private void line(LinearLayout parent) { View v = new View(this); v.setBackgroundColor(colors.divider); parent.addView(v, new LinearLayout.LayoutParams(-1, dp(1))); }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void base(String page) {
        screen = page;
        // Home is built around typing, so ask for the keyboard as soon as the
        // window takes focus; other pages must not inherit it.
        getWindow().setSoftInputMode("home".equals(page)
                ? WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                : WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        int pageColor = "settings".equals(page) ? colors.settings : paper;
        root = column(); root.setBackgroundColor(pageColor); root.setPadding(dp(26), 0, dp(26), 0);
        // API 35 draws edge-to-edge. Insets keep every control outside system bars and keyboard.
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            // "home".equals(screen) keeps the keyboard padding off the read-only pages.
            int bottom = Math.max(insets.getSystemWindowInsetBottom(), imeBottom(insets));
            root.setPadding(dp(26) + insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    dp(26) + insets.getSystemWindowInsetRight(), bottom);
            if ("home".equals(screen)) fitKeyboard(insets.getSystemWindowInsetTop(), bottom);
            return insets;
        });
        setContentView(root); Appearance.bars(this, pageColor); root.requestApplyInsets();
    }

    /**
     * A tall IME (candidate bar plus tool row) can leave less room than the grid
     * needs. The title row and the page dots give up their space first, so no
     * tile is ever pushed under the keyboard. The screen height is a stable
     * input, which keeps the decision out of any layout feedback loop.
     */
    private void fitKeyboard(int top, int bottom) {
        int available = getResources().getDisplayMetrics().heightPixels - top - bottom;
        boolean tight = available < dp(294);
        if (header != null && (header.getVisibility() == View.GONE) != tight)
            header.setVisibility(tight ? View.GONE : View.VISIBLE);
        if (dots != null && (dots.getVisibility() == View.GONE) != tight)
            dots.setVisibility(tight ? View.GONE : View.VISIBLE);
    }
    /** API 35 ignores ADJUST_RESIZE, so the keyboard arrives as IME insets instead. */
    private static int imeBottom(android.view.WindowInsets insets) {
        if (android.os.Build.VERSION.SDK_INT < 30) return 0;
        return insets.getInsets(android.view.WindowInsets.Type.ime()).bottom;
    }

    private void captureDraft() { if (query != null) draft = query.getText().toString(); }

    private void showHome() {
        base("home");
        header = row();
        TextView logo = text("Search", 17, ink); logo.setLetterSpacing(.02f); logo.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(logo, new LinearLayout.LayoutParams(0, dp(58), 1));
        TextView history = action("历史", () -> { captureDraft(); showHistory(); });
        history.setTextSize(14); history.setTextColor(muted); history.setContentDescription("搜索历史"); header.addView(history);
        TextView settings = action("设置", () -> { captureDraft(); showSettings(); });
        settings.setTextSize(14); settings.setTextColor(muted); settings.setContentDescription("设置"); header.addView(settings);
        root.addView(header);

        // The app grid owns the centre of the screen and swipes sideways like a
        // launcher page; the field stays pinned to the bottom, one thumb away.
        grid = new PagedAppGrid(this);
        grid.setListener(new PagedAppGrid.Listener() {
            @Override public void onPick(SearchTarget target) { openWith(target); }
            @Override public void onPage(int page, int total) { renderDots(page, total); }
        });
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));

        dots = row(); dots.setGravity(Gravity.CENTER);
        root.addView(dots, new LinearLayout.LayoutParams(-1, dp(16)));

        LinearLayout field = row(); field.setBackground(background(colors.field, 16, 0));
        query = new EditText(this); query.setSingleLine(true); query.setTextSize(17); query.setTextColor(ink); query.setHintTextColor(colors.hint);
        query.setHint("搜索…"); query.setBackgroundColor(Color.TRANSPARENT); query.setPadding(dp(18), 0, dp(4), 0);
        query.setGravity(Gravity.START | Gravity.CENTER_VERTICAL); query.setIncludeFontPadding(false);
        query.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        query.setTypeface(uiFont);
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH); query.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});
        query.setText(draft); query.setContentDescription("搜索关键词");
        field.addView(query, new LinearLayout.LayoutParams(0, dp(54), 1));
        TextView clear = action("×", () -> query.setText("")); clear.setTextSize(20); clear.setTextColor(muted); clear.setContentDescription("清空关键词");
        clear.setVisibility(draft.isEmpty() ? View.GONE : View.VISIBLE);
        query.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int length, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int length) { clear.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE); }
            public void afterTextChanged(android.text.Editable s) { }
        });
        field.addView(clear, new LinearLayout.LayoutParams(dp(40), dp(54)));
        root.addView(field, new LinearLayout.LayoutParams(-1, dp(54)));
        space(root, 14);
        query.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP)) { submit(); return true; }
            return false;
        });
        grid.setTargets(HomeApps.visible(this), selected);
        renderDots(0, 1);
        // The keyboard may already be up from the previous page, so the insets
        // are re-applied once the new views exist.
        root.requestApplyInsets();
        showKeyboard();
    }

    private void renderDots(int page, int total) {
        if (dots == null) return;
        dots.removeAllViews();
        if (total <= 1) return;
        for (int i = 0; i < total; i++) {
            View dot = new View(this);
            GradientDrawable shape = new GradientDrawable(); shape.setShape(GradientDrawable.OVAL);
            shape.setColor(i == page ? ink : colors.divider);
            dot.setBackground(shape);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(5), dp(5));
            params.leftMargin = dp(4); params.rightMargin = dp(4);
            dots.addView(dot, params);
        }
    }
    private TextView appLabel(String value, int color) {
        TextView label = text(value, 12, color);
        label.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        label.setSingleLine(true); label.setGravity(Gravity.CENTER);
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return label;
    }
    private void updateCount() { if (count != null) count.setText(getString(R.string.search_count, store.prefs.getLong("count", 0))); }
    /** Home always arrives ready to type: the field takes focus and the keyboard follows. */
    private void showKeyboard() {
        if (query == null) return;
        query.requestFocus();
        query.postDelayed(() -> {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) manager.showSoftInput(query, InputMethodManager.SHOW_IMPLICIT);
        }, 200);
        query.postDelayed(() -> {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            // Some ROMs drop the first implicit request while the window settles.
            if (manager != null && !manager.isActive(query)) manager.showSoftInput(query, InputMethodManager.SHOW_FORCED);
        }, 550);
    }

    private void submit() {
        String value = query.getText().toString().trim();
        if (value.isEmpty()) { query.requestFocus(); query.setError("先写下想搜索的内容"); return; }
        SearchTarget target = selected;
        if (target == null) for (SearchTarget t : HomeApps.visible(this)) { target = t; break; }
        if (target == null) { Toast.makeText(this, "没有可用的搜索应用", Toast.LENGTH_SHORT).show(); return; }
        selected = target;
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(query.getWindowToken(), 0);
        SearchLauncher.launch(this, store, target, value, this::updateCount);
    }
    /** One tap on a tile is the whole gesture: keyword first, then the app. */
    private void openWith(SearchTarget target) {
        String value = query.getText().toString().trim();
        if (value.isEmpty()) {
            query.requestFocus();
            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(query, InputMethodManager.SHOW_IMPLICIT);
            Toast.makeText(this, "先在下方输入关键词，再点应用", Toast.LENGTH_SHORT).show();
            return;
        }
        selected = target;
        grid.post(() -> grid.setSelected(target));
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(query.getWindowToken(), 0);
        SearchLauncher.launch(this, store, target, value, this::updateCount);
    }

    private LinearLayout page(String name) {
        base(name);
        LinearLayout header = row(); TextView title = text(name, 25, ink);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(80), 1)); title.setGravity(Gravity.CENTER_VERTICAL);
        TextView close = action("×", this::showHome); close.setTextSize(28); close.setContentDescription("返回首页"); close.setBackground(background(colors.field, 14, colors.divider)); header.addView(close); root.addView(header);
        ScrollView scroll = new ScrollView(this); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = column(); body.setPadding(0, dp(8), 0, dp(32)); scroll.addView(body); return body;
    }
    private void showHistory() {
        LinearLayout body = page("历史记录");
        body.addView(text("回溯每一次主动寻找。点击记录，再次搜索。", 12, muted)); space(body, 24);
        JSONArray entries = store.history();
        if (entries.length() == 0) { space(body, 70); body.addView(text("还没有搜索记录", 20, ink)); space(body, 12); body.addView(text("下一次主动寻找，会留在这里。", 14, muted)); return; }
        body.addView(settingAction("清空历史记录", () -> new AlertDialog.Builder(this).setTitle("清空历史记录？").setMessage("此操作会删除本机的搜索记录，累计次数保留。")
                .setNegativeButton("取消", null).setPositiveButton("清空", (d, w) -> { store.prefs.edit().remove("history").apply(); showHistory(); }).show()));
        String lastDay = "";
        SimpleDateFormat day = new SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA), time = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.optJSONObject(i); if (entry == null) continue;
            SearchTarget t = target(entry.optString("target")); if (t == null) continue;
            Date date = new Date(entry.optLong("time")); String dateText = day.format(date);
            if (!dateText.equals(lastDay)) { space(body, 22); body.addView(text(dateText, 17, ink)); space(body, 10); lastDay = dateText; }
            String q = entry.optString("query");
            LinearLayout record = column(); record.setPadding(0, dp(12), 0, dp(12));
            record.addView(text("用「" + t.label + "」搜索「" + q + "」", 15, ink)); space(record, 6);
            record.addView(text(time.format(date) + (entry.optBoolean("web") ? " · 网页" : " · 应用"), 11, muted));
            record.setContentDescription(t.label + "，" + q + "，再次搜索"); record.setFocusable(true);
            record.setOnClickListener(v -> { selected = t; draft = q; showHome(); submit(); });
            record.setOnLongClickListener(v -> { new AlertDialog.Builder(this).setMessage("删除这条记录？").setNegativeButton("取消", null)
                    .setPositiveButton("删除", (d, w) -> { JSONArray next = new JSONArray(); for (int n = 0; n < entries.length(); n++) if (entries.optJSONObject(n) != entry) next.put(entries.opt(n)); store.prefs.edit().putString("history", next.toString()).apply(); showHistory(); }).show(); return true; });
            body.addView(record); line(body);
        }
    }

    private TextView settingAction(String value, Runnable click) {
        TextView view = action(value, click); view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        view.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(colors.ripple), background(colors.field, 14, colors.divider), null));
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2); layout.bottomMargin = dp(10); view.setLayoutParams(layout); return view;
    }

    private void showSettings() {
        base("settings");
        SettingsUi ui = new SettingsUi(this);
        LinearLayout header = row();
        TextView title = ui.text("设置", 24, ink); header.addView(title, new LinearLayout.LayoutParams(0, dp(76), 1));
        TextView close = ui.text("×", 26, muted); close.setGravity(Gravity.CENTER); close.setContentDescription("返回首页");
        ui.interactive(close, colors.surface, 20, this::showHome); header.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40))); root.addView(header);
        ScrollView scroll = new ScrollView(this); scroll.setVerticalScrollBarEnabled(false); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = column(); body.setPadding(0, 0, 0, dp(32)); scroll.addView(body);
        LinearLayout preferences = ui.group(body, "搜索偏好");
        SearchTarget defaultTarget = target(store.prefs.getString("default_target", ""));
        ui.choice(preferences, "默认应用", defaultTarget == null ? "每次选择" : defaultTarget.label, () -> {
            java.util.List<SearchTarget> order = AppCatalog.ordered(this);
            String[] options = new String[order.size() + 1]; options[0] = "每次选择";
            for (int i=0; i<order.size(); i++) options[i+1] = order.get(i).label;
            new AlertDialog.Builder(this).setTitle("默认应用").setSingleChoiceItems(options, defaultTarget == null ? 0 : order.indexOf(defaultTarget) + 1, (d, which) -> {
                store.prefs.edit().putString("default_target", which == 0 ? "" : order.get(which-1).name()).apply(); d.dismiss(); showSettings();
            }).setNegativeButton("取消", null).show();
        });
        ui.toggle(preferences, "保存搜索历史", "仅保存在本机；关闭后不再新增", store.prefs.getBoolean("save_history", true),
                checked -> store.prefs.edit().putBoolean("save_history", checked).apply());
        ui.action(preferences, "首页应用", HomeApps.summary(this), () -> HomeAppsDialog.show(this, this::showHome));
        String mode = Appearance.mode(this);
        String modeLabel = "dark".equals(mode) ? "深色" : "light".equals(mode) ? "浅色" : "跟随系统";
        ui.choice(preferences, "外观", modeLabel, () -> {
            String[] modes = {"system", "light", "dark"};
            String[] labels = {"跟随系统", "浅色", "深色"};
            int current = "dark".equals(mode) ? 2 : "light".equals(mode) ? 1 : 0;
            new AlertDialog.Builder(this).setTitle("外观").setSingleChoiceItems(labels, current, (d, which) -> {
                d.dismiss();
                if (!modes[which].equals(Appearance.mode(this))) {
                    Appearance.save(this, modes[which]); SearchWidget.refresh(); recreate();
                }
            }).setNegativeButton("取消", null).show();
        });
        LinearLayout assistance = ui.group(body, "微信辅助");
        String status = WechatSearchService.status(this);
        ui.action(assistance, "微信搜索辅助", (WechatSearchService.ready() ? "已开启" : "未开启") + (status.isEmpty() ? " · 点击管理权限与搜索位置" : " · " + status),
                () -> WechatSearchService.showOptions(this, draft));
        LinearLayout desktop = ui.group(body, "桌面");
        ui.action(desktop, "添加桌面小组件", "4 × 1 · 搜索框与应用图标", () -> SearchWidget.add(this));
        LinearLayout about = ui.group(body, "关于");
        ui.action(about, "开源许可与源码", "GNU AGPL v3 · 查看许可与本版本源码", () -> LicenseInfo.show(this));
        TextView footer = ui.text("Search 1.1.0 · 让搜索直达目的地", 12, muted);
        footer.setGravity(Gravity.CENTER); footer.setPadding(0, dp(24), 0, 0); body.addView(footer);
    }
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && "home".equals(screen) && query != null && !query.hasFocus()) showKeyboard();
    }

    @Override public void onBackPressed() { if (!"home".equals(screen)) showHome(); else super.onBackPressed(); }
}
