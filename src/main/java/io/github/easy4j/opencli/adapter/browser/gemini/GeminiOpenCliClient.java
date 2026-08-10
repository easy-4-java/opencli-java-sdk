package io.github.easy4j.opencli.adapter.browser.gemini;

import io.github.easy4j.opencli.util.OpenCliLists;
import io.github.easy4j.opencli.adapter.browser.support.BrowserLlmOptions;
import io.github.easy4j.opencli.core.OpenCliAdapterChannel;
import io.github.easy4j.opencli.core.OpenCliArgSupport;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.registry.OpenCliAdapterIds;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * OpenCLI {@code gemini} 浏览器适配器。
 */
@RequiredArgsConstructor/**

 * OpenCLI {@code gemini} browser adapter client.
 *
 * <p>Provides typed methods for Gemini Web interactions such as {@code ask}, {@code image},
 * {@code deepResearch}, {@code history}, and session management.</p>

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

public final class GeminiOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.GEMINI);
    }

    /** 新开 Gemini Web Chat。 */
    public OpenCliResult newChat(List<String> additionalRawArgs) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("new"), additionalRawArgs));
    }

    /** @see #newChat(List) */
    public OpenCliResult newChat() {
        return newChat(null);
    }

    /**
     * {@code ask}：返回助手回复（纯文本为主）。
     *
     * @param prompt  提示词
     * @param options 可选超时等，可为 null
     * @param more    透传参数，可为 null
     */
    public OpenCliResult ask(String prompt, BrowserLlmOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("ask");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /**
     * {@code image} 文生图参数块（短选项 {@code --rt}/{@code --st}/{@code --op}/{@code --sd}）。
     */
    @Data
    @Builder
    public static class GeminiImageArgs {

        private String aspectRatio;

        private String style;

        private String outputDir;

        /** Skip download：仅打印页面链接。 */
        private Boolean skipDownload;

        private Integer timeoutSeconds;

        public void appendTo(List<String> target) {
            if (aspectRatio != null) {
                OpenCliArgSupport.addOptionPair(target, "--rt", aspectRatio);
            }
            if (style != null) {
                OpenCliArgSupport.addOptionPair(target, "--st", style);
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

    /**
     * {@code gemini image <prompt>}。
     */
    public OpenCliResult image(String prompt, GeminiImageArgs imageArgs, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("image");
        args.add(prompt);
        if (imageArgs != null) {
            imageArgs.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** Deep Research 启动。 */
    public OpenCliResult deepResearch(String prompt, GeminiDeepResearchOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("deep-research");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** @see #deepResearch(String, GeminiDeepResearchOptions, List) */
    public OpenCliResult deepResearch(String prompt, BrowserLlmOptions options, List<String> more) {
        GeminiDeepResearchOptions mapped = null;
        if (options != null) {
            mapped = GeminiDeepResearchOptions.builder()
                .timeoutSeconds(options.getTimeoutSeconds())
                .jsonOutput(options.getJsonOutput())
                .build();
        }
        return deepResearch(prompt, mapped, more);
    }

    /**
     * {@code deep-research} 专用选项（含 {@code --tool}、{@code --confirm}）。
     */
    @Data
    @Builder
    public static class GeminiDeepResearchOptions {

        private Integer timeoutSeconds;

        private String model;

        private String thinking;

        private String tool;

        /** 确认按钮文案覆盖（CLI {@code --confirm} 字符串标签，非布尔开关）。 */
        private String confirmLabel;

        private Boolean jsonOutput;

        public void appendTo(List<String> target) {
            if (timeoutSeconds != null) {
                OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
            }
            if (model != null) {
                OpenCliArgSupport.addOptionPair(target, "--model", model);
            }
            if (thinking != null) {
                OpenCliArgSupport.addOptionPair(target, "--thinking", thinking);
            }
            if (tool != null) {
                OpenCliArgSupport.addOptionPair(target, "--tool", tool);
            }
            if (confirmLabel != null) {
                OpenCliArgSupport.addOptionPair(target, "--confirm", confirmLabel);
            }
            if (Boolean.TRUE.equals(jsonOutput)) {
                target.add("-f");
                target.add("json");
            }
        }
    }

    /** Gemini 会话详情。 */
    public OpenCliResult detail(String idOrUrl, List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("detail", idOrUrl), more));
    }

    public OpenCliResult history(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("history");
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult history(List<String> more) {
        return history(null, more);
    }

    public OpenCliResult login(Integer timeoutSeconds, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("login");
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(args, "--timeout", String.valueOf(timeoutSeconds));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult models(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("models"), more));
    }

    public OpenCliResult read(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("read"), more));
    }

    public OpenCliResult status(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("status"), more));
    }

    public OpenCliResult whoami(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("whoami"), more));
    }


    public OpenCliResult deepResearchResult(String query, String match, Integer timeoutSeconds, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("deep-research-result");
        args.add(query);
        if (match != null) {
            OpenCliArgSupport.addOptionPair(args, "--match", match);
        }
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(args, "--timeout", String.valueOf(timeoutSeconds));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** @see #deepResearchResult(String, String, Integer, List) */
    public OpenCliResult deepResearchResult(String query, List<String> more) {
        return deepResearchResult(query, null, null, more);
    }
}
