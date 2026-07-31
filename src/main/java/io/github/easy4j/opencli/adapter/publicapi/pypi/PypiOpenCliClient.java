package io.github.easy4j.opencli.adapter.publicapi.pypi;

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
import lombok.RequiredArgsConstructor;

/**
 * OpenCLI {@code pypi} 公共 registry 适配器（package 元数据与 pypistats 下载统计）。
 */
@RequiredArgsConstructor
public final class PypiOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.PYPI);
    }

    /**
     * {@code pypi package <name>}。
     *
     * @param packageName PyPI 包名
     * @param json        是否 {@code -f json}
     * @param more        透传参数
     */
    public OpenCliResult packageInfo(String packageName, boolean json, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("package");
        args.add(packageName);
        if (json) {
            args.add("-f");
            args.add("json");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /**
     * {@code pypi downloads <name>}，使用命名周期。
     *
     * @param period null 时 CLI 默认 recent
     */
    public OpenCliResult downloads(String packageName, PypiDownloadPeriod period, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("downloads");
        args.add(packageName);
        if (period != null) {
            OpenCliArgSupport.addOptionPair(args, "--period", period.getCliValue());
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /**
     * {@code pypi downloads <name> --period <value>}，自定义 period 字符串。
     */
    public OpenCliResult downloads(String packageName, String period, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("downloads");
        args.add(packageName);
        if (period != null) {
            OpenCliArgSupport.addOptionPair(args, "--period", period);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** {@code pypi package -f json} 解析 stdout。 */
    public OpenCliTypedResult<JsonNode> packageInfoTyped(String packageName, List<String> more) {
        return OpenCliStdoutJson.typed(packageInfo(packageName, true, more));
    }

    /** {@code pypi downloads -f json}。 */
    public OpenCliTypedResult<JsonNode> downloadsTyped(String packageName, PypiDownloadPeriod period, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("downloads");
        args.add(packageName);
        if (period != null) {
            OpenCliArgSupport.addOptionPair(args, "--period", period.getCliValue());
        }
        args.add("-f");
        args.add("json");
        return OpenCliStdoutJson.typed(ch().invoke(OpenCliArgSupport.merge(args, more)));
    }
}
