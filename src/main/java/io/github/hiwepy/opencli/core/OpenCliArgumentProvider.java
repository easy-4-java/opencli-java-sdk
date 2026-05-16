package io.github.hiwepy.opencli.core;

import java.util.List;

/**
 * 将领域请求对象映射为 OpenCLI argv 片段（不含可执行文件名与 adapter 名）。
 */
public interface OpenCliArgumentProvider {

    /**
     * @return 命令参数列表，可为空；不得包含 null 或空白项（由调用方过滤）
     */
    List<String> toCliArgs();
}
