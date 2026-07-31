package io.github.easy4j.opencli.browser;

import io.github.easy4j.opencli.browser.support.OpenCliBrowserConsoleOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserDragLocator;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserExtractOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserFindOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserGetHtmlOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserScreenshotOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserSemanticLocator;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserStateOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserTabOptions;
import io.github.easy4j.opencli.core.OpenCliArgSupport;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.util.OpenCliLists;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 具名浏览器会话上的子命令封装，对应 {@code opencli browser <session> <subcommand> ...}。
 */
@RequiredArgsConstructor
@Slf4j
public final class OpenCliBrowserSession {

    private final OpenCliExecutor executor;
    private final String sessionName;
    private final String windowMode;

    private List<String> sessionPrefix() {
        List<String> tokens = new ArrayList<>();
        tokens.add("browser");
        if (Objects.nonNull(windowMode)) {
            OpenCliArgSupport.addOptionPair(tokens, "--window", windowMode);
        }
        tokens.add(sessionName);
        return tokens;
    }

    private OpenCliResult invokeSub(List<String> subcommandAndOptions) {
        List<String> tokens = sessionPrefix();
        tokens.addAll(subcommandAndOptions);
        log.debug("OpenCLI browser session={} subcommandSummary={}", sessionName, summarizeSubcommand(tokens));
        return executor.invoke(tokens);
    }

    private static String summarizeSubcommand(List<String> tokens) {
        int start = tokens.indexOf("browser");
        int from = start >= 0 && start + 2 < tokens.size() ? start + 2 : 0;
        int limit = Math.min(tokens.size(), from + 3);
        if (from >= tokens.size()) {
            return "(root)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < limit; i++) {
            if (i > from) {
                sb.append(' ');
            }
            sb.append(tokens.get(i));
        }
        if (tokens.size() > limit) {
            sb.append(" ...");
        }
        return sb.toString();
    }

    private static void appendTabAndLocator(
        List<String> args, OpenCliBrowserTabOptions tab, OpenCliBrowserSemanticLocator locator) {
        if (Objects.nonNull(tab)) {
            tab.appendTo(args);
        }
        if (Objects.nonNull(locator)) {
            locator.appendTo(args);
        }
    }

    /** {@code browser bind} */
    public OpenCliResult bind() {
        return invokeSub(OpenCliLists.of("bind"));
    }

    /** {@code browser unbind} */
    public OpenCliResult unbind() {
        return invokeSub(OpenCliLists.of("unbind"));
    }

    /** {@code browser tab list} */
    public OpenCliResult tabList() {
        return invokeSub(OpenCliLists.of("tab", "list"));
    }

    /**
     * {@code browser tab new [url]}。
     *
     * @param url 可选初始 URL
     */
    public OpenCliResult tabNew(String url) {
        List<String> args = new ArrayList<>();
        args.add("tab");
        args.add("new");
        if (url != null) {
            args.add(url);
        }
        return invokeSub(args);
    }

    /** @see #tabNew(String) */
    public OpenCliResult tabNew() {
        return tabNew(null);
    }

    /**
     * {@code browser tab select [targetId]}。
     */
    public OpenCliResult tabSelect(String targetId, OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("tab");
        args.add("select");
        if (targetId != null) {
            args.add(targetId);
        }
        if (tab != null) {
            tab.appendTo(args);
        }
        return invokeSub(args);
    }

    /**
     * {@code browser tab close [targetId]}。
     */
    public OpenCliResult tabClose(String targetId, OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("tab");
        args.add("close");
        if (targetId != null) {
            args.add(targetId);
        }
        if (tab != null) {
            tab.appendTo(args);
        }
        return invokeSub(args);
    }

    /** {@code browser open <url>} */
    public OpenCliResult open(String url, OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("open");
        args.add(Objects.requireNonNull(url, "url"));
        if (tab != null) {
            tab.appendTo(args);
        }
        return invokeSub(args);
    }

    /** {@code browser back} */
    public OpenCliResult back(OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("back");
        if (tab != null) {
            tab.appendTo(args);
        }
        return invokeSub(args);
    }

    /**
     * {@code browser scroll <direction>}，{@code direction} 为 {@code up} 或 {@code down}。
     */
    public OpenCliResult scroll(String direction, String amountPixels, OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("scroll");
        args.add(direction);
        if (amountPixels != null) {
            OpenCliArgSupport.addOptionPair(args, "--amount", amountPixels);
        }
        if (tab != null) {
            tab.appendTo(args);
        }
        return invokeSub(args);
    }

    /**
     * {@code browser state}，对齐 {@code --source} 与 {@code --compare-sources}。
     */
    public OpenCliResult state(OpenCliBrowserTabOptions tab, OpenCliBrowserStateOptions options) {
        List<String> args = new ArrayList<>();
        args.add("state");
        if (tab != null) {
            tab.appendTo(args);
        }
        if (options != null) {
            options.appendTo(args);
        }
        return invokeSub(args);
    }

    /**
     * @deprecated 使用 {@link #state(OpenCliBrowserTabOptions, OpenCliBrowserStateOptions)}；
     *     CLI 无 {@code --json} 开关
     */
    @Deprecated
    public OpenCliResult state(OpenCliBrowserTabOptions tab, Boolean json) {
        OpenCliBrowserStateOptions options = null;
        if (Boolean.TRUE.equals(json)) {
            options = OpenCliBrowserStateOptions.builder().source("dom").build();
        }
        return state(tab, options);
    }

    /** {@code browser frames} */
    public OpenCliResult frames(OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("frames");
        if (tab != null) {
            tab.appendTo(args);
        }
        return invokeSub(args);
    }

    /**
     * {@code browser screenshot [path]}。
     */
    public OpenCliResult screenshot(String path, OpenCliBrowserTabOptions tab, OpenCliBrowserScreenshotOptions options) {
        List<String> args = new ArrayList<>();
        args.add("screenshot");
        if (path != null) {
            args.add(path);
        }
        if (tab != null) {
            tab.appendTo(args);
        }
        if (options != null) {
            options.appendTo(args);
        }
        return invokeSub(args);
    }

    /**
     * @deprecated 使用 {@link #screenshot(String, OpenCliBrowserTabOptions, OpenCliBrowserScreenshotOptions)}
     */
    @Deprecated
    public OpenCliResult screenshot(
        String path, OpenCliBrowserTabOptions tab, Boolean annotate, Boolean fullPage) {
        OpenCliBrowserScreenshotOptions options = OpenCliBrowserScreenshotOptions.builder()
            .annotate(annotate)
            .fullPage(fullPage)
            .build();
        return screenshot(path, tab, options);
    }

    /**
     * {@code browser console}，对齐 {@code --level}、{@code --since}、{@code --until}、{@code --follow}。
     */
    public OpenCliResult console(OpenCliBrowserTabOptions tab, OpenCliBrowserConsoleOptions options) {
        List<String> args = new ArrayList<>();
        args.add("console");
        if (tab != null) {
            tab.appendTo(args);
        }
        if (options != null) {
            options.appendTo(args);
        }
        return invokeSub(args);
    }

    /**
     * @deprecated 使用 {@link #console(OpenCliBrowserTabOptions, OpenCliBrowserConsoleOptions)}；
     *     CLI 无 {@code --limit}
     */
    @Deprecated
    public OpenCliResult console(OpenCliBrowserTabOptions tab, Integer limit, String level) {
        OpenCliBrowserConsoleOptions options = OpenCliBrowserConsoleOptions.builder().level(level).build();
        return console(tab, options);
    }

    /** {@code browser analyze <url>} */
    public OpenCliResult analyze(String url, OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("analyze");
        args.add(url);
        if (tab != null) {
            tab.appendTo(args);
        }
        return invokeSub(args);
    }

    /**
     * {@code browser find}，对齐 {@code --css}、{@code --limit}、{@code --text-max}。
     */
    public OpenCliResult find(
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        OpenCliBrowserFindOptions options) {
        List<String> args = new ArrayList<>();
        args.add("find");
        if (options != null) {
            options.appendTo(args);
        }
        appendTabAndLocator(args, tab, locator);
        return invokeSub(args);
    }

    /**
     * @deprecated 使用 {@link #find(OpenCliBrowserSemanticLocator, OpenCliBrowserTabOptions, OpenCliBrowserFindOptions)}；
     *     CLI 无 {@code --source}/{@code --nth}
     */
    @Deprecated
    public OpenCliResult find(
        String css,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        String source,
        Integer nth) {
        OpenCliBrowserFindOptions options = OpenCliBrowserFindOptions.builder().css(css).build();
        if (nth != null) {
            options = options.toBuilder().limit(nth).build();
        }
        return find(locator, tab, options);
    }

    /** {@code browser get title} */
    public OpenCliResult getTitle(OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("get");
        args.add("title");
        if (tab != null) {
            tab.appendTo(args);
        }
        return invokeSub(args);
    }

    /** {@code browser get url} */
    public OpenCliResult getUrl(OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("get");
        args.add("url");
        if (tab != null) {
            tab.appendTo(args);
        }
        return invokeSub(args);
    }

    /** {@code browser get text [target]} */
    public OpenCliResult getText(
        String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab, Integer nth) {
        return getSub("text", target, locator, tab, nth, null);
    }

    /** {@code browser get value [target]} */
    public OpenCliResult getValue(
        String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab, Integer nth) {
        return getSub("value", target, locator, tab, nth, null);
    }

    /**
     * {@code browser get html}，使用 {@code --selector} 等选项（无 positional target）。
     */
    public OpenCliResult getHtml(OpenCliBrowserTabOptions tab, OpenCliBrowserGetHtmlOptions options) {
        List<String> args = new ArrayList<>();
        args.add("get");
        args.add("html");
        if (tab != null) {
            tab.appendTo(args);
        }
        if (options != null) {
            options.appendTo(args);
        }
        return invokeSub(args);
    }

    /**
     * @deprecated 使用 {@link #getHtml(OpenCliBrowserTabOptions, OpenCliBrowserGetHtmlOptions)}；
     *     CLI {@code get html} 使用 {@code --selector} 而非 positional
     */
    @Deprecated
    public OpenCliResult getHtml(
        String target,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        Integer nth,
        String as,
        Integer depth) {
        OpenCliBrowserGetHtmlOptions options = OpenCliBrowserGetHtmlOptions.builder()
            .selector(target)
            .as(as)
            .depth(depth)
            .build();
        return getHtml(tab, options);
    }

    /** {@code browser get attributes [target]} */
    public OpenCliResult getAttributes(
        String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab, Integer nth) {
        return getSub("attributes", target, locator, tab, nth, null);
    }

    private OpenCliResult getSub(
        String property,
        String target,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        Integer nth,
        OpenCliBrowserGetHtmlOptions htmlOptions) {
        List<String> args = new ArrayList<>();
        args.add("get");
        args.add(property);
        if (target != null) {
            args.add(target);
        }
        appendTabAndLocator(args, tab, locator);
        if (nth != null) {
            OpenCliArgSupport.addOptionPair(args, "--nth", String.valueOf(nth));
        }
        if (htmlOptions != null) {
            htmlOptions.appendTo(args);
        }
        return invokeSub(args);
    }

    /** {@code browser click [target]} */
    public OpenCliResult click(
        String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab, Integer nth) {
        return interaction("click", target, null, locator, tab, null, nth);
    }

    /** @see #click(String, OpenCliBrowserSemanticLocator, OpenCliBrowserTabOptions, Integer) */
    public OpenCliResult click(String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab) {
        return click(target, locator, tab, null);
    }

    /** {@code browser type [target] <text>} */
    public OpenCliResult type(
        String targetOrText,
        String text,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        Boolean clear,
        Integer nth) {
        return interaction("type", targetOrText, text, locator, tab, clear, nth);
    }

    /** @see #type(String, String, OpenCliBrowserSemanticLocator, OpenCliBrowserTabOptions, Boolean, Integer) */
    public OpenCliResult type(
        String targetOrText,
        String text,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        Boolean clear) {
        return type(targetOrText, text, locator, tab, clear, null);
    }

    /** {@code browser hover [target]} */
    public OpenCliResult hover(
        String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab, Integer nth) {
        return interaction("hover", target, null, locator, tab, null, nth);
    }

    public OpenCliResult hover(String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab) {
        return hover(target, locator, tab, null);
    }

    /** {@code browser focus [target]} */
    public OpenCliResult focus(
        String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab, Integer nth) {
        return interaction("focus", target, null, locator, tab, null, nth);
    }

    public OpenCliResult focus(String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab) {
        return focus(target, locator, tab, null);
    }

    /** {@code browser dblclick [target]} */
    public OpenCliResult dblclick(
        String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab, Integer nth) {
        return interaction("dblclick", target, null, locator, tab, null, nth);
    }

    public OpenCliResult dblclick(String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab) {
        return dblclick(target, locator, tab, null);
    }

    /** {@code browser check [target]} */
    public OpenCliResult check(
        String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab, Integer nth) {
        return interaction("check", target, null, locator, tab, null, nth);
    }

    public OpenCliResult check(String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab) {
        return check(target, locator, tab, null);
    }

    /** {@code browser uncheck [target]} */
    public OpenCliResult uncheck(
        String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab, Integer nth) {
        return interaction("uncheck", target, null, locator, tab, null, nth);
    }

    public OpenCliResult uncheck(String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab) {
        return uncheck(target, locator, tab, null);
    }

    /** {@code browser upload [target] <files...>} */
    public OpenCliResult upload(
        String targetOrFile,
        List<String> files,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        Integer nth) {
        List<String> args = new ArrayList<>();
        args.add("upload");
        if (targetOrFile != null) {
            args.add(targetOrFile);
        }
        if (files != null) {
            args.addAll(files);
        }
        appendTabAndLocator(args, tab, locator);
        if (nth != null) {
            OpenCliArgSupport.addOptionPair(args, "--nth", String.valueOf(nth));
        }
        return invokeSub(args);
    }

    public OpenCliResult upload(
        String targetOrFile,
        List<String> files,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab) {
        return upload(targetOrFile, files, locator, tab, null);
    }

    /** {@code browser drag <source> <target>} */
    public OpenCliResult drag(
        String source,
        String target,
        OpenCliBrowserDragLocator locators,
        OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("drag");
        args.add(source);
        args.add(target);
        if (locators != null) {
            locators.appendTo(args);
        }
        if (tab != null) {
            tab.appendTo(args);
        }
        return invokeSub(args);
    }

    /** {@code browser fill [target] <text>} */
    public OpenCliResult fill(
        String targetOrText,
        String text,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        Integer nth) {
        return interaction("fill", targetOrText, text, locator, tab, null, nth);
    }

    public OpenCliResult fill(
        String targetOrText,
        String text,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab) {
        return fill(targetOrText, text, locator, tab, null);
    }

    /** {@code browser select [target] <option>} */
    public OpenCliResult select(
        String targetOrOption,
        String option,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        Integer nth) {
        return interaction("select", targetOrOption, option, locator, tab, null, nth);
    }

    public OpenCliResult select(
        String targetOrOption,
        String option,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab) {
        return select(targetOrOption, option, locator, tab, null);
    }

    /** {@code browser keys <key>} */
    public OpenCliResult keys(String key, OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("keys");
        args.add(key);
        if (tab != null) {
            tab.appendTo(args);
        }
        return invokeSub(args);
    }

    /** {@code browser dialog accept [--text]} */
    public OpenCliResult dialogAccept(String promptText, OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("dialog");
        args.add("accept");
        if (tab != null) {
            tab.appendTo(args);
        }
        if (promptText != null) {
            OpenCliArgSupport.addOptionPair(args, "--text", promptText);
        }
        return invokeSub(args);
    }

    /** {@code browser dialog dismiss} */
    public OpenCliResult dialogDismiss(OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("dialog");
        args.add("dismiss");
        if (tab != null) {
            tab.appendTo(args);
        }
        return invokeSub(args);
    }

    /**
     * {@code browser wait <type> [value]} — type 如 selector、text、time、xhr、download。
     *
     * @param timeoutMillis {@code --timeout} 毫秒（CLI 默认 10000）
     */
    public OpenCliResult waitFor(
        String type,
        String value,
        OpenCliBrowserTabOptions tab,
        Long timeoutMillis) {
        List<String> args = new ArrayList<>();
        args.add("wait");
        args.add(type);
        if (value != null) {
            args.add(value);
        }
        if (tab != null) {
            tab.appendTo(args);
        }
        if (timeoutMillis != null) {
            OpenCliArgSupport.addOptionPair(args, "--timeout", String.valueOf(timeoutMillis));
        }
        return invokeSub(args);
    }

    /**
     * @deprecated {@code --timeout} 单位为毫秒而非秒；请使用 {@link #waitFor(String, String, OpenCliBrowserTabOptions, Long)}
     */
    @Deprecated
    public OpenCliResult waitFor(
        String type,
        String value,
        OpenCliBrowserTabOptions tab,
        Integer timeoutSeconds) {
        Long millis = timeoutSeconds != null ? timeoutSeconds.longValue() * 1000L : null;
        return waitFor(type, value, tab, millis);
    }

    /** {@code browser eval <js>} */
    public OpenCliResult eval(String js, OpenCliBrowserTabOptions tab, Integer frameIndex) {
        List<String> args = new ArrayList<>();
        args.add("eval");
        args.add(js);
        if (tab != null) {
            tab.appendTo(args);
        }
        if (frameIndex != null) {
            OpenCliArgSupport.addOptionPair(args, "--frame", String.valueOf(frameIndex));
        }
        return invokeSub(args);
    }

    /**
     * {@code browser extract}，对齐 {@code --chunk-size} 与 {@code --start}。
     */
    public OpenCliResult extract(OpenCliBrowserTabOptions tab, OpenCliBrowserExtractOptions options) {
        List<String> args = new ArrayList<>();
        args.add("extract");
        if (tab != null) {
            tab.appendTo(args);
        }
        if (options != null) {
            options.appendTo(args);
        }
        return invokeSub(args);
    }

    /**
     * @deprecated 使用 {@link #extract(OpenCliBrowserTabOptions, OpenCliBrowserExtractOptions)}；
     *     CLI 使用 {@code --chunk-size}/{@code --start} 而非 {@code --max-chars}
     */
    @Deprecated
    public OpenCliResult extract(OpenCliBrowserTabOptions tab, String selector, Integer maxChars) {
        OpenCliBrowserExtractOptions options = OpenCliBrowserExtractOptions.builder()
            .selector(selector)
            .chunkSize(maxChars)
            .build();
        return extract(tab, options);
    }

    /** {@code browser network} */
    public OpenCliResult network(OpenCliBrowserNetworkOptions options, OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("network");
        if (tab != null) {
            tab.appendTo(args);
        }
        if (options != null) {
            options.appendTo(args);
        }
        return invokeSub(args);
    }

    /** {@code browser init <site/command>} */
    public OpenCliResult init(String siteCommandName) {
        List<String> args = new ArrayList<>();
        args.add("init");
        args.add(siteCommandName);
        return invokeSub(args);
    }

    /** {@code browser verify <site/command>} */
    public OpenCliResult verifyAdapter(String siteCommandName, OpenCliBrowserVerifyOptions options) {
        List<String> args = new ArrayList<>();
        args.add("verify");
        args.add(siteCommandName);
        if (options != null) {
            options.appendTo(args);
        }
        return invokeSub(args);
    }

    /** {@code browser close} */
    public OpenCliResult close() {
        return invokeSub(OpenCliLists.of("close"));
    }

    private OpenCliResult interaction(
        String verb,
        String firstArg,
        String secondArg,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        Boolean clear,
        Integer nth) {
        List<String> args = new ArrayList<>();
        args.add(verb);
        if (firstArg != null) {
            args.add(firstArg);
        }
        if (secondArg != null) {
            args.add(secondArg);
        }
        appendTabAndLocator(args, tab, locator);
        if (Boolean.TRUE.equals(clear)) {
            args.add("--clear");
        }
        if (nth != null) {
            OpenCliArgSupport.addOptionPair(args, "--nth", String.valueOf(nth));
        }
        return invokeSub(args);
    }

    /**
     * {@code browser network} 选项块。
     */
    @Data
    @Builder
    public static class OpenCliBrowserNetworkOptions {

        private String detailKey;
        private Boolean all;
        private Boolean raw;
        private String filterFields;
        private String since;
        private String until;
        private Boolean follow;
        private Boolean failed;
        private String maxBody;
        private String ttlMs;

        public void appendTo(List<String> target) {
            OpenCliArgSupport.addOptionPairIfPresent(target, "--detail", detailKey);
            OpenCliArgSupport.addFlagIfTrue(target, "--all", all);
            OpenCliArgSupport.addFlagIfTrue(target, "--raw", raw);
            OpenCliArgSupport.addOptionPairIfPresent(target, "--filter", filterFields);
            OpenCliArgSupport.addOptionPairIfPresent(target, "--since", since);
            OpenCliArgSupport.addOptionPairIfPresent(target, "--until", until);
            OpenCliArgSupport.addFlagIfTrue(target, "--follow", follow);
            OpenCliArgSupport.addFlagIfTrue(target, "--failed", failed);
            OpenCliArgSupport.addOptionPairIfPresent(target, "--max-body", maxBody);
            OpenCliArgSupport.addOptionPairIfPresent(target, "--ttl", ttlMs);
        }
    }

    /**
     * {@code browser verify} 选项块。
     */
    @Data
    @Builder
    public static class OpenCliBrowserVerifyOptions {

        private Boolean writeFixture;
        private Boolean updateFixture;
        private Boolean noFixture;
        private Boolean strictMemory;
        private String seedArgs;
        private String trace;

        public void appendTo(List<String> target) {
            OpenCliArgSupport.addFlagIfTrue(target, "--write-fixture", writeFixture);
            OpenCliArgSupport.addFlagIfTrue(target, "--update-fixture", updateFixture);
            OpenCliArgSupport.addFlagIfTrue(target, "--no-fixture", noFixture);
            OpenCliArgSupport.addFlagIfTrue(target, "--strict-memory", strictMemory);
            OpenCliArgSupport.addOptionPairIfPresent(target, "--seed-args", seedArgs);
            OpenCliArgSupport.addOptionPairIfPresent(target, "--trace", trace);
        }
    }
}
