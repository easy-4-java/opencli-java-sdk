package io.github.easy4j.opencli.core;

import java.util.concurrent.atomic.AtomicBoolean;

/** Cooperative cancellation for one local invocation; cancellation never affects another invocation. */
public final class OpenCliCancellationToken {
    private final AtomicBoolean cancelled = new AtomicBoolean();

    /** Request cancellation. This operation is idempotent. */
    public void cancel() { cancelled.set(true); }

    /** @return whether cancellation has been requested */
    public boolean isCancelled() { return cancelled.get(); }
}
