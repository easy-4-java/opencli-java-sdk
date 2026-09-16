package io.github.easy4j.opencli.adapter.browser.yuanbao;

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
 * OpenCLI {@code yuanbao} 浏览器适配器 typed 客户端。
 *
 * <p>命令面与上游 {@code cli-manifest.json} 逐项对齐；位置参数在前、
 * 旗标在后；全部方法带 {@code more} 原生参数透传。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@RequiredArgsConstructor
public final class YuanbaoOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.YUANBAO);
    }
    /** {@code yuanbao ask} 选项块。 */
    @Data
    @Builder(toBuilder = true)
    public static class YuanbaoAskOptions {

        private Integer timeoutSeconds;

        private Boolean search;

        private Boolean think;

        /** 追加 {@code -f json}（全局格式旗标）。 */
        private Boolean jsonOutput;

        public void appendTo(List<String> target) {
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
        }
        if (Boolean.TRUE.equals(search)) {
            target.add("--search");
        }
        if (Boolean.TRUE.equals(think)) {
            target.add("--think");
        }
        if (Boolean.TRUE.equals(jsonOutput)) {
            target.add("-f");
            target.add("json");
        }
        }
    }


    /** {@code yuanbao ask <prompt>}。 */
    public OpenCliResult ask(String prompt, YuanbaoAskOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("ask");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code yuanbao ask <prompt> -f json}，解析为 typed 结果。 */
    public OpenCliTypedResult<JsonNode> askTyped(String prompt, YuanbaoAskOptions options, List<String> more) {
        YuanbaoAskOptions withJson = options;
        if (withJson == null) {
            withJson = YuanbaoAskOptions.builder().jsonOutput(true).build();
        } else if (!Boolean.TRUE.equals(withJson.getJsonOutput())) {
            withJson = withJson.toBuilder().jsonOutput(true).build();
        }
        return OpenCliStdoutJson.typed(ask(prompt, withJson, more));
    }

    /** {@code yuanbao send <prompt>}（fire-and-forget）。 */
    public OpenCliResult send(String prompt, Boolean newConversation, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("send");
        args.add(prompt);
        if (Boolean.TRUE.equals(newConversation)) {
            args.add("--new");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code yuanbao read}。 */
    public OpenCliResult read(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("read"), more));
    }

    /** {@code yuanbao detail <id>}。 */
    public OpenCliResult detail(String conversationId, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("detail");
        args.add(conversationId);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code yuanbao history [--limit N]}。 */
    public OpenCliResult history(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("history");
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code yuanbao login [--timeout N]}。 */
    public OpenCliResult login(Integer timeoutSeconds, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("login");
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(args, "--timeout", String.valueOf(timeoutSeconds));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code yuanbao new}。 */
    public OpenCliResult newChat(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("new"), more));
    }

    /** {@code yuanbao status}。 */
    public OpenCliResult status(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("status"), more));
    }

    /** {@code yuanbao whoami}。 */
    public OpenCliResult whoami(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("whoami"), more));
    }
}
