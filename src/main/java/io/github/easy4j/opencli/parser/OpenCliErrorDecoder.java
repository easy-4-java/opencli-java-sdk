package io.github.easy4j.opencli.parser;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;
import io.github.easy4j.opencli.parser.OpenCliErrorEnvelope.Format;
import io.github.easy4j.opencli.parser.OpenCliErrorEnvelope.Status;
import io.github.easy4j.opencli.util.OpenCliStrings;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Decodes local CLI failure metadata without changing process status or interpreting advice. */
public final class OpenCliErrorDecoder {
    private static final int MAX_BYTES = 256 * 1024;
    private static final int MAX_DEPTH = 32;
    private static final ObjectReader JSON = new JsonMapper(JsonFactory.builder()
        .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(MAX_DEPTH).build()).build())
        .readerFor(Object.class)
        .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    private OpenCliErrorDecoder() {
    }

    /**
     * @param stderr captured error text
     * @param observedExit observed local process exit code, or null
     * @param incomplete true if transport or lifecycle prevented a complete local observation
     * @return decoded metadata or a status describing why it is unavailable
     */
    public static OpenCliErrorEnvelope decode(String stderr, Integer observedExit, boolean incomplete) {
        OpenCliErrorEnvelope.OpenCliErrorEnvelopeBuilder result =
            OpenCliErrorEnvelope.builder().observedProcessExit(observedExit);
        if (incomplete) {
            return result.status(Status.INCOMPLETE).build();
        }
        if (Objects.isNull(stderr) || stderr.isEmpty()) {
            return result.status(Status.EMPTY).build();
        }
        if (stderr.length() > MAX_BYTES || stderr.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return result.status(Status.OVER_BUDGET).build();
        }
        int first = 0;
        while (first < stderr.length() && " \t\r\n".indexOf(stderr.charAt(first)) >= 0) {
            first++;
        }
        if (first == stderr.length()) {
            return result.status(Status.EMPTY).build();
        }
        boolean json = stderr.charAt(first) == '{' || stderr.charAt(first) == '[';
        result.format(json ? Format.JSON : Format.YAML);
        try {
            Object decoded = json ? JSON.readValue(stderr) : OpenCliErrorYaml.read(stderr, MAX_DEPTH, MAX_BYTES);
            if (!(decoded instanceof Map)) {
                return result.status(Status.NO_ENVELOPE).build();
            }
            Map<String, Object> root = immutableMap((Map<?, ?>) decoded);
            if (!Boolean.FALSE.equals(root.get("ok")) || !(root.get("error") instanceof Map)) {
                return result.status(Status.NO_ENVELOPE).build();
            }
            Map<String, Object> error = asMap(root.get("error"));
            Object code = error.get("code");
            Object message = error.get("message");
            Object help = error.get("help");
            Object exit = error.get("exitCode");
            Object trace = root.get("trace");
            if (!(code instanceof String) || OpenCliStrings.isBlank((String) code)
                || !(message instanceof String) || !(exit instanceof Integer)
                || (Objects.nonNull(help) && !(help instanceof String))
                || (Objects.nonNull(trace) && !(trace instanceof Map))) {
                return result.status(Status.INVALID).build();
            }
            return result.status(Status.DECODED).code((String) code).message((String) message)
                .help((String) help).reportedExit((Integer) exit).envelopeFields(root).errorFields(error)
                .trace(Objects.isNull(trace) ? Collections.emptyMap() : asMap(trace)).build();
        } catch (RuntimeException ex) {
            return result.status(Status.INVALID).build();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static Map<String, Object> immutableMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException("Envelope keys must be strings");
            }
            result.put((String) key, immutableValue(value));
        });
        return Collections.unmodifiableMap(result);
    }

    private static Object immutableValue(Object value) {
        if (value instanceof Map) {
            return immutableMap((Map<?, ?>) value);
        }
        if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            ((List<?>) value).forEach(item -> result.add(immutableValue(item)));
            return Collections.unmodifiableList(result);
        }
        return value;
    }
}
