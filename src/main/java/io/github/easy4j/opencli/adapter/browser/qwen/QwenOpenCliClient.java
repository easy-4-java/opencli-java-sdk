package io.github.easy4j.opencli.adapter.browser.qwen;

import tools.jackson.databind.JsonNode;
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
 * OpenCLI {@code qwen} 浏览器适配器 typed 客户端。
 *
 * <p>命令面与上游 {@code cli-manifest.json} 逐项对齐；位置参数在前、
 * 旗标在后；全部方法带 {@code more} 原生参数透传。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@RequiredArgsConstructor
public final class QwenOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.QWEN);
    }
    /** {@code qwen ask} 选项块。 */
    @Data
    @Builder(toBuilder = true)
    public static class QwenAskOptions {

        private Integer timeoutSeconds;

        private Boolean newConversation;

        private Boolean think;

        private Boolean research;

        private Boolean markdown;

        /** 追加 {@code -f json}（全局格式旗标）。 */
        private Boolean jsonOutput;

        public void appendTo(List<String> target) {
        if (Boolean.TRUE.equals(newConversation)) {
            target.add("--new");
        }
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
        }
        if (Boolean.TRUE.equals(think)) {
            target.add("--think");
        }
        if (Boolean.TRUE.equals(research)) {
            target.add("--research");
        }
        if (Boolean.TRUE.equals(markdown)) {
            target.add("--markdown");
        }
        if (Boolean.TRUE.equals(jsonOutput)) {
            target.add("-f");
            target.add("json");
        }
        }
    }

    /** {@code qwen image} 选项块。 */
    @Data
    @Builder(toBuilder = true)
    public static class QwenImageOptions {

        private String outputPath;

        private Boolean newConversation;

        private Boolean sdModel;

        private Integer timeoutSeconds;

        public void appendTo(List<String> target) {
        if (outputPath != null) {
            OpenCliArgSupport.addOptionPair(target, "--op", String.valueOf(outputPath));
        }
        if (Boolean.TRUE.equals(newConversation)) {
            target.add("--new");
        }
        if (Boolean.TRUE.equals(sdModel)) {
            target.add("--sd");
        }
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
        }
        }
    }


    /** {@code qwen ask <prompt>}。 */
    public OpenCliResult ask(String prompt, QwenAskOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("ask");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code qwen ask <prompt> -f json}，解析为 typed 结果。 */
    public OpenCliTypedResult<JsonNode> askTyped(String prompt, QwenAskOptions options, List<String> more) {
        QwenAskOptions withJson = options;
        if (withJson == null) {
            withJson = QwenAskOptions.builder().jsonOutput(true).build();
        } else if (!Boolean.TRUE.equals(withJson.getJsonOutput())) {
            withJson = withJson.toBuilder().jsonOutput(true).build();
        }
        return OpenCliStdoutJson.typed(ask(prompt, withJson, more));
    }

    /** {@code qwen send <prompt>}（fire-and-forget）。 */
    public OpenCliResult send(String prompt, QwenAskOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("send");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code qwen read [--markdown]}。 */
    public OpenCliResult read(Boolean markdown, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("read");
        if (Boolean.TRUE.equals(markdown)) {
            args.add("--markdown");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code qwen detail <id> [--markdown]}。 */
    public OpenCliResult detail(String conversationId, Boolean markdown, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("detail");
        args.add(conversationId);
        if (Boolean.TRUE.equals(markdown)) {
            args.add("--markdown");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code qwen history [--limit N]}。 */
    public OpenCliResult history(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("history");
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code qwen image <prompt>}（AI 生图）。 */
    public OpenCliResult image(String prompt, QwenImageOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("image");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code qwen login [--timeout N]}。 */
    public OpenCliResult login(Integer timeoutSeconds, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("login");
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(args, "--timeout", String.valueOf(timeoutSeconds));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code qwen new}。 */
    public OpenCliResult newChat(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("new"), more));
    }

    /** {@code qwen status}。 */
    public OpenCliResult status(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("status"), more));
    }

    /** {@code qwen whoami}。 */
    public OpenCliResult whoami(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("whoami"), more));
    }
}
