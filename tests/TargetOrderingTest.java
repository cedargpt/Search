package org.nofeed.android;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public final class TargetOrderingTest {
    private static void check(boolean condition, String why) { if (!condition) throw new AssertionError(why); }
    public static void main(String[] args) {
        int total = SearchTarget.values().length;
        check(total == 16, "Sixteen targets");
        EnumSet<SearchTarget> installed = EnumSet.of(SearchTarget.TAOBAO, SearchTarget.XIAOHONGSHU);
        List<SearchTarget> defaults = TargetOrdering.resolve("", installed);
        check(defaults.size() == total, "All missing applications remain present");
        check(defaults.get(0) == SearchTarget.XIAOHONGSHU && defaults.get(1) == SearchTarget.TAOBAO,
                "Installed applications lead the default order, keeping declaration order among themselves");
        check(installed.equals(EnumSet.copyOf(defaults.subList(0, 2))), "Installed applications fill the leading slots");
        SearchTarget[] declared = {SearchTarget.DOUYIN, SearchTarget.ZHIHU, SearchTarget.XIAOHONGSHU,
                SearchTarget.BILIBILI, SearchTarget.TAOBAO, SearchTarget.XIANYU, SearchTarget.PINDUODUO,
                SearchTarget.DIANPING, SearchTarget.DOUBAN, SearchTarget.MEITUAN, SearchTarget.WECHAT,
                SearchTarget.AMAP, SearchTarget.NETEASE_MUSIC, SearchTarget.COOLAPK, SearchTarget.XUEQIU,
                SearchTarget.WEREAD};
        check(Arrays.equals(SearchTarget.values(), declared), "Target IDs follow the reviewed declaration order");

        String custom = "WECHAT,DOUBAN,TAOBAO,XIANYU,BILIBILI,PINDUODUO,XIAOHONGSHU";
        List<SearchTarget> upgraded = TargetOrdering.resolve(custom, installed);
        check(TargetOrdering.encode(upgraded.subList(0, 7)).equals(custom), "The saved seven-app manual order survives an upgrade unchanged");
        check(upgraded.size() == total, "Saved manual order still exposes every target");
        check(upgraded.subList(7, total).equals(Arrays.asList(SearchTarget.DOUYIN, SearchTarget.ZHIHU,
                SearchTarget.DIANPING, SearchTarget.MEITUAN, SearchTarget.AMAP, SearchTarget.NETEASE_MUSIC,
                SearchTarget.COOLAPK, SearchTarget.XUEQIU, SearchTarget.WEREAD)),
                "New apps append to the saved order instead of resetting it");

        String allCustom = TargetOrdering.encode(upgraded);
        check(TargetOrdering.encode(TargetOrdering.resolve(allCustom, installed)).equals(allCustom), "The expanded manual order round-trips");
        check(TargetOrdering.resolve(custom, EnumSet.of(SearchTarget.AMAP)).subList(0, 7).equals(upgraded.subList(0, 7)),
                "Installing a new app cannot reorder the user's saved seven apps");

        List<SearchTarget> partial = TargetOrdering.resolve("WECHAT,UNKNOWN,WECHAT", installed);
        check(partial.size() == total && partial.get(0) == SearchTarget.WECHAT && partial.get(1) == SearchTarget.XIAOHONGSHU,
                "Unknown and duplicate entries cannot hide targets; new targets append");
        check(TargetOrdering.resolve("UNKNOWN", installed).equals(defaults), "Invalid saved state falls back safely");
        check(TargetOrdering.resolve(null, EnumSet.noneOf(SearchTarget.class)).equals(Arrays.asList(SearchTarget.values())),
                "All-uninstalled state retains all icons in standard order");
        System.out.println("Target ordering tests passed (installed priority, manual override, persistence, malformed state, all missing).");
    }
}
