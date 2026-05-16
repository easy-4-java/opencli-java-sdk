package io.github.hiwepy.opencli.registry;

import java.util.Collections;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文档 index 中的 Browser / Desktop 分类（由脚本生成）。
 */
public final class OpenCliAdapterTaxonomy {

    private OpenCliAdapterTaxonomy() {
    }

    /** Desktop 适配器 id 列表（固定集）。 */
    public static final List<String> DESKTOP_IDS = Collections.unmodifiableList(Arrays.asList(
        "antigravity",
        "chatgpt-app",
        "chatwise",
        "codex",
        "cursor",
        "discord",
        "doubao-app"
    ));

    /** Browser 文档链接中的适配器 id。 */
    public static final List<String> BROWSER_IDS = Collections.unmodifiableList(Arrays.asList(
        "1688",
        "36kr",
        "51job",
        "aibase",
        "amazon",
        "apple-podcasts",
        "arxiv",
        "baidu-scholar",
        "band",
        "barchart",
        "bbc",
        "bilibili",
        "binance",
        "bloomberg",
        "bluesky",
        "boss",
        "chaoxing",
        "chatgpt",
        "claude",
        "coingecko",
        "coupang",
        "crates",
        "ctrip",
        "dblp",
        "defillama",
        "devto",
        "dianping",
        "dictionary",
        "dockerhub",
        "douban",
        "doubao",
        "douyin",
        "eastmoney",
        "endoflife",
        "facebook",
        "flathub",
        "gemini",
        "gitee",
        "google",
        "google-scholar",
        "goproxy",
        "gov-law",
        "gov-policy",
        "grok",
        "hackernews",
        "hf",
        "homebrew",
        "hupu",
        "imdb",
        "indeed",
        "instagram",
        "jd",
        "jike",
        "jimeng",
        "ke",
        "lesswrong",
        "lichess",
        "linkedin",
        "linux-do",
        "lobsters",
        "maimai",
        "maven",
        "mdn",
        "medium",
        "mubu",
        "notebooklm",
        "nowcoder",
        "npm",
        "nuget",
        "nvd",
        "oeis",
        "ones",
        "openalex",
        "openfda",
        "openreview",
        "osv",
        "packagist",
        "paperreview",
        "pixiv",
        "powerchina",
        "producthunt",
        "pubmed",
        "pypi",
        "quark",
        "reddit",
        "rednote",
        "rest-countries",
        "reuters",
        "rfc",
        "rubygems",
        "sinablog",
        "sinafinance",
        "smzdm",
        "spotify",
        "stackoverflow",
        "steam",
        "substack",
        "tdx",
        "ths",
        "tieba",
        "tiktok",
        "toutiao",
        "tvmaze",
        "twitter",
        "uisdc",
        "uiverse",
        "v2ex",
        "wanfang",
        "web",
        "weibo",
        "weixin",
        "weread",
        "wikidata",
        "wikipedia",
        "wttr",
        "xianyu",
        "xiaoe",
        "xiaohongshu",
        "xiaoyuzhou",
        "xueqiu",
        "yahoo-finance",
        "yollomi",
        "youtube",
        "yuanbao",
        "zhihu",
        "zlibrary",
        "zsxq"
    ));

    private static final Set<String> DESKTOP_ID_SET = new HashSet<>(DESKTOP_IDS);

    /**
     * @param adapterId OpenCLI adapter id
     * @return 是否属于 Desktop 表
     */
    public static boolean isDesktopAdapter(String adapterId) {
        return adapterId != null && DESKTOP_ID_SET.contains(adapterId);
    }
}
