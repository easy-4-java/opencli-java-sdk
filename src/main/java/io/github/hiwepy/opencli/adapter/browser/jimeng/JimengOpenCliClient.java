package io.github.hiwepy.opencli.adapter.browser.jimeng;

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
 * OpenCLI {@code jimeng}（即梦）浏览器适配器。
 */
@RequiredArgsConstructor
public final class JimengOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.JIMENG);
    }

    @Data
    @Builder
    public static class JimengGenerateOptions {

        private String model;

        private Integer waitSeconds;

        private Integer timeoutSeconds;

        private String workspace;

        /** 配音音色预设（{@code generate-audio} 的 {@code --tone}）。 */
        private String tone;

        /** 音色参考文件（{@code --tone-file}）。 */
        private String toneFile;

        /** 克隆参考文件（{@code --clone-file}）。 */
        private String cloneFile;

        private Boolean jsonOutput;

        public void appendTo(List<String> target) {
            if (model != null) {
                OpenCliArgSupport.addOptionPair(target, "--model", model);
            }
            if (waitSeconds != null) {
                OpenCliArgSupport.addOptionEquals(target, "--wait", String.valueOf(waitSeconds));
            }
            if (timeoutSeconds != null) {
                OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
            }
            if (workspace != null) {
                OpenCliArgSupport.addOptionEquals(target, "--workspace", workspace);
            }
            if (tone != null) {
                OpenCliArgSupport.addOptionPair(target, "--tone", tone);
            }
            if (toneFile != null) {
                OpenCliArgSupport.addOptionPair(target, "--tone-file", toneFile);
            }
            if (cloneFile != null) {
                OpenCliArgSupport.addOptionPair(target, "--clone-file", cloneFile);
            }
            if (Boolean.TRUE.equals(jsonOutput)) {
                target.add("-f");
                target.add("json");
            }
        }
    }

    private List<String> withGen(JimengGenerateOptions opt, List<String> prefix, List<String> more) {
        List<String> args = new ArrayList<>(prefix);
        if (opt != null) {
            opt.appendTo(args);
        }
        return OpenCliArgSupport.merge(args, more);
    }

    /** 文生图 {@code generate}。 */
    public OpenCliResult generate(String prompt, JimengGenerateOptions opt, List<String> more) {
        List<String> prefix = OpenCliLists.of("generate", prompt);
        return ch().invoke(withGen(opt, prefix, more));
    }

    /** 图生图。 */
    public OpenCliResult generateImage2Image(
        String prompt,
        String commaSeparatedImages,
        JimengGenerateOptions opt,
        List<String> more) {
        List<String> prefix = new ArrayList<>(OpenCliLists.of("generate-image2image", prompt));
        prefix.add("--images=" + commaSeparatedImages);
        return ch().invoke(withGen(opt, prefix, more));
    }

    /** 文生视频。 */
    public OpenCliResult generateVideo(String prompt, JimengGenerateOptions opt, List<String> more) {
        return ch().invoke(withGen(opt, OpenCliLists.of("generate-video", prompt), more));
    }

    /** 图生视频。 */
    public OpenCliResult generateImage2Video(
        String prompt,
        String imagePath,
        JimengGenerateOptions opt,
        List<String> more) {
        List<String> prefix = new ArrayList<>(OpenCliLists.of("generate-image2video", prompt));
        prefix.add("--image=" + imagePath);
        return ch().invoke(withGen(opt, prefix, more));
    }

    /** 配音。 */
    public OpenCliResult generateAudio(String prompt, JimengGenerateOptions opt, List<String> more) {
        return ch().invoke(withGen(opt, OpenCliLists.of("generate-audio", prompt), more));
    }

    /** 数字人。 */
    public OpenCliResult generateDigitalHuman(String prompt, JimengGenerateOptions opt, List<String> more) {
        return ch().invoke(withGen(opt, OpenCliLists.of("generate-digital-human", prompt), more));
    }

    /**
     * 动作模仿：首个位置参数为参考图路径。
     *
     * @param referenceImagePath 本地参考图
     * @param opt                生成选项
     * @param more               透传
     */
    public OpenCliResult generateActionCopy(String referenceImagePath, JimengGenerateOptions opt, List<String> more) {
        List<String> prefix = OpenCliLists.of("generate-action-copy", referenceImagePath);
        return ch().invoke(withGen(opt, prefix, more));
    }

    public OpenCliResult history(Integer limit, String type, String workspace, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("history");
        if (limit != null) {
            OpenCliArgSupport.addOptionEquals(args, "--limit", String.valueOf(limit));
        }
        if (type != null) {
            OpenCliArgSupport.addOptionEquals(args, "--type", type);
        }
        if (workspace != null) {
            OpenCliArgSupport.addOptionEquals(args, "--workspace", workspace);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult newWorkspace(String type, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("new");
        if (type != null) {
            OpenCliArgSupport.addOptionEquals(args, "--type", type);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult workspaces(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("workspaces"), more));
    }

    public OpenCliResult userCredit(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("user_credit"), more));
    }

    public OpenCliResult userAssets(String tab, Integer waitSeconds, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("user_assets");
        if (tab != null) {
            OpenCliArgSupport.addOptionEquals(args, "--tab", tab);
        }
        if (waitSeconds != null) {
            OpenCliArgSupport.addOptionEquals(args, "--wait", String.valueOf(waitSeconds));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult userSubscription(List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("user_subscription");
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }
}
