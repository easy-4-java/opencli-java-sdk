import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliRemoteExecutionDetails;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import java.net.URI;
import java.util.Objects;

public final class OpenCliAdminLoopbackProbe {

    public static void main(String[] args) {
        URI endpoint = URI.create(args[0]);
        require("127.0.0.1".equals(endpoint.getHost()), "test Agent must be on IPv4 loopback");
        try (OpenCliExecutor executor = executor(endpoint, false)) {
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
        try (OpenCliExecutor executor = executor(endpoint, true)) {
            OpenCliResult help = executor.invoke("npm", "search", "fixture", "--help=true", "-f", "json");
            verify(help, true);
            require(Objects.nonNull(help.getRemoteRawHttpBody()) && help.getRemoteRawHttpBody().contains("opencli npm search"),
                "explicit capture did not retain the original Agent body");
        }
        System.out.println("PASS: real Agent help, missing-argument failure, and opt-in raw HTTP capture");
    }

    private static OpenCliExecutor executor(URI endpoint, boolean capture) {
        OpenCliProperties properties = new OpenCliProperties();
        properties.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        properties.setRemoteAgentBaseUrl(endpoint.toString());
        properties.setRemoteCollectMode("bridge");
        properties.setRemoteCaptureRawHttpResponse(capture);
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
