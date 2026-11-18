import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.OpenCliRemoteAuthentication;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliRemoteExecutionDetails;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.exception.OpenCliExecutableFailureException;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public final class OpenCliAdminLoopbackProbe {

    public static void main(String[] args) throws IOException {
        URI endpoint = URI.create(args[0]);
        require("127.0.0.1".equals(endpoint.getHost()), "test Agent must be on IPv4 loopback");
        if (args.length == 2) {
            require("https".equals(endpoint.getScheme()), "TLS proxy requires HTTPS");
            String token = new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.US_ASCII).trim();
            verifyTlsProxy(endpoint, token);
            return;
        }
        try (OpenCliExecutor executor = executor(endpoint, false, null)) {
            OpenCliResult help = executor.invoke("npm", "search", "fixture", "--help=true", "-f", "json");
            verify(help, true);
            require(help.getStdout().contains("opencli npm search"), "structured CLI help is missing");
            require(Objects.isNull(help.getRemoteRawHttpBody()), "raw capture must be off by default");

            try {
                executor.invoke("npm", "search", "-f", "json");
                throw new AssertionError("missing CLI query unexpectedly succeeded");
            } catch (OpenCliNonZeroExitException failure) {
                OpenCliResult result = failure.getPartialResult();
                verify(result, false);
                require(Objects.isNull(result.getRemoteRawHttpBody()), "failed response was captured by default");
                require(result.getStderr().contains("opencli exit"), "the Agent's business error is missing");
            }
        }
        try (OpenCliExecutor executor = executor(endpoint, true, null)) {
            OpenCliResult help = executor.invoke("npm", "search", "fixture", "--help=true", "-f", "json");
            verify(help, true);
            require(Objects.nonNull(help.getRemoteRawHttpBody()) && help.getRemoteRawHttpBody().contains("opencli npm search"),
                "explicit capture did not retain the original Agent body");
        }
        System.out.println("PASS: real Agent help, missing-argument failure, and opt-in raw HTTP capture");
    }

    private static void verifyTlsProxy(URI endpoint, String token) {
        try (OpenCliExecutor executor = executor(endpoint, false, "Bearer " + token)) {
            OpenCliResult help = executor.invoke("npm", "search", "fixture", "--help=true", "-f", "json");
            verify(help, true);
            require(help.getStdout().contains("opencli npm search"), "structured CLI help is missing");
            require(Objects.isNull(help.getRemoteRawHttpBody()), "raw capture must be off by default");
        }
        verifyRejected(endpoint, null, 401);
        verifyRejected(endpoint, "Bearer invalid-" + token, 401);
        verifyRejected(URI.create("https://localhost:" + endpoint.getPort()), "Bearer " + token, 0);
        System.out.println("PASS: real Agent through TLS, missing and invalid credentials, and certificate hostname rejection");
    }

    private static void verifyRejected(URI endpoint, String credential, int status) {
        try (OpenCliExecutor executor = executor(endpoint, false, credential)) {
            executor.invoke("npm", "search", "fixture", "--help=true", "-f", "json");
            throw new AssertionError("proxy unexpectedly accepted the request");
        } catch (OpenCliExecutableFailureException failure) {
            OpenCliResult result = failure.getPartialResult();
            OpenCliRemoteExecutionDetails details = result.getRemoteExecutionDetails();
            require(details.getObservedHttpStatus().orElse(0) == status, "wrong observed HTTP status");
            require(details.getOutcome() == (status == 0 ? OpenCliRemoteExecutionDetails.Outcome.TRANSPORT_FAILURE
                : OpenCliRemoteExecutionDetails.Outcome.HTTP_FAILURE), "wrong rejection classification");
            require(Objects.isNull(result.getExitCode()), "rejection must not invent an exit code");
            require(Objects.isNull(result.getRemoteRawHttpBody()), "raw capture must be off by default");
            if (Objects.nonNull(credential)) {
                StringWriter diagnostics = new StringWriter();
                failure.printStackTrace(new PrintWriter(diagnostics));
                require(!diagnostics.toString().contains(credential), "exception included the authorization value");
            }
        }
    }

    private static OpenCliExecutor executor(URI endpoint, boolean capture, String credential) {
        OpenCliProperties properties = new OpenCliProperties();
        properties.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        properties.setRemoteAgentBaseUrl(endpoint.toString());
        properties.setRemoteCollectMode("bridge");
        properties.setRemoteCaptureRawHttpResponse(capture);
        if (Objects.nonNull(credential)) {
            properties.setRemoteAuthentication(new OpenCliRemoteAuthentication("Authorization", credential));
        }
        return new OpenCliExecutor(properties);
    }

    private static void verify(OpenCliResult result, boolean success) {
        require(result.isSuccess() == success, "wrong business status");
        require(Integer.valueOf(success ? 0 : 1).equals(result.getExitCode()), "wrong mapped exit value");
        OpenCliRemoteExecutionDetails details = result.getRemoteExecutionDetails();
        require(details.getObservedHttpStatus().orElse(-1) == 200, "HTTP status was lost");
        require(details.getOutcome() == (success ? OpenCliRemoteExecutionDetails.Outcome.SUCCESS
            : OpenCliRemoteExecutionDetails.Outcome.COMMAND_FAILURE), "wrong outcome classification");
        require(details.getExitCodeSource() == OpenCliRemoteExecutionDetails.ExitCodeSource.SYNTHETIC_COMPATIBILITY,
            "legacy mapping must be marked synthetic");
        require(!details.getObservedRemoteExitCode().isPresent(), "legacy response contains no CLI exit field");
        require(Objects.isNull(result.getExecutionDetails()), "legacy response contains no local process evidence");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
