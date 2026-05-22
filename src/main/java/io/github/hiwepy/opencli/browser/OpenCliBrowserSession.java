package io.github.hiwepy.opencli.browser;

import io.github.hiwepy.opencli.browser.support.OpenCliBrowserDragLocator;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserSemanticLocator;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserTabOptions;
import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import io.github.hiwepy.opencli.core.OpenCliExecutor;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.util.OpenCliLists;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * 具名浏览器会话上的子命令封装，对应 {@code opencli browser <session> <subcommand> ...}。
 */
@RequiredArgsConstructor
public final class OpenCliBrowserSession {

    private final OpenCliExecutor executor;
    private final String sessionName;

    private List<String> sessionPrefix() {
        List<String> tokens = new ArrayList<>();
        tokens.add("browser");
        tokens.add(sessionName);
        return tokens;
    }

    private OpenCliResult invokeSub(List<String> subcommandAndOptions) {
        List<String> tokens = sessionPrefix();
        tokens.addAll(subcommandAndOptions);
        return executor.invoke(tokens);
    }

    private static void appendTabAndLocator(
        List<String> args, OpenCliBrowserTabOptions tab, OpenCliBrowserSemanticLocator locator) {
        if (tab != null) {
            tab.appendTo(args);
        }
        if (locator != null) {
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

    /** {@code browser state} */
    public OpenCliResult state(OpenCliBrowserTabOptions tab, Boolean json) {
        List<String> args = new ArrayList<>();
        args.add("state");
        if (tab != null) {
            tab.appendTo(args);
        }
        if (Boolean.TRUE.equals(json)) {
            args.add("--json");
        }
        return invokeSub(args);
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
    public OpenCliResult screenshot(
        String path, OpenCliBrowserTabOptions tab, Boolean annotate, Boolean fullPage) {
        List<String> args = new ArrayList<>();
        args.add("screenshot");
        if (path != null) {
            args.add(path);
        }
        if (tab != null) {
            tab.appendTo(args);
        }
        if (Boolean.TRUE.equals(annotate)) {
            args.add("--annotate");
        }
        if (Boolean.TRUE.equals(fullPage)) {
            args.add("--full-page");
        }
        return invokeSub(args);
    }

    /** {@code browser console} */
    public OpenCliResult console(OpenCliBrowserTabOptions tab, Integer limit, String level) {
        List<String> args = new ArrayList<>();
        args.add("console");
        if (tab != null) {
            tab.appendTo(args);
        }
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        if (level != null) {
            OpenCliArgSupport.addOptionPair(args, "--level", level);
        }
        return invokeSub(args);
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

    /** {@code browser find} */
    public OpenCliResult find(
        String css,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        String source,
        Integer nth) {
        List<String> args = new ArrayList<>();
        args.add("find");
        if (css != null) {
            OpenCliArgSupport.addOptionPair(args, "--css", css);
        }
        appendTabAndLocator(args, tab, locator);
        if (source != null) {
            OpenCliArgSupport.addOptionPair(args, "--source", source);
        }
        if (nth != null) {
            OpenCliArgSupport.addOptionPair(args, "--nth", String.valueOf(nth));
        }
        return invokeSub(args);
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
        return getSub("text", target, locator, tab, nth, null, null);
    }

    /** {@code browser get value [target]} */
    public OpenCliResult getValue(
        String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab, Integer nth) {
        return getSub("value", target, locator, tab, nth, null, null);
    }

    /** {@code browser get html [target]} */
    public OpenCliResult getHtml(
        String target,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        Integer nth,
        String as,
        Integer depth) {
        return getSub("html", target, locator, tab, nth, as, depth);
    }

    /** {@code browser get attributes [target]} */
    public OpenCliResult getAttributes(
        String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab, Integer nth) {
        return getSub("attributes", target, locator, tab, nth, null, null);
    }

    private OpenCliResult getSub(
        String property,
        String target,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        Integer nth,
        String as,
        Integer depth) {
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
        if (as != null) {
            OpenCliArgSupport.addOptionPair(args, "--as", as);
        }
        if (depth != null) {
            OpenCliArgSupport.addOptionPair(args, "--depth", String.valueOf(depth));
        }
        return invokeSub(args);
    }

    /** {@code browser click [target]} */
    public OpenCliResult click(String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab) {
        return interaction("click", target, null, locator, tab, null);
    }

    /** {@code browser type [target] <text>} */
    public OpenCliResult type(
        String targetOrText,
        String text,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab,
        Boolean clear) {
        return interaction("type", targetOrText, text, locator, tab, clear);
    }

    /** {@code browser hover [target]} */
    public OpenCliResult hover(String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab) {
        return interaction("hover", target, null, locator, tab, null);
    }

    /** {@code browser focus [target]} */
    public OpenCliResult focus(String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab) {
        return interaction("focus", target, null, locator, tab, null);
    }

    /** {@code browser dblclick [target]} */
    public OpenCliResult dblclick(String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab) {
        return interaction("dblclick", target, null, locator, tab, null);
    }

    /** {@code browser check [target]} */
    public OpenCliResult check(String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab) {
        return interaction("check", target, null, locator, tab, null);
    }

    /** {@code browser uncheck [target]} */
    public OpenCliResult uncheck(String target, OpenCliBrowserSemanticLocator locator, OpenCliBrowserTabOptions tab) {
        return interaction("uncheck", target, null, locator, tab, null);
    }

    /** {@code browser upload [target] <files...>} */
    public OpenCliResult upload(
        String targetOrFile,
        List<String> files,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab) {
        List<String> args = new ArrayList<>();
        args.add("upload");
        if (targetOrFile != null) {
            args.add(targetOrFile);
        }
        if (files != null) {
            args.addAll(files);
        }
        appendTabAndLocator(args, tab, locator);
        return invokeSub(args);
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
        OpenCliBrowserTabOptions tab) {
        return interaction("fill", targetOrText, text, locator, tab, null);
    }

    /** {@code browser select [target] <option>} */
    public OpenCliResult select(
        String targetOrOption,
        String option,
        OpenCliBrowserSemanticLocator locator,
        OpenCliBrowserTabOptions tab) {
        return interaction("select", targetOrOption, option, locator, tab, null);
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
     */
    public OpenCliResult waitFor(
        String type,
        String value,
        OpenCliBrowserTabOptions tab,
        Integer timeoutSeconds) {
        List<String> args = new ArrayList<>();
        args.add("wait");
        args.add(type);
        if (value != null) {
            args.add(value);
        }
        if (tab != null) {
            tab.appendTo(args);
        }
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(args, "--timeout", String.valueOf(timeoutSeconds));
        }
        return invokeSub(args);
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

    /** {@code browser extract} */
    public OpenCliResult extract(OpenCliBrowserTabOptions tab, String selector, Integer maxChars) {
        List<String> args = new ArrayList<>();
        args.add("extract");
        if (tab != null) {
            tab.appendTo(args);
        }
        if (selector != null) {
            OpenCliArgSupport.addOptionPair(args, "--selector", selector);
        }
        if (maxChars != null) {
            OpenCliArgSupport.addOptionPair(args, "--max-chars", String.valueOf(maxChars));
        }
        return invokeSub(args);
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
        Boolean clear) {
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
        return invokeSub(args);
    }

    /**
     * {@code browser network} 选项块。
     */
    @lombok.Data
    @lombok.Builder
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
            if (detailKey != null) {
                OpenCliArgSupport.addOptionPair(target, "--detail", detailKey);
            }
            if (Boolean.TRUE.equals(all)) {
                target.add("--all");
            }
            if (Boolean.TRUE.equals(raw)) {
                target.add("--raw");
            }
            if (filterFields != null) {
                OpenCliArgSupport.addOptionPair(target, "--filter", filterFields);
            }
            if (since != null) {
                OpenCliArgSupport.addOptionPair(target, "--since", since);
            }
            if (until != null) {
                OpenCliArgSupport.addOptionPair(target, "--until", until);
            }
            if (Boolean.TRUE.equals(follow)) {
                target.add("--follow");
            }
            if (Boolean.TRUE.equals(failed)) {
                target.add("--failed");
            }
            if (maxBody != null) {
                OpenCliArgSupport.addOptionPair(target, "--max-body", maxBody);
            }
            if (ttlMs != null) {
                OpenCliArgSupport.addOptionPair(target, "--ttl", ttlMs);
            }
        }
    }

    /**
     * {@code browser verify} 选项块。
     */
    @lombok.Data
    @lombok.Builder
    public static class OpenCliBrowserVerifyOptions {

        private Boolean writeFixture;
        private Boolean updateFixture;
        private Boolean noFixture;
        private Boolean strictMemory;
        private String seedArgs;
        private String trace;

        public void appendTo(List<String> target) {
            if (Boolean.TRUE.equals(writeFixture)) {
                target.add("--write-fixture");
            }
            if (Boolean.TRUE.equals(updateFixture)) {
                target.add("--update-fixture");
            }
            if (Boolean.TRUE.equals(noFixture)) {
                target.add("--no-fixture");
            }
            if (Boolean.TRUE.equals(strictMemory)) {
                target.add("--strict-memory");
            }
            if (seedArgs != null) {
                OpenCliArgSupport.addOptionPair(target, "--seed-args", seedArgs);
            }
            if (trace != null) {
                OpenCliArgSupport.addOptionPair(target, "--trace", trace);
            }
        }
    }
}
