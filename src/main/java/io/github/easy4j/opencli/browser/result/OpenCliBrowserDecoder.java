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
import java.math.BigInteger;
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
        if (!output.startsWith(prefix) || headerEnd < prefix.length()) {
            return new OpenCliBrowserResult<>(raw, Status.INVALID_SHAPE, null);
        }
        int blankStart = headerEnd + 1;
        int bodyStart;
        if (output.startsWith("\r\n", blankStart)) {
            bodyStart = blankStart + 2;
        } else if (output.startsWith("\n", blankStart)) {
            bodyStart = blankStart + 1;
        } else {
            return new OpenCliBrowserResult<>(raw, Status.INVALID_SHAPE, null);
        }
        String url = output.substring(prefix.length(), headerEnd);
        if (url.endsWith("\r")) {
            url = url.substring(0, url.length() - 1);
        }
        String body = output.substring(bodyStart);
        if (url.chars().anyMatch(Character::isISOControl) || isBlankOutput(body)) {
            return new OpenCliBrowserResult<>(raw, Status.INVALID_SHAPE, null);
        }
        return new OpenCliBrowserResult<>(raw, Status.PARSED, new OpenCliBrowserSnapshot(url, body));
    }

    /** @param raw browser state --compare-sources result @return strict DOM and AX comparison */
    public static OpenCliBrowserResult<OpenCliBrowserSourceComparison> compareSources(OpenCliResult raw) {
        return decode(raw, OpenCliBrowserDecoder::readComparison);
    }

    /** @param raw browser find result @return strict selector matches */
    public static OpenCliBrowserResult<OpenCliBrowserFindResult> find(OpenCliResult raw) {
        return decode(raw, OpenCliBrowserDecoder::readFind);
    }

    /** @param raw one-shot browser console result @return strict console capture */
    public static OpenCliBrowserResult<OpenCliBrowserConsoleResult> console(OpenCliResult raw) {
        return decode(raw, OpenCliBrowserDecoder::readConsole);
    }

    /** @param raw one-shot browser network listing @return strict network capture */
    public static OpenCliBrowserResult<OpenCliBrowserNetworkResult> network(OpenCliResult raw) {
        return decode(raw, OpenCliBrowserDecoder::readNetwork);
    }

    /** @param raw browser analyze result @return strict analysis observation */
    public static OpenCliBrowserResult<OpenCliBrowserAnalysisResult> analyze(OpenCliResult raw) {
        return decode(raw, OpenCliBrowserDecoder::readAnalysis);
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

    private static OpenCliBrowserFindResult readFind(JsonNode json) {
        long total = counter(json.path("matches_n"));
        JsonNode reported = json.path("entries");
        if (total < reported.size()) {
            throw invalidShape();
        }
        List<OpenCliBrowserFindResult.Entry> entries = entries(reported, item -> {
            JsonNode ref = item.path("ref");
            JsonNode visible = item.path("visible");
            BigInteger numericRef = ref.isNull() ? null : unsignedInteger(ref);
            if (!visible.isBoolean() || !item.path("attrs").isObject()) {
                throw invalidShape();
            }
            return new OpenCliBrowserFindResult.Entry(counter(item.path("nth")), numericRef, text(item.path("tag")),
                text(item.path("role")), text(item.path("text")), visible.booleanValue(), item);
        });
        return new OpenCliBrowserFindResult(total, entries, json);
    }

    private static OpenCliBrowserConsoleResult readConsole(JsonNode json) {
        JsonNode reported = json.path("messages");
        validateCount(json, reported);
        List<OpenCliBrowserConsoleResult.Message> messages = entries(reported, item ->
            new OpenCliBrowserConsoleResult.Message(optionalText(item.path("type")), optionalText(item.path("text")),
                text(item.path("timestamp")), item));
        return new OpenCliBrowserConsoleResult(text(json.path("session")), text(json.path("captured_at")), messages, json);
    }

    private static OpenCliBrowserNetworkResult readNetwork(JsonNode json) {
        JsonNode reported = json.path("entries");
        validateCount(json, reported);
        List<OpenCliBrowserNetworkResult.Entry> entries = entries(reported, item -> {
            JsonNode status = item.path("status");
            if (!status.isIntegralNumber() || !status.canConvertToInt() || status.intValue() < 0) {
                throw invalidShape();
            }
            return new OpenCliBrowserNetworkResult.Entry(text(item.path("key")), text(item.path("method")),
                status.intValue(), text(item.path("url")), text(item.path("ct")), counter(item.path("size")),
                optionalText(item.path("timestamp")), item);
        });
        return new OpenCliBrowserNetworkResult(text(json.path("session")), text(json.path("captured_at")),
            counter(json.path("filtered_out")), entries, json);
    }

    private static OpenCliBrowserAnalysisResult readAnalysis(JsonNode json) {
        JsonNode pattern = json.path("pattern");
        if (!json.path("anti_bot").isObject() || !json.path("initial_state").isObject()
            || !json.path("api_candidates").isArray() || !json.path("recommended_next_step").isTextual()) {
            throw invalidShape();
        }
        return new OpenCliBrowserAnalysisResult(text(json.path("requested_url")), text(json.path("final_url")),
            text(json.path("title")), text(pattern.path("pattern")), json);
    }

    private static <T> List<T> entries(JsonNode array, Function<JsonNode, T> entry) {
        if (!array.isArray()) {
            throw invalidShape();
        }
        List<T> result = new ArrayList<>(array.size());
        for (JsonNode item : array) {
            if (!item.isObject()) {
                throw invalidShape();
            }
            result.add(entry.apply(item));
        }
        return Collections.unmodifiableList(result);
    }

    private static void validateCount(JsonNode json, JsonNode entries) {
        if (counter(json.path("count")) != entries.size()) {
            throw invalidShape();
        }
    }

    private static BigInteger unsignedInteger(JsonNode node) {
        if (!node.isIntegralNumber() || node.bigIntegerValue().signum() < 0) {
            throw invalidShape();
        }
        return node.bigIntegerValue();
    }

    private static String text(JsonNode node) {
        if (!node.isTextual()) {
            throw invalidShape();
        }
        return node.textValue();
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
