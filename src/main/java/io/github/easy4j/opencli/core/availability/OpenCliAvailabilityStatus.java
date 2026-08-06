package io.github.easy4j.opencli.core.availability;

/**
 * OpenCLI 可用性探测结论分类。
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 1.0.0
 */
public enum OpenCliAvailabilityStatus {

    /** {@code opencli list} 探测成功。 */
    AVAILABLE,

    /** 远程 Agent 模式，跳过本机 CLI 探测。 */
    SKIPPED_REMOTE_MODE,

    /** 未配置可执行文件。 */
    EXECUTABLE_NOT_CONFIGURED,

    /** 路径不存在或 PATH 中找不到。 */
    EXECUTABLE_NOT_FOUND,

    /** 存在但不可执行。 */
    EXECUTABLE_NOT_EXECUTABLE,

    /** 进程无法启动。 */
    SPAWN_FAILED,

    /** 非零退出。 */
    NON_ZERO_EXIT,

    /** 探测超时。 */
    TIMEOUT,

    /** 其它失败。 */
    FAILED
}
