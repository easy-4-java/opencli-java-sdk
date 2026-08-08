package io.github.easy4j.opencli.adapter.browser.claude;

import io.github.easy4j.opencli.util.OpenCliLists;
import io.github.easy4j.opencli.adapter.browser.support.BrowserLlmOptions;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.easy4j.opencli.core.OpenCliAdapterChannel;
import io.github.easy4j.opencli.core.OpenCliArgSupport;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.core.OpenCliTypedResult;
import io.github.easy4j.opencli.parser.OpenCliStdoutJson;
import io.github.easy4j.opencli.registry.OpenCliAdapterIds;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * OpenCLI {@code claude} 浏览器适配器。
 */
@RequiredArgsConstructor/**

 * OpenCLI {@code claude} browser adapter client.
 *
 * <p>Provides typed methods for Claude Web interactions such as {@code ask}, {@code send},
 * {@code history}, {@code detail}, and session management.</p>

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public final class ClaudeOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.CLAUDE);
    }

    @Data
    @Builder(toBuilder = true)
    public static class ClaudeAskOptions {

        private Integer timeoutSeconds;

        /** 文档：{@code --new} 布尔开关（此处若 true 仅追加 flag，不传值）。 */
        private Boolean newConversation;

        private String model;

        private Boolean adaptiveThinking;

        private String attachmentPath;

        private Boolean jsonOutput;

        /**
         * @param target argv
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
            if (Boolean.TRUE.equals(adaptiveThinking)) {
                target.add("--think");
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

    public OpenCliResult ask(String prompt, ClaudeAskOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("ask");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult send(String prompt, List<String> more) {
        return send(prompt, null, more);
    }

    /**
     * {@code claude send}，支持 {@code --new}。
     */
    public OpenCliResult send(String prompt, ClaudeAskOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("send");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult login(Integer timeoutSeconds, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("login");
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(args, "--timeout", String.valueOf(timeoutSeconds));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult whoami(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("whoami"), more));
    }
    public OpenCliResult newChat(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("new"), more));
    }

    public OpenCliResult status(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("status"), more));
    }

    public OpenCliResult read(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("read"), more));
    }

    public OpenCliResult history(List<String> more) {
        return history(null, more);
    }

    /**
     * {@code claude history [--limit N]}。
     */
    public OpenCliResult history(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("history");
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult detail(String conversationIdOrUrl, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("detail");
        args.add(conversationIdOrUrl);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code claude ask} 且 {@code -f json} 时解析 stdout。 */
    public OpenCliTypedResult<JsonNode> askTyped(String prompt, ClaudeAskOptions options, List<String> more) {
        ClaudeAskOptions withJson = options;
        if (withJson == null) {
            withJson = ClaudeAskOptions.builder().jsonOutput(true).build();
        } else if (!Boolean.TRUE.equals(withJson.getJsonOutput())) {
            withJson = withJson.toBuilder().jsonOutput(true).build();
        }
        return OpenCliStdoutJson.typed(ask(prompt, withJson, more));
    }

    /** {@code claude history -f json}。 */
    public OpenCliTypedResult<JsonNode> historyTyped(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("history");
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        args.add("-f");
        args.add("json");
        return OpenCliStdoutJson.typed(ch().invoke(OpenCliArgSupport.merge(args, more)));
    }
}
