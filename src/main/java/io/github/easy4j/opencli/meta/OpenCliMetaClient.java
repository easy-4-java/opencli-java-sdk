package io.github.easy4j.opencli.meta;

import io.github.easy4j.opencli.core.OpenCliArgSupport;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.util.OpenCliLists;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * OpenCLI 根级元命令：{@code list}、{@code validate}、{@code verify}、{@code doctor}、{@code convention-audit}。
 */
@RequiredArgsConstructor/**

 * OpenCLI root-level meta commands: {@code list}, {@code validate}, {@code verify},
 * {@code doctor}, and {@code convention-audit}.

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

public final class OpenCliMetaClient {

    private final OpenCliExecutor executor;

    private OpenCliResult invokeRoot(List<String> tokens) {
        return executor.invoke(tokens);
    }

    /**
     * {@code opencli list}。
     *
     * @param format 输出格式：table、json、yaml、md、csv；null 表示 CLI 默认 table
     */
    public OpenCliResult list(String format) {
        List<String> args = new ArrayList<>();
        args.add("list");
        if (Objects.nonNull(format)) {
            args.add("-f");
            args.add(format);
        }
        return invokeRoot(args);
    }

    /** @see #list(String) */
    public OpenCliResult list() {
        return list(null);
    }

    /**
     * {@code opencli validate [target]}。
     *
     * @param target site 或 site/name，可为 null
     */
    public OpenCliResult validate(String target) {
        List<String> args = new ArrayList<>();
        args.add("validate");
        if (Objects.nonNull(target)) {
            args.add(target);
        }
        return invokeRoot(args);
    }

    /**
     * {@code opencli verify [target]}。
     *
     * @param target 可选过滤目标
     * @param smoke  是否运行 smoke 测试
     */
    public OpenCliResult verify(String target, boolean smoke) {
        List<String> args = new ArrayList<>();
        args.add("verify");
        if (Objects.nonNull(target)) {
            args.add(target);
        }
        if (smoke) {
            args.add("--smoke");
        }
        return invokeRoot(args);
    }

    /** @see #verify(String, boolean) */
    public OpenCliResult verify(String target) {
        return verify(target, false);
    }

    /**
     * {@code opencli doctor}。
     *
     * @param verbose 是否 {@code -v/--verbose}
     */
    public OpenCliResult doctor(boolean verbose) {
        List<String> args = new ArrayList<>();
        args.add("doctor");
        if (verbose) {
            args.add("-v");
            args.add("--verbose");
        }
        return invokeRoot(args);
    }

    /** @see #doctor(boolean) */
    public OpenCliResult doctor() {
        return doctor(false);
    }

    /**
     * {@code opencli completion <shell>}，输出 bash/zsh/fish 补全脚本。
     *
     * @param shell {@code bash}、{@code zsh} 或 {@code fish}
     */
    public OpenCliResult completion(String shell) {
        List<String> args = new ArrayList<>();
        args.add("completion");
        args.add(shell);
        return invokeRoot(args);
    }

    /**
     * {@code opencli convention-audit [target]}。
     */
    public OpenCliResult conventionAudit(String target, String site, String format, boolean strict) {
        List<String> args = new ArrayList<>();
        args.add("convention-audit");
        if (Objects.nonNull(target)) {
            args.add(target);
        }
        if (Objects.nonNull(site)) {
            OpenCliArgSupport.addOptionPair(args, "--site", site);
        }
        if (Objects.nonNull(format)) {
            args.add("-f");
            args.add(format);
        }
        if (strict) {
            args.add("--strict");
        }
        return invokeRoot(args);
    }

    /** @return 插件管理子门面 */
    public OpenCliPluginClient plugin() {
        return new OpenCliPluginClient(executor);
    }

    /** @return 适配器覆盖管理子门面 */
    public OpenCliAdapterMgmtClient adapter() {
        return new OpenCliAdapterMgmtClient(executor);
    }

    /** @return Browser Bridge Chrome profile 管理 */
    public OpenCliProfileClient profile() {
        return new OpenCliProfileClient(executor);
    }

    /** @return opencli 守护进程管理 */
    public OpenCliDaemonClient daemon() {
        return new OpenCliDaemonClient(executor);
    }

    /** @return 外部 CLI 透传注册 */
    public OpenCliExternalClient external() {
        return new OpenCliExternalClient(executor);
    }

    /** @return opencli 内置技能包管理（list / read） */
    public OpenCliSkillsClient skills() {
        return new OpenCliSkillsClient(executor);
    }

    /** @return 登录状态与会话刷新 */
    public OpenCliAuthClient auth() {
        return new OpenCliAuthClient(executor);
    }

    /**
     * Antigravity 子命令（{@code serve} 为长驻进程，调用会阻塞直至进程退出）。
     */
    public OpenCliAntigravityClient antigravity() {
        return new OpenCliAntigravityClient(executor);
    }
}
