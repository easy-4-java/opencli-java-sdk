package io.github.easy4j.opencli.adapter.browser.chatgpt;

import io.github.easy4j.opencli.util.OpenCliLists;
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
 * OpenCLI {@code chatgpt}（ChatGPT Web）浏览器适配器。
 */
@RequiredArgsConstructor/**

 * OpenCLI {@code chatgpt} (ChatGPT Web) browser adapter client.
 *
 * <p>Provides typed methods for ChatGPT Web interactions such as {@code ask}, {@code send},
 * {@code history}, {@code image}, and session management.</p>

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

public final class ChatgptOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.CHATGPT);
    }

    @Data
    @Builder
    public static class ChatgptCommonOptions {

        private Integer timeoutSeconds;

        private Integer historyLimit;

        private Integer stableSeconds;

        private Boolean readAsMarkdown;

        private Boolean newConversation;

        private String conversation;

        private String project;

        private Boolean waitForResponse;

        private Boolean deepResearch;

        private Boolean webSearch;

        private Boolean jsonOutput;

        public void appendTo(List<String> target) {
            if (Boolean.TRUE.equals(newConversation)) {
                target.add("--new");
            }
            if (timeoutSeconds != null) {
                OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
            }
            if (historyLimit != null) {
                OpenCliArgSupport.addOptionPair(target, "--limit", String.valueOf(historyLimit));
            }
            if (stableSeconds != null) {
                OpenCliArgSupport.addOptionPair(target, "--stable", String.valueOf(stableSeconds));
            }
            if (readAsMarkdown != null) {
                OpenCliArgSupport.addOptionPair(target, "--markdown", String.valueOf(readAsMarkdown));
            }
            if (conversation != null) {
                OpenCliArgSupport.addOptionPair(target, "--conversation", conversation);
            }
            if (project != null) {
                OpenCliArgSupport.addOptionPair(target, "--project", project);
            }
            if (waitForResponse != null) {
                OpenCliArgSupport.addOptionPair(target, "--wait", String.valueOf(waitForResponse));
            }
            if (Boolean.TRUE.equals(deepResearch)) {
                target.add("--deep-research");
            }
            if (Boolean.TRUE.equals(webSearch)) {
                target.add("--web-search");
            }
            if (Boolean.TRUE.equals(jsonOutput)) {
                target.add("-f");
                target.add("json");
            }
        }
    }

    public OpenCliResult ask(String prompt, ChatgptCommonOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("ask");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult send(String prompt, ChatgptCommonOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("send");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult read(ChatgptCommonOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("read");
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult history(ChatgptCommonOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("history");
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult detail(String idOrUrl, ChatgptCommonOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("detail");
        args.add(idOrUrl);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult newChat(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("new"), more));
    }

    public OpenCliResult status(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("status"), more));
    }

    public OpenCliResult deepResearchResult(String id, Boolean wait, Integer timeoutSeconds,
        Integer stableSeconds, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("deep-research-result");
        args.add(id);
        if (wait != null) {
            OpenCliArgSupport.addOptionPair(args, "--wait", String.valueOf(wait));
        }
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(args, "--timeout", String.valueOf(timeoutSeconds));
        }
        if (stableSeconds != null) {
            OpenCliArgSupport.addOptionPair(args, "--stable", String.valueOf(stableSeconds));
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

    public OpenCliResult model(String model, String project, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("model");
        args.add(model);
        if (project != null) {
            OpenCliArgSupport.addOptionPair(args, "--project", project);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult projectFileAdd(String file, String id, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("project-file-add");
        args.add(file);
        OpenCliArgSupport.addOptionPair(args, "--id", id);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult projectList(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("project-list");
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult whoami(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("whoami"), more));
    }

    /**
     * {@code chatgpt image} 文生图参数。
     */
    @Data
    @Builder
    public static class ChatgptImageOptions {

        /** 参考图路径（{@code --image}）。 */
        private String referenceImagePath;

        /** 输出目录（{@code --op}）。 */
        private String outputDir;

        /** 跳过下载，仅返回链接（{@code --sd}）。 */
        private Boolean skipDownload;

        private Integer timeoutSeconds;

        public void appendTo(List<String> target) {
            if (referenceImagePath != null) {
                OpenCliArgSupport.addOptionPair(target, "--image", referenceImagePath);
            }
            if (outputDir != null) {
                OpenCliArgSupport.addOptionPair(target, "--op", outputDir);
            }
            if (Boolean.TRUE.equals(skipDownload)) {
                target.add("--sd");
            }
            if (timeoutSeconds != null) {
                OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
            }
        }
    }

    public OpenCliResult image(String prompt, ChatgptImageOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("image");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** @see #image(String, ChatgptImageOptions, List) */
    public OpenCliResult image(String prompt, List<String> more) {
        return image(prompt, null, more);
    }

    /** {@code chatgpt ask} 且 {@code -f json}。 */
    public OpenCliTypedResult<JsonNode> askTyped(String prompt, ChatgptCommonOptions options, List<String> more) {
        ChatgptCommonOptions opts = withJson(options);
        return OpenCliStdoutJson.typed(ask(prompt, opts, more));
    }

    /** {@code chatgpt history -f json}。 */
    public OpenCliTypedResult<JsonNode> historyTyped(ChatgptCommonOptions options, List<String> more) {
        ChatgptCommonOptions opts = withJson(options);
        return OpenCliStdoutJson.typed(history(opts, more));
    }

    private static ChatgptCommonOptions withJson(ChatgptCommonOptions options) {
        if (options == null) {
            return ChatgptCommonOptions.builder().jsonOutput(true).build();
        }
        if (Boolean.TRUE.equals(options.getJsonOutput())) {
            return options;
        }
        return ChatgptCommonOptions.builder()
            .timeoutSeconds(options.getTimeoutSeconds())
            .historyLimit(options.getHistoryLimit())
            .readAsMarkdown(options.getReadAsMarkdown())
            .newConversation(options.getNewConversation())
            .jsonOutput(true)
            .build();
    }
}
