package io.github.easy4j.opencli.browser.result;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserResult.Status;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserSourceComparison.Metrics;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails;
import io.github.easy4j.opencli.core.OpenCliResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Decodes known OpenCLI 1.8.8 browser leaf shapes without interpreting metadata as instructions. */
public final class OpenCliBrowserDecoder {
    private static final ObjectReader JSON = new JsonMapper().reader().with(
        DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
        DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY,
        DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    private OpenCliBrowserDecoder() {
    }

    /** @param raw browser tab list execution result @return strict tab-list observation */
    public static OpenCliBrowserResult<List<OpenCliBrowserTab>> tabList(OpenCliResult raw) {
        return decode(raw, OpenCliBrowserDecoder::readTabs);
    }

    /** @param raw browser open execution result @return strict navigation observation */
    public static OpenCliBrowserResult<OpenCliBrowserOpenResult> open(OpenCliResult raw) {
        return decode(raw, OpenCliBrowserDecoder::readNavigation);
    }

    /** @param raw browser state result @return strict text snapshot without interpreting its contents */
    public static OpenCliBrowserResult<OpenCliBrowserSnapshot> snapshot(OpenCliResult raw) {
        Status failure = unavailableStatus(raw);
        if (Objects.nonNull(failure)) {
            return new OpenCliBrowserResult<>(raw, failure, null);
        }
        String output = raw.getStdout();
        String prefix = "URL: ";
        int headerEnd = output.indexOf('\n');
        int blankEnd = output.indexOf('\n', headerEnd + 1);
        if (!output.startsWith(prefix) || headerEnd < prefix.length() || blankEnd < 0
            || !output.substring(headerEnd + 1, blankEnd).chars().allMatch(ch -> ch == '\r')) {
            return new OpenCliBrowserResult<>(raw, Status.INVALID_SHAPE, null);
        }
        String url = output.substring(prefix.length(), headerEnd);
        if (url.endsWith("\r")) {
            url = url.substring(0, url.length() - 1);
        }
        String body = output.substring(blankEnd + 1);
        if (isBlankOutput(body)) {
            return new OpenCliBrowserResult<>(raw, Status.INVALID_SHAPE, null);
        }
        return new OpenCliBrowserResult<>(raw, Status.PARSED, new OpenCliBrowserSnapshot(url, body));
    }

    /** @param raw browser state --compare-sources result @return strict DOM and AX comparison */
    public static OpenCliBrowserResult<OpenCliBrowserSourceComparison> compareSources(OpenCliResult raw) {
        return decode(raw, OpenCliBrowserDecoder::readComparison);
    }

    private static Status unavailableStatus(OpenCliResult raw) {
        Objects.requireNonNull(raw, "raw");
        OpenCliExecutionDetails execution = raw.getExecutionDetails();
        if (Objects.nonNull(execution) && execution.isStdoutTruncated()) {
            return Status.TRUNCATED;
        }
        if (!raw.isSuccess()) {
            return Status.EXECUTION_FAILED;
        }
        return isBlankOutput(raw.getStdout()) ? Status.EMPTY : null;
    }

    private static boolean isBlankOutput(String output) {
        return Objects.isNull(output) || output.chars().allMatch(value -> " \t\r\n".indexOf(value) >= 0);
    }

    private static <T> OpenCliBrowserResult<T> decode(OpenCliResult raw, Function<JsonNode, T> model) {
        Status failure = unavailableStatus(raw);
        if (Objects.nonNull(failure)) {
            return new OpenCliBrowserResult<>(raw, failure, null);
        }
        JsonNode json;
        try {
            json = JSON.readTree(raw.getStdout());
        } catch (JacksonException ex) {
            return new OpenCliBrowserResult<>(raw, Status.INVALID_JSON, null);
        }
        try {
            return new OpenCliBrowserResult<>(raw, Status.PARSED, model.apply(json));
        } catch (IllegalArgumentException ex) {
            return new OpenCliBrowserResult<>(raw, Status.INVALID_SHAPE, null);
        }
    }

    private static List<OpenCliBrowserTab> readTabs(JsonNode json) {
        if (!json.isArray()) {
            throw invalidShape();
        }
        List<OpenCliBrowserTab> tabs = new ArrayList<>(json.size());
        for (JsonNode item : json) {
            JsonNode index = item.path("index");
            JsonNode active = item.path("active");
            if (!item.isObject() || !index.isIntegralNumber() || !index.canConvertToInt()
                || index.intValue() < 0 || !active.isBoolean()) {
                throw invalidShape();
            }
            tabs.add(new OpenCliBrowserTab(index.intValue(), pageId(item.path("page")), active.booleanValue(),
                optionalText(item.path("url")), optionalText(item.path("title")), item));
        }
        return Collections.unmodifiableList(tabs);
    }

    private static OpenCliBrowserOpenResult readNavigation(JsonNode json) {
        if (!json.isObject() || !json.path("url").isTextual()) {
            throw invalidShape();
        }
        JsonNode page = json.path("page");
        return new OpenCliBrowserOpenResult(json.path("url").textValue(),
            page.isMissingNode() ? null : pageId(page), json);
    }

    private static OpenCliBrowserSourceComparison readComparison(JsonNode json) {
        JsonNode url = json.path("url");
        JsonNode sources = json.path("sources");
        if (!url.isTextual() || !sources.isObject()) {
            throw invalidShape();
        }
        return new OpenCliBrowserSourceComparison(url.textValue(), readMetrics(sources.path("dom")),
            readMetrics(sources.path("ax")), json);
    }

    private static Metrics readMetrics(JsonNode json) {
        JsonNode ok = json.path("ok");
        if (!ok.isBoolean()) {
            throw invalidShape();
        }
        long elapsed = counter(json.path("elapsed_ms"));
        if (!ok.booleanValue()) {
            JsonNode error = json.path("error");
            JsonNode message = error.path("message");
            if (!message.isTextual()) {
                throw invalidShape();
            }
            return new Metrics(false, elapsed, null, null, null, null, null, null, null,
                optionalText(error.path("code")), message.textValue());
        }
        JsonNode interactive = json.path("interactive");
        return new Metrics(true, elapsed, counter(json.path("chars")), counter(json.path("bytes")),
            counter(json.path("lines")), counter(json.path("approx_tokens")), counter(json.path("refs")),
            counter(json.path("frame_sections")), interactive.isMissingNode() ? null : counter(interactive), null, null);
    }

    private static long counter(JsonNode node) {
        if (!node.isIntegralNumber() || !node.canConvertToLong() || node.longValue() < 0) {
            throw invalidShape();
        }
        return node.longValue();
    }

    private static String pageId(JsonNode node) {
        if (node.isTextual() && !node.textValue().isEmpty()) {
            return node.textValue();
        }
        if (node.isIntegralNumber()) {
            return node.bigIntegerValue().toString();
        }
        throw invalidShape();
    }

    private static String optionalText(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw invalidShape();
        }
        return node.textValue();
    }

    private static IllegalArgumentException invalidShape() {
        return new IllegalArgumentException("Unrecognized browser result shape");
    }
}
