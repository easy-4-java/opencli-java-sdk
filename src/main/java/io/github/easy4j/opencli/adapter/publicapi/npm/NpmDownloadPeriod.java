package io.github.easy4j.opencli.adapter.publicapi.npm;

/**
 * npm registry 下载统计周期（文档枚举 + 自定义日期区间）。
 */
public enum NpmDownloadPeriod {

    LAST_DAY("last-day"),
    LAST_WEEK("last-week"),
    LAST_MONTH("last-month"),
    LAST_YEAR("last-year");

    private final String cliValue;

    NpmDownloadPeriod(String cliValue) {
        this.cliValue = cliValue;
    }

    /**
     * @return OpenCLI {@code --period} 值
     */
    public String getCliValue() {
        return cliValue;
    }
}
