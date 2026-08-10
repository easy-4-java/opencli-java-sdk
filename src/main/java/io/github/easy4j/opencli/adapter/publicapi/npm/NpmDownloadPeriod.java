package io.github.easy4j.opencli.adapter.publicapi.npm;

/**
 * npm registry download statistics period enumeration (documented enum values plus custom date ranges).
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */public enum NpmDownloadPeriod {

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
