package io.github.easy4j.opencli.adapter.publicapi.pypi;

/**
 * PyPI download statistics period enumeration.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */public enum PypiDownloadPeriod {

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
