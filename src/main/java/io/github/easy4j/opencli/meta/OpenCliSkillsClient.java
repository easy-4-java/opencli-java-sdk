package io.github.easy4j.opencli.meta;

import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * {@code opencli skills *} 子命令封装。
 * <p>支持 {@code skills list} 与 {@code skills read}：列举 / 查看内置 opencli-* 技能包。</p>
 */
@RequiredArgsConstructor/**

 * Client for {@code opencli skills *} subcommands.
 *
 * <p>Supports {@code skills list} and {@code skills read}: list/view built-in opencli-* skill packs.</p>

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public final class OpenCliSkillsClient {

    private final OpenCliExecutor executor;

    private OpenCliResult invokeSkillsSub(List<String> rest) {
        List<String> tokens = new ArrayList<>();
        tokens.add("skills");
        tokens.addAll(rest);
        return executor.invoke(tokens);
    }

    /**
     * {@code skills list [-f <fmt>]} — 列出内置技能包。
     *
     * @param format 输出格式：table、json、yaml、md、csv；{@code null} 表示 CLI 默认 table
     */
    public OpenCliResult list(String format) {
        List<String> args = new ArrayList<>();
        args.add("list");
        if (Objects.nonNull(format)) {
            args.add("-f");
            args.add(format);
        }
        return invokeSkillsSub(args);
    }

    /** @see #list(String) */
    public OpenCliResult list() {
        return list(null);
    }

    /**
     * {@code skills read <skill> [path] [--json]} — 读取技能包内容或指定路径片段。
     *
     * @param skill  技能包 id，例如 {@code opencli-browse}
     * @param path   可选的子路径（{@code null} 表示读取整体）
     * @param asJson {@code --json} 开关；{@code true} 时以 JSON 形式输出
     */
    public OpenCliResult read(String skill, String path, boolean asJson) {
        List<String> args = new ArrayList<>();
        args.add("read");
        Objects.requireNonNull(skill, "skill");
        args.add(skill);
        if (path != null) {
            args.add(path);
        }
        if (asJson) {
            args.add("--json");
        }
        return invokeSkillsSub(args);
    }

    /** @see #read(String, String, boolean) */
    public OpenCliResult read(String skill) {
        return read(skill, null, false);
    }

    /** @see #read(String, String, boolean) */
    public OpenCliResult read(String skill, boolean asJson) {
        return read(skill, null, asJson);
    }

    /** @see #read(String, String, boolean) */
    public OpenCliResult read(String skill, String path) {
        return read(skill, path, false);
    }

    /**
     * Options 形式：{@code skills read <skill> [path] [--json]}。
     *
     * @param options 参数对象，不得为 null
     */
    public OpenCliResult read(OpenCliSkillsReadOptions options) {
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(options.getSkill(), "skill");
        return read(options.getSkill(), options.getPath(), Boolean.TRUE.equals(options.getAsJson()));
    }
}
