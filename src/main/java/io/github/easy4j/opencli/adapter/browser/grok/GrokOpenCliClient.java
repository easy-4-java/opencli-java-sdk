package io.github.easy4j.opencli.adapter.browser.grok;

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
 * OpenCLI {@code grok} 浏览器适配器 typed 客户端。
 *
 * <p>命令面与上游 {@code cli-manifest.json} 逐项对齐；位置参数在前、
 * 旗标在后；全部方法带 {@code more} 原生参数透传。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@RequiredArgsConstructor
public final class GrokOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.GROK);
    }
    /** {@code grok ask} 选项块。 */
    @Data
    @Builder(toBuilder = true)
    public static class GrokAskOptions {

        private Integer timeoutSeconds;

        private Boolean newConversation;

        /** 追加 {@code -f json}（全局格式旗标）。 */
        private Boolean jsonOutput;

        public void appendTo(List<String> target) {
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
        }
        if (Boolean.TRUE.equals(newConversation)) {
            target.add("--new");
        }
        if (Boolean.TRUE.equals(jsonOutput)) {
            target.add("-f");
            target.add("json");
        }
        }
    }

    /** {@code grok image} 选项块。 */
    @Data
    @Builder(toBuilder = true)
    public static class GrokImageOptions {

        private Integer timeoutSeconds;

        private Boolean newConversation;

        private Integer count;

        private String outputPath;

        public void appendTo(List<String> target) {
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
        }
        if (Boolean.TRUE.equals(newConversation)) {
            target.add("--new");
        }
        if (count != null) {
            OpenCliArgSupport.addOptionPair(target, "--count", String.valueOf(count));
        }
        if (outputPath != null) {
            OpenCliArgSupport.addOptionPair(target, "--out", String.valueOf(outputPath));
        }
        }
    }

    /** {@code grok export} 选项块。 */
    @Data
    @Builder(toBuilder = true)
    public static class GrokExportOptions {

        private Integer limit;

        private Integer maxScrolls;

        public void appendTo(List<String> target) {
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(target, "--limit", String.valueOf(limit));
        }
        if (maxScrolls != null) {
            OpenCliArgSupport.addOptionPair(target, "--maxScrolls", String.valueOf(maxScrolls));
        }
        }
    }

    /** {@code grok export-all} 选项块。 */
    @Data
    @Builder(toBuilder = true)
    public static class GrokExportAllOptions {

        private Integer limit;

        private Integer offset;

        private String manifestPath;

        private Integer maxScrolls;

        private Integer pageScrolls;

        private Integer pageTimeoutMs;

        private Integer delayMinMs;

        private Integer delayMaxMs;

        public void appendTo(List<String> target) {
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(target, "--limit", String.valueOf(limit));
        }
        if (offset != null) {
            OpenCliArgSupport.addOptionPair(target, "--offset", String.valueOf(offset));
        }
        if (manifestPath != null) {
            OpenCliArgSupport.addOptionPair(target, "--manifestPath", String.valueOf(manifestPath));
        }
        if (maxScrolls != null) {
            OpenCliArgSupport.addOptionPair(target, "--maxScrolls", String.valueOf(maxScrolls));
        }
        if (pageScrolls != null) {
            OpenCliArgSupport.addOptionPair(target, "--pageScrolls", String.valueOf(pageScrolls));
        }
        if (pageTimeoutMs != null) {
            OpenCliArgSupport.addOptionPair(target, "--pageTimeoutMs", String.valueOf(pageTimeoutMs));
        }
        if (delayMinMs != null) {
            OpenCliArgSupport.addOptionPair(target, "--delayMinMs", String.valueOf(delayMinMs));
        }
        if (delayMaxMs != null) {
            OpenCliArgSupport.addOptionPair(target, "--delayMaxMs", String.valueOf(delayMaxMs));
        }
        }
    }


    /** {@code grok ask <prompt>}。 */
    public OpenCliResult ask(String prompt, GrokAskOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("ask");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code grok ask <prompt> -f json}，解析为 typed 结果。 */
    public OpenCliTypedResult<JsonNode> askTyped(String prompt, GrokAskOptions options, List<String> more) {
        GrokAskOptions withJson = options;
        if (withJson == null) {
            withJson = GrokAskOptions.builder().jsonOutput(true).build();
        } else if (!Boolean.TRUE.equals(withJson.getJsonOutput())) {
            withJson = withJson.toBuilder().jsonOutput(true).build();
        }
        return OpenCliStdoutJson.typed(ask(prompt, withJson, more));
    }

    /** {@code grok send <prompt>}（fire-and-forget）。 */
    public OpenCliResult send(String prompt, Boolean newConversation, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("send");
        args.add(prompt);
        if (Boolean.TRUE.equals(newConversation)) {
            args.add("--new");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code grok read [--markdown]}。 */
    public OpenCliResult read(Boolean markdown, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("read");
        if (Boolean.TRUE.equals(markdown)) {
            args.add("--markdown");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code grok detail <id> [--markdown]}。 */
    public OpenCliResult detail(String conversationId, Boolean markdown, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("detail");
        args.add(conversationId);
        if (Boolean.TRUE.equals(markdown)) {
            args.add("--markdown");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code grok history [--limit N]}。 */
    public OpenCliResult history(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("history");
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code grok delete <id>}（立即生效，需 --yes 确认）。 */
    public OpenCliResult delete(String conversationId, Boolean yes, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("delete");
        args.add(conversationId);
        if (Boolean.TRUE.equals(yes)) {
            args.add("--yes");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code grok pin <id>}。 */
    public OpenCliResult pin(String conversationId, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("pin");
        args.add(conversationId);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code grok unpin <id>}。 */
    public OpenCliResult unpin(String conversationId, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("unpin");
        args.add(conversationId);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code grok image <prompt>}。 */
    public OpenCliResult image(String prompt, GrokImageOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("image");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code grok export}。 */
    public OpenCliResult export(GrokExportOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("export");
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code grok export-all}。 */
    public OpenCliResult exportAll(GrokExportAllOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("export-all");
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code grok login [--timeout N]}。 */
    public OpenCliResult login(Integer timeoutSeconds, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("login");
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(args, "--timeout", String.valueOf(timeoutSeconds));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code grok new}。 */
    public OpenCliResult newChat(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("new"), more));
    }

    /** {@code grok status}。 */
    public OpenCliResult status(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("status"), more));
    }

    /** {@code grok whoami}。 */
    public OpenCliResult whoami(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("whoami"), more));
    }
}
