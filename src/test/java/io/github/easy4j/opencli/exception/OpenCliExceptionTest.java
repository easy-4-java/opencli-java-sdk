package io.github.easy4j.opencli.exception;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.core.OpenCliResult;
import org.junit.jupiter.api.Test;

class OpenCliExceptionTest {

    @Test
    void shouldCreateWithMessageAndCauseAndPartialResult() {
        OpenCliResult partial = OpenCliResult.builder().stdout("out").success(false).build();
        OpenCliException ex = new OpenCliException("msg", new RuntimeException("cause"), partial);
        assertEquals("msg", ex.getMessage());
        assertNotNull(ex.getCause());
        assertSame(partial, ex.getPartialResult());
    }

    @Test
    void shouldCreateWithMessageAndPartialResult() {
        OpenCliResult partial = OpenCliResult.builder().stdout("out").success(false).build();
        OpenCliException ex = new OpenCliException("msg", partial);
        assertEquals("msg", ex.getMessage());
        assertSame(partial, ex.getPartialResult());
    }

    @Test
    void shouldCreateExecutableFailureException() {
        OpenCliExecutableFailureException ex = new OpenCliExecutableFailureException("fail", new RuntimeException());
        assertEquals("fail", ex.getMessage());
        assertNull(ex.getPartialResult());
    }

    @Test
    void shouldCreateNonZeroExitException() {
        OpenCliResult partial = OpenCliResult.builder().stdout("").exitCode(1).success(false).build();
        OpenCliNonZeroExitException ex = new OpenCliNonZeroExitException("exit 1", partial);
        assertEquals("exit 1", ex.getMessage());
        assertSame(partial, ex.getPartialResult());
    }

    @Test
    void shouldCreateTimeoutException() {
        OpenCliResult partial = OpenCliResult.builder().stdout("").success(false).build();
        OpenCliTimeoutException ex = new OpenCliTimeoutException("timeout", partial);
        assertEquals("timeout", ex.getMessage());
        assertSame(partial, ex.getPartialResult());
    }

    @Test
    void shouldCreateStartupException() {
        io.github.easy4j.opencli.core.availability.OpenCliAvailabilityReport report =
            io.github.easy4j.opencli.core.availability.OpenCliAvailabilityReport.builder()
                .status(io.github.easy4j.opencli.core.availability.OpenCliAvailabilityStatus.EXECUTABLE_NOT_FOUND)
                .available(false)
                .message("not found")
                .build();
        OpenCliStartupException ex = new OpenCliStartupException("startup fail", report);
        assertEquals("startup fail", ex.getMessage());
        assertSame(report, ex.getAvailabilityReport());
    }

    @Test
    void shouldHandleNullReportInStartupException() {
        OpenCliStartupException ex = new OpenCliStartupException("fail", null);
        assertNull(ex.getAvailabilityReport());
        assertNull(ex.getPartialResult());
    }
}
