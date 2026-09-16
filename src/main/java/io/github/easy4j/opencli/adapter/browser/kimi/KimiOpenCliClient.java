package io.github.easy4j.opencli.adapter.browser.kimi;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.easy4j.opencli.core.OpenCliAdapterChannel;
import io.github.easy4j.opencli.core.OpenCliArgSupport;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.core.OpenCliTypedResult;
import io.github.easy4j.opencli.parser.OpenCliStdoutJson;
import io.github.easy4j.opencli.registry.OpenCliAdapterIds;
import io.github.easy4j.opencli.util.OpenCliLists;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * OpenCLI {@code kimi} 浏览器适配器 typed 客户端。
 *
 * <p>命令面与上游 {@code cli-manifest.json} 逐项对齐；位置参数在前、
 * 旗标在后；全部方法带 {@code more} 原生参数透传。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@RequiredArgsConstructor
public final class KimiOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.KIMI);
    }
    /** {@code kimi ask} 选项块。 */
    @Data
    @Builder(toBuilder = true)
    public static class KimiAskOptions {

        private Integer timeoutSeconds;

        /** 追加 {@code -f json}（全局格式旗标）。 */
        private Boolean jsonOutput;

        public void appendTo(List<String> target) {
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
        }
        if (Boolean.TRUE.equals(jsonOutput)) {
            target.add("-f");
            target.add("json");
        }
        }
    }

    /** {@code kimi read} 选项块。 */
    @Data
    @Builder(toBuilder = true)
    public static class KimiReadOptions {

        private String conv;

        private Integer limit;

        public void appendTo(List<String> target) {
        if (conv != null) {
            OpenCliArgSupport.addOptionPair(target, "--conv", String.valueOf(conv));
        }
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(target, "--limit", String.valueOf(limit));
        }
        }
    }

    /** {@code kimi model} 选项块（{@code --list} 与 {@code --set} 互斥使用）。 */
    @Data
    @Builder(toBuilder = true)
    public static class KimiModelOptions {

        private Boolean list;

        private String set;

        public void appendTo(List<String> target) {
        if (Boolean.TRUE.equals(list)) {
            target.add("--list");
        }
        if (set != null) {
            OpenCliArgSupport.addOptionPair(target, "--set", String.valueOf(set));
        }
        }
    }

    /** {@code kimi storage-keys} 选项块。 */
    @Data
    @Builder(toBuilder = true)
    public static class KimiStorageKeysOptions {

        private String storage;

        private String filter;

        private Integer limit;

        public void appendTo(List<String> target) {
        if (storage != null) {
            OpenCliArgSupport.addOptionPair(target, "--storage", String.valueOf(storage));
        }
        if (filter != null) {
            OpenCliArgSupport.addOptionPair(target, "--filter", String.valueOf(filter));
        }
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(target, "--limit", String.valueOf(limit));
        }
        }
    }

    /** {@code kimi templates} 选项块。 */
    @Data
    @Builder(toBuilder = true)
    public static class KimiTemplatesOptions {

        private String mode;

        private Integer limit;

        public void appendTo(List<String> target) {
        if (mode != null) {
            OpenCliArgSupport.addOptionPair(target, "--mode", String.valueOf(mode));
        }
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(target, "--limit", String.valueOf(limit));
        }
        }
    }


    /** {@code kimi ask <text>}。 */
    public OpenCliResult ask(String text, KimiAskOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("ask");
        args.add(text);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi ask <text> -f json}，解析为 typed 结果。 */
    public OpenCliTypedResult<JsonNode> askTyped(String text, KimiAskOptions options, List<String> more) {
        KimiAskOptions withJson = options;
        if (withJson == null) {
            withJson = KimiAskOptions.builder().jsonOutput(true).build();
        } else if (!Boolean.TRUE.equals(withJson.getJsonOutput())) {
            withJson = withJson.toBuilder().jsonOutput(true).build();
        }
        return OpenCliStdoutJson.typed(ask(text, withJson, more));
    }

    /** {@code kimi send <text>}（fire-and-forget）。 */
    public OpenCliResult send(String text, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("send");
        args.add(text);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi read [--conv <id>] [--limit N]}。 */
    public OpenCliResult read(String conv, Integer limit, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("read");
        if (conv != null) {
            OpenCliArgSupport.addOptionPair(args, "--conv", conv);
        }
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi detail <id> [--limit N]}。 */
    public OpenCliResult detail(String conversationId, Integer limit, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("detail");
        args.add(conversationId);
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi history [--limit N]}。 */
    public OpenCliResult history(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("history");
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi history-rename <chat-id> <new-title> [--yes]}。 */
    public OpenCliResult historyRename(String chatId, String newTitle, Boolean yes, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("history-rename");
        args.add(chatId);
        args.add(newTitle);
        if (Boolean.TRUE.equals(yes)) {
            args.add("--yes");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi copy-message [--conv <id>] [--click-button]}。 */
    public OpenCliResult copyMessage(String conv, Boolean clickButton, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("copy-message");
        if (conv != null) {
            OpenCliArgSupport.addOptionPair(args, "--conv", conv);
        }
        if (Boolean.TRUE.equals(clickButton)) {
            args.add("--click-button");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi react <kind> [--conv <id>]}（like/dislike）。 */
    public OpenCliResult react(String kind, String conv, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("react");
        args.add(kind);
        if (conv != null) {
            OpenCliArgSupport.addOptionPair(args, "--conv", conv);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi regenerate [--conv <id>]}。 */
    public OpenCliResult regenerate(String conv, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("regenerate");
        if (conv != null) {
            OpenCliArgSupport.addOptionPair(args, "--conv", conv);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi share [--conv <id>]}。 */
    public OpenCliResult share(String conv, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("share");
        if (conv != null) {
            OpenCliArgSupport.addOptionPair(args, "--conv", conv);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi mode <name>}（ppt/docs/deep-research/websites/...）。 */
    public OpenCliResult mode(String name, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("mode");
        args.add(name);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi model --list}。 */
    public OpenCliResult modelList(List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("model");
        args.add("--list");
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi model --set <model>}。 */
    public OpenCliResult modelSet(String model, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("model");
        OpenCliArgSupport.addOptionPair(args, "--set", model);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi storage-get <key>}。 */
    public OpenCliResult storageGet(String key, String storage, Integer maxBytes, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("storage-get");
        args.add(key);
        if (storage != null) {
            OpenCliArgSupport.addOptionPair(args, "--storage", storage);
        }
        if (maxBytes != null) {
            OpenCliArgSupport.addOptionPair(args, "--max-bytes", String.valueOf(maxBytes));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi storage-keys}。 */
    public OpenCliResult storageKeys(KimiStorageKeysOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("storage-keys");
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi templates}。 */
    public OpenCliResult templates(KimiTemplatesOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("templates");
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi account}。 */
    public OpenCliResult account(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("account"), more));
    }

    /** {@code kimi cookies}。 */
    public OpenCliResult cookies(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("cookies"), more));
    }

    /** {@code kimi idb-list}。 */
    public OpenCliResult idbList(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("idb-list"), more));
    }

    /** {@code kimi settings}。 */
    public OpenCliResult settings(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("settings"), more));
    }

    /** {@code kimi sidebar-toggle}。 */
    public OpenCliResult sidebarToggle(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("sidebar-toggle"), more));
    }

    /** {@code kimi dismiss-banner}。 */
    public OpenCliResult dismissBanner(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("dismiss-banner"), more));
    }

    /** {@code kimi upgrade}。 */
    public OpenCliResult upgrade(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("upgrade"), more));
    }

    /** {@code kimi usage}。 */
    public OpenCliResult usage(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("usage"), more));
    }

    /** {@code kimi view-all-history}。 */
    public OpenCliResult viewAllHistory(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("view-all-history"), more));
    }

    /** {@code kimi sign-out [--yes]}。 */
    public OpenCliResult signOut(Boolean yes, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("sign-out");
        if (Boolean.TRUE.equals(yes)) {
            args.add("--yes");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi login [--timeout N]}。 */
    public OpenCliResult login(Integer timeoutSeconds, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("login");
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(args, "--timeout", String.valueOf(timeoutSeconds));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code kimi new}。 */
    public OpenCliResult newChat(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("new"), more));
    }

    /** {@code kimi status}。 */
    public OpenCliResult status(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("status"), more));
    }

    /** {@code kimi whoami}。 */
    public OpenCliResult whoami(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("whoami"), more));
    }
}
