package io.github.easy4j.opencli.browser.result;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserResult.Status;
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

    private static <T> OpenCliBrowserResult<T> decode(OpenCliResult raw, Function<JsonNode, T> model) {
        Objects.requireNonNull(raw, "raw");
        OpenCliExecutionDetails execution = raw.getExecutionDetails();
        if (Objects.nonNull(execution) && execution.isStdoutTruncated()) {
            return new OpenCliBrowserResult<>(raw, Status.TRUNCATED, null);
        }
        if (!raw.isSuccess()) {
            return new OpenCliBrowserResult<>(raw, Status.EXECUTION_FAILED, null);
        }
        String stdout = raw.getStdout();
        if (Objects.isNull(stdout) || stdout.chars().allMatch(value -> " \t\r\n".indexOf(value) >= 0)) {
            return new OpenCliBrowserResult<>(raw, Status.EMPTY, null);
        }
        JsonNode json;
        try {
            json = JSON.readTree(stdout);
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
