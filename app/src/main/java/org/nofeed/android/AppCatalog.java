// SPDX-License-Identifier: AGPL-3.0-only
package org.nofeed.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import java.util.EnumSet;
import java.util.List;

final class AppCatalog {
    /** Bump when the built-in platform set changes so stale saved orders are dropped. */
    private static final int CATALOG_VERSION = 2;

    static void migrate(Context context) {
        LocalStore store = new LocalStore(context);
        if (store.prefs.getInt("catalog_version", 0) < CATALOG_VERSION) {
            store.prefs.edit().remove("target_order").putInt("catalog_version", CATALOG_VERSION).apply();
        }
    }

    static boolean installed(Context context, SearchTarget target) {
        try { context.getPackageManager().getApplicationInfo(target.packageName, 0); return true; }
        catch (PackageManager.NameNotFoundException | RuntimeException unavailable) { return false; }
    }
    static List<SearchTarget> ordered(Context context) {
        EnumSet<SearchTarget> installed = EnumSet.noneOf(SearchTarget.class);
        for (SearchTarget t : SearchTarget.values()) if (installed(context, t)) installed.add(t);
        return TargetOrdering.resolve(new LocalStore(context).prefs.getString("target_order", ""), installed);
    }
    static void save(Context context, List<SearchTarget> order) {
        new LocalStore(context).prefs.edit().putString("target_order", TargetOrdering.encode(order)).apply();
        SearchWidget.initialize(context); SearchWidget.refresh();
    }
    static void reset(Context context) {
        new LocalStore(context).prefs.edit().remove("target_order").apply();
        SearchWidget.initialize(context); SearchWidget.refresh();
    }
    static Drawable icon(Context context, SearchTarget target) {
        // Every tile borrows the icon the system hands back for that package, so
        // the grid follows whatever the phone's theme or launcher currently
        // shows (adaptive, themed or OEM-replaced artwork alike).
        PackageManager manager = context.getPackageManager();
        try { return manager.getApplicationIcon(target.packageName); }
        catch (PackageManager.NameNotFoundException | RuntimeException unavailable) {
            return context.getDrawable(R.drawable.ic_more_apps);
        }
    }
}
