// SPDX-License-Identifier: AGPL-3.0-only
package org.nofeed.android;

import java.net.URLEncoder;

/** A platform adapter. Add platforms here; UI and dispatch remain generic. */
public enum SearchTarget {
    DOUYIN("抖音", "com.ss.android.ugc.aweme", "snssdk1128://search?keyword={query}", "https://www.douyin.com/search/{query}"),
    ZHIHU("知乎", "com.zhihu.android", "zhihu://search?q={query}", "https://www.zhihu.com/search?q={query}"),
    XIAOHONGSHU("小红书", "com.xingin.xhs", "xhsdiscover://search/result?keyword={query}", "https://www.xiaohongshu.com/search_result?keyword={query}"),
    BILIBILI("哔哩哔哩", "tv.danmaku.bili", "bilibili://search?keyword={query}", "https://search.bilibili.com/all?keyword={query}"),
    TAOBAO("淘宝", "com.taobao.taobao", "tbopen://m.taobao.com/tbopen/index.html?action=ali.open.nav&module=h5&h5Url=https%3A%2F%2Fs.taobao.com%2Fsearch%3Fq%3D{query}", "https://s.taobao.com/search?q={query}"),
    XIANYU("闲鱼", "com.taobao.idlefish", "fleamarket://idle_search?flutter=true&flutter_animated=false&initSubPageTag=searchResult&keyword={query}", "https://www.goofish.com/search?q={query}"),
    PINDUODUO("拼多多", "com.xunmeng.pinduoduo", "pinduoduo://com.xunmeng.pinduoduo/search_result.html?search_key={query}", "https://mobile.yangkeduo.com/search_result.html?search_key={query}"),
    DIANPING("大众点评", "com.dianping.v1", "dianping://searchshoplist?keyword={query}", "https://m.dianping.com/search/keyword/{query}/0_0"),
    DOUBAN("豆瓣", "com.douban.frodo", "douban://douban.com/search/result?q={query}", "https://www.douban.com/search?q={query}"),
    MEITUAN("美团", "com.sankuai.meituan", "imeituan://www.meituan.com/search/result?q={query}", null),
    WECHAT("微信", "com.tencent.mm", null, null),
    AMAP("高德地图", "com.autonavi.minimap", "androidamap://poi?sourceApplication=yisou&keywords={query}&dev=0", "https://uri.amap.com/search?keyword={query}&src=yisou&callnative=0"),
    NETEASE_MUSIC("网易云音乐", "com.netease.cloudmusic", "orpheus://search?keyword={query}", "https://music.163.com/search?keyword={query}"),
    COOLAPK("酷安", "com.coolapk.market", "coolmarket://search?q={query}", "https://www.coolapk.com/search?q={query}"),
    XUEQIU("雪球", "com.xueqiu.android", "xueqiu://xueqiu.com/search?q={query}", "https://xueqiu.com/k?q={query}"),
    WEREAD("微信读书", "com.tencent.weread", "weread://search?keyword={query}", "https://weread.qq.com/search?keyword={query}");

    public final String label, packageName, appTemplate, webTemplate;

    SearchTarget(String label, String packageName, String appTemplate, String webTemplate) {
        this.label = label; this.packageName = packageName;
        this.appTemplate = appTemplate; this.webTemplate = webTemplate;
    }

    public static String buildUrl(String template, String query) {
        if (query.trim().isEmpty()) throw new IllegalArgumentException("请输入关键词");
        if (!template.contains("{query}")) throw new IllegalArgumentException("链接需要包含 {query}");
        try {
            String encoded = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20");
            // Taobao's gateway contains an encoded URL. Escape the keyword once
            // for the inner search URL, then again for the outer h5Url value.
            if (template.contains("h5Url=")) encoded = URLEncoder.encode(encoded, "UTF-8").replace("+", "%20");
            return template.replace("{query}", encoded);
        } catch (java.io.UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
    }

    public String[] fallbackTemplates() {
        if (this == ZHIHU) return new String[]{"zhihu://search/result?q={query}", "zhihu://search/?q={query}"};
        if (this == XIANYU) return new String[]{"fleamarket://x_search_items?keyword={query}"};
        if (this == DOUYIN) return new String[]{"snssdk1128://search/result?keyword={query}", "snssdk1128://search/tabs?keyword={query}"};
        return new String[0];
    }

}
