package io.github.hiwepy.opencli.adapter.browser.chatgpt;

import io.github.hiwepy.opencli.util.OpenCliLists;
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
 * OpenCLI {@code chatgpt}（ChatGPT Web）浏览器适配器。
 */
@RequiredArgsConstructor
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

        private Boolean readAsMarkdown;

        private Boolean jsonOutput;

        public void appendTo(List<String> target) {
            if (timeoutSeconds != null) {
                OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
            }
            if (historyLimit != null) {
                OpenCliArgSupport.addOptionPair(target, "--limit", String.valueOf(historyLimit));
            }
            if (readAsMarkdown != null) {
                OpenCliArgSupport.addOptionPair(target, "--markdown", String.valueOf(readAsMarkdown));
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
                OpenCliArgSupport.addOptionPair(target, "--sd", "true");
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
}
