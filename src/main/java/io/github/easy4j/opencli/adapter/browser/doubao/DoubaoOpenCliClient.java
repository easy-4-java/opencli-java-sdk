package io.github.easy4j.opencli.adapter.browser.doubao;

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
 * OpenCLI {@code doubao} 浏览器适配器 typed 客户端。
 *
 * <p>命令面与上游 {@code cli-manifest.json} 逐项对齐；位置参数在前、
 * 旗标在后；全部方法带 {@code more} 原生参数透传。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@RequiredArgsConstructor
public final class DoubaoOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.DOUBAO);
    }
    /** {@code doubao ask} 选项块。 */
    @Data
    @Builder(toBuilder = true)
    public static class DoubaoAskOptions {

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


    /** {@code doubao ask <text>}。 */
    public OpenCliResult ask(String text, DoubaoAskOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("ask");
        args.add(text);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code doubao ask <text> -f json}，解析为 typed 结果。 */
    public OpenCliTypedResult<JsonNode> askTyped(String text, DoubaoAskOptions options, List<String> more) {
        DoubaoAskOptions withJson = options;
        if (withJson == null) {
            withJson = DoubaoAskOptions.builder().jsonOutput(true).build();
        } else if (!Boolean.TRUE.equals(withJson.getJsonOutput())) {
            withJson = withJson.toBuilder().jsonOutput(true).build();
        }
        return OpenCliStdoutJson.typed(ask(text, withJson, more));
    }

    /** {@code doubao send <text>}（fire-and-forget）。 */
    public OpenCliResult send(String text, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("send");
        args.add(text);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code doubao read}。 */
    public OpenCliResult read(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("read"), more));
    }

    /** {@code doubao detail <id>}。 */
    public OpenCliResult detail(String conversationId, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("detail");
        args.add(conversationId);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code doubao history [--limit N]}。 */
    public OpenCliResult history(String limit, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("history");
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", limit);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code doubao meeting-summary <id>}。 */
    public OpenCliResult meetingSummary(String conversationId, String chapters, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("meeting-summary");
        args.add(conversationId);
        if (chapters != null) {
            OpenCliArgSupport.addOptionPair(args, "--chapters", chapters);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code doubao meeting-transcript <id>}。 */
    public OpenCliResult meetingTranscript(String conversationId, String download, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("meeting-transcript");
        args.add(conversationId);
        if (download != null) {
            OpenCliArgSupport.addOptionPair(args, "--download", download);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code doubao login [--timeout N]}。 */
    public OpenCliResult login(Integer timeoutSeconds, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("login");
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(args, "--timeout", String.valueOf(timeoutSeconds));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code doubao new}。 */
    public OpenCliResult newChat(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("new"), more));
    }

    /** {@code doubao status}。 */
    public OpenCliResult status(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("status"), more));
    }

    /** {@code doubao whoami}。 */
    public OpenCliResult whoami(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("whoami"), more));
    }
}
