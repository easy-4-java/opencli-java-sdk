package io.github.hiwepy.opencli.adapter.browser.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hiwepy.opencli.core.OpenCliAdapterChannel;
import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import io.github.hiwepy.opencli.core.OpenCliExecutor;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.core.OpenCliTypedResult;
import io.github.hiwepy.opencli.parser.OpenCliStdoutJson;
import io.github.hiwepy.opencli.registry.OpenCliAdapterIds;
import io.github.hiwepy.opencli.util.OpenCliLists;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * OpenCLI {@code deepseek} 浏览器适配器。
 */
@RequiredArgsConstructor
public final class DeepseekOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.DEEPSEEK);
    }

    /**
     * {@code deepseek ask} 选项块。
     */
    @Data
    @Builder(toBuilder = true)
    public static class DeepseekAskOptions {

        private Integer timeoutSeconds;

        private Boolean newConversation;

        /** instant、expert、vision。 */
        private String model;

        private Boolean deepThink;

        private Boolean webSearch;

        private String attachmentPath;

        private Boolean jsonOutput;

        /**
         * 追加 ask/send 共用选项到 argv。
         *
         * @param target argv 列表
         */
        public void appendTo(List<String> target) {
            if (Boolean.TRUE.equals(newConversation)) {
                target.add("--new");
            }
            if (timeoutSeconds != null) {
                OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
            }
            if (model != null) {
                OpenCliArgSupport.addOptionPair(target, "--model", model);
            }
            if (Boolean.TRUE.equals(deepThink)) {
                target.add("--think");
            }
            if (Boolean.TRUE.equals(webSearch)) {
                target.add("--search");
            }
            if (attachmentPath != null) {
                OpenCliArgSupport.addOptionPair(target, "--file", attachmentPath);
            }
            if (Boolean.TRUE.equals(jsonOutput)) {
                target.add("-f");
                target.add("json");
            }
        }
    }

    /** {@code deepseek ask <prompt>}。 */
    public OpenCliResult ask(String prompt, DeepseekAskOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("ask");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code deepseek send <id> <prompt>}。 */
    public OpenCliResult send(String conversationId, String prompt, DeepseekAskOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("send");
        args.add(conversationId);
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code deepseek new}。 */
    public OpenCliResult newChat(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("new"), more));
    }

    /** {@code deepseek status}。 */
    public OpenCliResult status(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("status"), more));
    }

    /** {@code deepseek read}。 */
    public OpenCliResult read(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("read"), more));
    }

    /** {@code deepseek history [--limit N]}。 */
    public OpenCliResult history(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("history");
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code deepseek detail <id>}。 */
    public OpenCliResult detail(String conversationIdOrUrl, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("detail");
        args.add(conversationIdOrUrl);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code deepseek ask -f json}。 */
    public OpenCliTypedResult<JsonNode> askTyped(String prompt, DeepseekAskOptions options, List<String> more) {
        DeepseekAskOptions withJson = options;
        if (withJson == null) {
            withJson = DeepseekAskOptions.builder().jsonOutput(true).build();
        } else if (!Boolean.TRUE.equals(withJson.getJsonOutput())) {
            withJson = withJson.toBuilder().jsonOutput(true).build();
        }
        return OpenCliStdoutJson.typed(ask(prompt, withJson, more));
    }
}
