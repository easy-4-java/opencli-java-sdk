package io.github.easy4j.opencli.contract;

import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliAdapterChannel;
import io.github.easy4j.opencli.core.OpenCliAdapterCommandRequest;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Public binary/API contract: missing methods fail assertions rather than preventing the RED build. */
class OpenCliStructuredArgvContractTest {
    private static Class<?> type(String simpleName) {
        return assertDoesNotThrow(() -> Class.forName("io.github.easy4j.opencli.core." + simpleName),
            "required ordered-option API is not implemented");
    }

    private static Object call(Class<?> owner, Object receiver, String name, Class<?>[] parameterTypes, Object... args) {
        Method method = assertDoesNotThrow(() -> owner.getMethod(name, parameterTypes),
            "required public method is not implemented: " + name);
        try {
            return method.invoke(receiver, args);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new AssertionError("unexpected checked failure", cause);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("public API is inaccessible", ex);
        }
    }

    private static Object schema(String form, String name, boolean repeatable) {
        Class<?> owner = type("OpenCliOptionSchema");
        return "value".equals(form)
            ? call(owner, null, form, new Class<?>[]{String.class, boolean.class}, name, repeatable)
            : call(owner, null, form, new Class<?>[]{String.class}, name);
    }

    private static Object value(Object definition, Object value) {
        return call(type("OpenCliOption"), null, "value",
            new Class<?>[]{type("OpenCliOptionSchema"), Object.class}, definition, value);
    }

    private static Object flag(String form, Object definition) {
        return call(type("OpenCliOption"), null, form,
            new Class<?>[]{type("OpenCliOptionSchema")}, definition);
    }

    private static void add(Object builder, Object occurrence) {
        call(builder.getClass(), builder, "option", new Class<?>[]{type("OpenCliOption")}, occurrence);
    }

    private static List<String> run(OpenCliAdapterCommandRequest request) {
        OpenCliProperties p = new OpenCliProperties();
        String exe = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        p.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), exe).getAbsolutePath());
        p.setLeadingArguments(new ArrayList<>(Arrays.asList("-cp",
            System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
            ContractProbe.class.getName())));
        p.setCommandTimeoutMillis(10000L);
        OpenCliResult result = new OpenCliAdapterChannel(new OpenCliExecutor(p), "demo").invoke(request);
        assertTrue(result.isSuccess());
        String[] lines = result.getStdout().split("\r?\n");
        List<String> actual = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            assertTrue(lines[i].startsWith("arg:"));
            actual.add(new String(Base64.getDecoder().decode(lines[i].substring(4)), StandardCharsets.UTF_8));
        }
        assertEquals("argc:" + actual.size(), lines[0]);
        return actual;
    }

    @Test
    void orderedRepeatedValuesAndExplicitFalseReachChild() {
        Object tags = schema("value", "--tag", true);
        Object enabled = schema("value", "--enabled", false);
        OpenCliAdapterCommandRequest.OpenCliAdapterCommandRequestBuilder b = OpenCliAdapterCommandRequest.builder().subcommand("echo");
        add(b, value(tags, " A "));
        add(b, value(enabled, false));
        add(b, value(tags, ""));
        assertEquals(Arrays.asList("demo", "echo", "--tag", " A ", "--enabled", "false", "--tag", ""), run(b.build()));
    }

    @Test
    void explicitNegationIsDifferentFromAbsenceAndPresence() {
        Object cache = schema("negatableFlag", "--cache", false);
        OpenCliAdapterCommandRequest.OpenCliAdapterCommandRequestBuilder b = OpenCliAdapterCommandRequest.builder().subcommand("echo");
        add(b, flag("negated", cache));
        assertEquals(Arrays.asList("demo", "echo", "--no-cache"), run(b.build()));
        OpenCliAdapterCommandRequest.OpenCliAdapterCommandRequestBuilder present = OpenCliAdapterCommandRequest.builder().subcommand("echo");
        add(present, flag("present", cache));
        assertEquals(Arrays.asList("demo", "echo", "--cache"), run(present.build()));
        assertEquals(Arrays.asList("demo", "echo"), run(OpenCliAdapterCommandRequest.builder().subcommand("echo").build()));
    }

    @Test
    void flagSchemaCannotSilentlyConsumeAValue() {
        Object verbose = schema("flag", "--verbose", false);
        assertThrows(IllegalArgumentException.class, () -> value(verbose, false));
        assertThrows(IllegalArgumentException.class, () -> flag("negated", verbose));
    }

    @Test
    void nonrepeatableOptionCannotAppearTwice() {
        Object once = schema("value", "--limit", false);
        OpenCliAdapterCommandRequest.OpenCliAdapterCommandRequestBuilder b = OpenCliAdapterCommandRequest.builder().subcommand("echo");
        add(b, value(once, 1));
        add(b, value(once, 2));
        assertThrows(IllegalArgumentException.class, () -> b.build().toSubcommandAndArgs());
    }

    @Test
    void legacyAndOrderedOptionsCannotCollide() {
        Object limit = schema("value", "--limit", true);
        OpenCliAdapterCommandRequest.OpenCliAdapterCommandRequestBuilder b = OpenCliAdapterCommandRequest.builder().subcommand("echo")
            .options(Collections.<String, Object>singletonMap("limit", "1"));
        add(b, value(limit, "2"));
        assertThrows(IllegalArgumentException.class, () -> b.build().toSubcommandAndArgs());
    }

    @Test
    void mutableOccurrenceValueIsCapturedWhenCreated() {
        StringBuilder text = new StringBuilder(" before ");
        Object occurrence = value(schema("value", "--text", false), text);
        text.append("after");
        OpenCliAdapterCommandRequest.OpenCliAdapterCommandRequestBuilder b = OpenCliAdapterCommandRequest.builder().subcommand("echo");
        add(b, occurrence);
        assertEquals(Arrays.asList("demo", "echo", "--text", " before "), run(b.build()));
    }

    @Test
    void malformedSchemaIdentifiersAreRejected() {
        Class<?> owner = type("OpenCliOptionSchema");
        assertThrows(IllegalArgumentException.class,
            () -> call(owner, null, "flag", new Class<?>[]{String.class}, "--x y"));
        assertThrows(IllegalArgumentException.class,
            () -> call(owner, null, "flag", new Class<?>[]{String.class}, ""));
    }

    @Test
    void legacyMapIsCapturedRatherThanAliased() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("text", " before ");
        OpenCliAdapterCommandRequest request = OpenCliAdapterCommandRequest.builder().subcommand("echo").options(options).build();
        options.put("text", "after");
        assertEquals(Arrays.asList("demo", "echo", "--text", " before "), run(request));
    }

    @Test
    void mutableLegacyValueCannotChangeAnExistingRequest() {
        StringBuilder text = new StringBuilder(" before ");
        OpenCliAdapterCommandRequest request = OpenCliAdapterCommandRequest.builder().subcommand("echo")
            .options(Collections.<String, Object>singletonMap("text", text)).build();
        text.append("after");
        assertEquals(Arrays.asList("demo", "echo", "--text", " before "), run(request));
    }

    @Test
    void reusingBuilderDoesNotMutatePreviousRequest() {
        Object tags = schema("value", "--tag", true);
        OpenCliAdapterCommandRequest.OpenCliAdapterCommandRequestBuilder b = OpenCliAdapterCommandRequest.builder().subcommand("echo");
        add(b, value(tags, "A"));
        OpenCliAdapterCommandRequest first = b.build();
        add(b, value(tags, "B"));
        assertEquals(Arrays.asList("demo", "echo", "--tag", "A"), run(first));
    }
}
