// SPDX-License-Identifier: AGPL-3.0-only
package org.nofeed.android;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/** User-defined selection and order of the app tiles shown on the home grid. */
final class HomeApps {
    private static final String KEY = "home_apps";

    private HomeApps() {}

    /** Tiles the home grid shows: the saved selection, otherwise every installed platform. */
    static List<SearchTarget> visible(Context context) {
        List<SearchTarget> picked = new ArrayList<>();
        for (SearchTarget target : saved(context)) {
            if (AppCatalog.installed(context, target)) picked.add(target);
        }
        return picked.isEmpty() ? installed(context) : picked;
    }

    static List<SearchTarget> installed(Context context) {
        List<SearchTarget> result = new ArrayList<>();
        for (SearchTarget target : AppCatalog.ordered(context)) {
            if (AppCatalog.installed(context, target)) result.add(target);
        }
        return result;
    }

    static List<SearchTarget> saved(Context context) {
        String raw = new LocalStore(context).prefs.getString(KEY, "");
        List<SearchTarget> result = new ArrayList<>();
        for (String name : raw.split(",")) {
            if (name.isEmpty()) continue;
            try {
                SearchTarget target = SearchTarget.valueOf(name);
                if (!result.contains(target)) result.add(target);
            } catch (IllegalArgumentException ignored) {
                // A platform removed in a later build simply drops out of the saved order.
            }
        }
        return result;
    }

    static void save(Context context, List<SearchTarget> targets) {
        StringBuilder builder = new StringBuilder();
        for (SearchTarget target : targets) {
            if (builder.length() > 0) builder.append(',');
            builder.append(target.name());
        }
        new LocalStore(context).prefs.edit().putString(KEY, builder.toString()).apply();
    }

    static void reset(Context context) {
        new LocalStore(context).prefs.edit().remove(KEY).apply();
    }

    static String summary(Context context) {
        return visible(context).size() + " 个图标，点击可增删与排序";
    }
}
