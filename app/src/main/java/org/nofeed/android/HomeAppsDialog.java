// SPDX-License-Identifier: AGPL-3.0-only
package org.nofeed.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Picks which platforms sit on the home grid, in which order. */
final class HomeAppsDialog {
    private HomeAppsDialog() {}

    static void show(Activity activity, Runnable changed) {
        Appearance.Palette colors = Appearance.palette(activity);
        List<SearchTarget> available = HomeApps.installed(activity);
        if (available.isEmpty()) return;

        List<SearchTarget> working = new ArrayList<>(HomeApps.visible(activity));
        for (SearchTarget target : available) if (!working.contains(target)) working.add(target);
        Set<SearchTarget> checked = new LinkedHashSet<>(HomeApps.visible(activity));

        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(activity, 20), dp(activity, 6), dp(activity, 20), 0);

        TextView hint = new TextView(activity);
        hint.setText("勾选的应用会显示在首页，用 ↑ ↓ 调整顺序。");
        hint.setTextSize(12);
        hint.setTextColor(colors.muted);
        hint.setPadding(0, 0, 0, dp(activity, 6));
        body.addView(hint);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new LinearLayout.LayoutParams(-1, -2));
        body.addView(scroll, new LinearLayout.LayoutParams(-1, dp(activity, 360)));

        final Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            list.removeAllViews();
            for (int index = 0; index < working.size(); index++) {
                SearchTarget target = working.get(index);
                final int at = index;
                LinearLayout row = new LinearLayout(activity);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setMinimumHeight(dp(activity, 52));

                CheckBox box = new CheckBox(activity);
                box.setChecked(checked.contains(target));
                box.setButtonTintList(ColorStateList.valueOf(colors.accent));
                box.setContentDescription(target.label);
                box.setOnCheckedChangeListener((v, on) -> {
                    if (on) checked.add(target); else checked.remove(target);
                });
                row.addView(box);

                AppIconView icon = new AppIconView(activity, AppCatalog.icon(activity, target));
                row.addView(icon, new LinearLayout.LayoutParams(dp(activity, 26), dp(activity, 26)));

                TextView name = new TextView(activity);
                name.setText(target.label);
                name.setTextSize(15);
                name.setTypeface(Typeface.DEFAULT);
                name.setSingleLine(true);
                name.setTextColor(colors.ink);
                name.setPadding(dp(activity, 12), 0, 0, 0);
                row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));

                row.addView(step(activity, colors, "↑", at > 0, () -> {
                    SearchTarget moved = working.remove(at);
                    working.add(at - 1, moved);
                    rebuild[0].run();
                }));
                row.addView(step(activity, colors, "↓", at < working.size() - 1, () -> {
                    SearchTarget moved = working.remove(at);
                    working.add(at + 1, moved);
                    rebuild[0].run();
                }));
                list.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
        };
        rebuild[0].run();

        new AlertDialog.Builder(activity)
                .setTitle("首页应用")
                .setView(body)
                .setPositiveButton("完成", (dialog, which) -> {
                    List<SearchTarget> result = new ArrayList<>();
                    for (SearchTarget target : working) if (checked.contains(target)) result.add(target);
                    HomeApps.save(activity, result);
                    changed.run();
                })
                .setNeutralButton("恢复默认", (dialog, which) -> {
                    HomeApps.reset(activity);
                    changed.run();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private static TextView step(Context context, Appearance.Palette colors, String glyph,
                                 boolean enabled, Runnable click) {
        TextView button = new TextView(context);
        button.setText(glyph);
        button.setTextSize(17);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(dp(context, 40));
        button.setMinHeight(dp(context, 40));
        button.setTextColor(enabled ? colors.ink : colors.divider);
        button.setContentDescription("↑".equals(glyph) ? "上移" : "下移");
        if (enabled) button.setOnClickListener(v -> click.run());
        return button;
    }

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
