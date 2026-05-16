package io.github.hiwepy.opencli.adapter.browser.gemini;

import io.github.hiwepy.opencli.util.OpenCliLists;
import io.github.hiwepy.opencli.adapter.browser.support.BrowserLlmOptions;
import io.github.hiwepy.opencli.core.OpenCliAdapterChannel;
import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import io.github.hiwepy.opencli.core.OpenCliExecutor;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.registry.OpenCliAdapterIds;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * OpenCLI {@code gemini} 浏览器适配器。
 */
@RequiredArgsConstructor
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
                OpenCliArgSupport.addOptionPair(target, "--sd", "true");
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
    public OpenCliResult deepResearch(String prompt, BrowserLlmOptions options, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("deep-research");
        args.add(prompt);
        if (options != null) {
            options.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** 导出 Deep Research 报告 URL。 */
    public OpenCliResult deepResearchResult(String query, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("deep-research-result");
        args.add(query);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }
}
