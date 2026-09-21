package io.github.easy4j.opencli.core;

import io.github.easy4j.opencli.core.support.SubprocessExecutionSupport;
import java.io.IOException;

/**
 * Stable process-capacity owner. Pass the same instance to multiple executors to
 * share a limit deliberately; constructing an unrelated executor cannot replace it.
 */
public final class OpenCliProcessRuntime {
    private final SubprocessExecutionSupport.Runtime runtime;

    /** @param maxConcurrent positive limit, or zero for the CPU-derived default; negative is invalid */
    public OpenCliProcessRuntime(int maxConcurrent) {
        runtime = new SubprocessExecutionSupport.Runtime(maxConcurrent);
    }

    /** @return fixed capacity for this runtime */
    public int getMaxConcurrentExecutions() { return runtime.getMaxConcurrentExecutions(); }

    /** @return whether unconfirmed resource cleanup has quarantined this runtime */
    public boolean isQuarantined() { return runtime.isQuarantined(); }

    /**
     * Low-level bridge used by the SDK executor. Results retain bounded process evidence;
     * the executor maps terminal conditions to the existing SDK exception hierarchy.
     *
     * @param request immutable submission snapshot
     * @return terminal execution evidence
     * @throws IOException retained for compatibility with the low-level execution API
     * @throws InterruptedException retained for compatibility; observed interruption is normally returned as CANCELLED
     */
    public SubprocessExecutionSupport.RunSession execute(SubprocessExecutionSupport.ExecutionRequest request)
        throws IOException, InterruptedException {
        return runtime.execute(request);
    }
}
