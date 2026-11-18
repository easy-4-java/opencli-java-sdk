package io.github.easy4j.opencli.center.ws;

import tools.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Tracks accepted requests and bounded, instance-local replay results. */
final class OpenCliWsCollectRequests {

    enum Status {
        ACCEPTED, IN_FLIGHT, CONFLICT, CACHED
    }

    record Key(Object center, String requestId) { }

    record Admission(Status status, ObjectNode cachedResult) { }

    private record Completed(byte[] digest, ObjectNode result, long completedAt) { }

    private final Map<Key, byte[]> inFlight = new HashMap<>();
    private final Map<Key, Completed> completed = new LinkedHashMap<>();
    private final int maxCompleted;
    private final long ttlNanos;
    private final LongSupplier clock;

    OpenCliWsCollectRequests(int maxCompleted, long ttlNanos, LongSupplier clock) {
        this.maxCompleted = maxCompleted;
        this.ttlNanos = ttlNanos;
        this.clock = clock;
    }

    synchronized Admission reserve(Key key, byte[] digest) {
        byte[] active = inFlight.get(key);
        if (Objects.nonNull(active)) {
            return new Admission(Arrays.equals(active, digest) ? Status.IN_FLIGHT : Status.CONFLICT, null);
        }
        expire(clock.getAsLong());
        Completed previous = completed.get(key);
        if (Objects.nonNull(previous)) {
            if (!Arrays.equals(previous.digest(), digest)) {
                return new Admission(Status.CONFLICT, null);
            }
            return new Admission(Status.CACHED, previous.result().deepCopy().put("cached", true));
        }
        inFlight.put(key, digest);
        return new Admission(Status.ACCEPTED, null);
    }

    synchronized void complete(Key key, ObjectNode result) {
        byte[] digest = inFlight.remove(key);
        if (Objects.isNull(digest) || Objects.isNull(result)) {
            return;
        }
        long now = clock.getAsLong();
        expire(now);
        if (completed.size() >= maxCompleted) {
            Iterator<Key> entries = completed.keySet().iterator();
            entries.next();
            entries.remove();
        }
        completed.put(key, new Completed(digest, result, now));
    }

    synchronized void clear() {
        inFlight.clear();
        completed.clear();
    }

    private void expire(long now) {
        Iterator<Completed> entries = completed.values().iterator();
        while (entries.hasNext() && now - entries.next().completedAt() >= ttlNanos) {
            entries.remove();
        }
    }
}
