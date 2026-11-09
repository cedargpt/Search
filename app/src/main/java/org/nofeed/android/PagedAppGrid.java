// SPDX-License-Identifier: AGPL-3.0-only
package org.nofeed.android;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Launcher-style pager: full-width pages of 4x3 app tiles that swipe sideways. */
final class PagedAppGrid extends HorizontalScrollView {
    interface Listener {
        void onPick(SearchTarget target);
        void onPage(int page, int total);
    }

    private static final int COLUMNS = 4, ROWS = 3;

    private final LinearLayout pages;
    private final List<SearchTarget> targets = new ArrayList<>();
    private final Runnable settle = this::snap;
    private Listener listener;
    private SearchTarget selected;
    private int builtWidth = -1, builtHeight = -1, builtCount = -1, totalPages = 1, currentPage = 0;

    PagedAppGrid(Context context) {
        super(context);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setClipToPadding(false);
        pages = new LinearLayout(context);
        pages.setOrientation(LinearLayout.HORIZONTAL);
        addView(pages, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
        pages.setBaselineAligned(false);
        setOnScrollChangeListener((v, scrollX, scrollY, oldX, oldY) -> {
            removeCallbacks(settle);
            postDelayed(settle, 90);
        });
        // Targets usually arrive before the first layout pass, so the measured
        // width has to trigger the build rather than the setter alone.
        addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> build());
    }

    void setListener(Listener listener) { this.listener = listener; }

    void setTargets(List<SearchTarget> order, SearchTarget selected) {
        targets.clear();
        targets.addAll(order);
        this.selected = selected;
        builtWidth = -1; builtHeight = -1;
        build();
        post(this::build);
        requestLayout();
    }

    void setSelected(SearchTarget target) {
        selected = target;
        int keep = getScrollX();
        builtWidth = -1; builtHeight = -1;
        build();
        scrollTo(keep, 0);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }


    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        build();
    }

    private void build() {
        int width = getWidth(), height = getHeight();
        if (width <= 0 || targets.isEmpty()) return;
        // The keyboard raises and lowers this view constantly, so the height is
        // part of the build key: a grid sized for the full screen would push
        // the last row underneath the keyboard once the IME covers it.
        if (width == builtWidth && height == builtHeight && targets.size() == builtCount) return;
        builtWidth = width; builtHeight = height; builtCount = targets.size();
        pages.removeAllViews();
        final int perPage = COLUMNS * ROWS;
        totalPages = Math.max(1, (targets.size() + perPage - 1) / perPage);
        int pageHeight = 0;
        // Rows shrink with whatever room the keyboard leaves free: three rows
        // always fit between the top of the screen and the raised IME.
        for (int page = 0; page < totalPages; page++) {
            LinearLayout column = new LinearLayout(getContext());
            column.setOrientation(LinearLayout.VERTICAL);
            column.setPadding(0, dp(4), 0, dp(4));
            final int cellHeight = Math.max(dp(48), Math.min(dp(124), (height - dp(8)) / ROWS));
            pageHeight = cellHeight * ROWS;
            for (int rowIndex = 0; rowIndex < ROWS; rowIndex++) {
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                for (int col = 0; col < COLUMNS; col++) {
                    int index = page * perPage + rowIndex * COLUMNS + col;
                    if (index < targets.size()) row.addView(tile(targets.get(index), cellHeight), cellParams());
                    else row.addView(new View(getContext()), cellParams());
                }
                column.addView(row, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, cellHeight));
            }
            pages.addView(column, new LinearLayout.LayoutParams(width, pageHeight));
        }
        // Give the strip an explicit size: a wrap-content HorizontalScrollView
        // child measures to zero inside a weighted parent, which leaves every
        // page invisible. Fixed pixels keep the measure pass deterministic.
        android.view.ViewGroup.LayoutParams strip = pages.getLayoutParams();
        strip.width = width * totalPages;
        strip.height = pageHeight;
        pages.setLayoutParams(strip);
        post(() -> { requestLayout(); invalidate(); });
        currentPage = Math.min(currentPage, totalPages - 1);
        scrollTo(currentPage * width, 0);
        if (listener != null) listener.onPage(currentPage, totalPages);
    }

    private LinearLayout.LayoutParams cellParams() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
    }

    private View tile(SearchTarget target, int cellSide) {
        Context context = getContext();
        boolean dark = Appearance.isDark(context);
        Appearance.Palette palette = Appearance.palette(context);
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setFocusable(true);
        // The tile follows the taller cell the 4x3 layout hands it, capped so
        // artwork never crowds the labels beside it.
        int size = Math.max(dp(34), Math.min(dp(56), cellSide - dp(22)));
        // A short cell (keyboard raised on a tall IME) has to fit artwork plus
        // label inside the row, so the gap closes before anything gets clipped.
        int gap = Math.max(dp(2), Math.min(dp(8), (cellSide - size - dp(14)) / 2));
        AppIconView icon = new AppIconView(this.getContext(), AppCatalog.icon(context, target));
        item.addView(icon, new LinearLayout.LayoutParams(size, size));
        View gapView = new View(context);
        item.addView(gapView, new LinearLayout.LayoutParams(1, gap));
        TextView label = new TextView(context);
        label.setText(target.label);
        label.setTextSize(11.5f);
        label.setTypeface(Typeface.DEFAULT);
        label.setSingleLine(true);
        label.setGravity(Gravity.CENTER);
        label.setTextColor(selected == target ? palette.ink : palette.muted);
        label.setIncludeFontPadding(false);
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        item.addView(label, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        boolean installed = AppCatalog.installed(context, target);
        if (selected == target) {
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setColor(palette.selected);
            shape.setCornerRadius(dp(20));
            item.setBackground(shape);
        }
        item.setContentDescription(target.label + (installed ? "" : "（未安装）"));
        item.setOnClickListener(v -> {
            selected = target;
            if (listener != null) listener.onPick(target);
        });
        return item;
    }

    private void snap() {
        int width = getWidth();
        if (width <= 0) return;
        int page = Math.max(0, Math.min(totalPages - 1, Math.round((float) getScrollX() / width)));
        smoothScrollTo(page * width, 0);
        if (page != currentPage) {
            currentPage = page;
            if (listener != null) listener.onPage(page, totalPages);
        }
    }

    int pageCount() { return totalPages; }
}
