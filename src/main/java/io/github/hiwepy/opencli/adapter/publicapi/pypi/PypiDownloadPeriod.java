package io.github.hiwepy.opencli.adapter.publicapi.pypi;

/**
 * PyPI 下载统计周期（文档枚举）。
 */
public enum PypiDownloadPeriod {

    RECENT("recent"),
    OVERALL("overall");

    private final String cliValue;

    PypiDownloadPeriod(String cliValue) {
        this.cliValue = cliValue;
    }

    /**
     * @return OpenCLI {@code --period} 值
     */
    public String getCliValue() {
        return cliValue;
    }
}
