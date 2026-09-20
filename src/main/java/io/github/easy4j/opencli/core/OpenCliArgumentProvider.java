package io.github.easy4j.opencli.core;

import java.util.List;

/**
 * Maps a domain request object to OpenCLI argv fragments (excluding the executable name
 * and adapter name).
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */public interface OpenCliArgumentProvider {

    /**
     * @return 命令参数列表，可为空；不得包含 null 或空白项（由调用方过滤）
     */
    List<String> toCliArgs();
}
