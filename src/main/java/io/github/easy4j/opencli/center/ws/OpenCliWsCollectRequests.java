package io.github.easy4j.opencli.center.ws;

import com.fasterxml.jackson.databind.node.ObjectNode;
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

    static final class Key {
        private final Object center;
        private final String requestId;

        Key(Object center, String requestId) {
            this.center = center;
            this.requestId = requestId;
        }

        String requestId() {
            return requestId;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Key)) {
                return false;
            }
            Key key = (Key) other;
            return center.equals(key.center) && requestId.equals(key.requestId);
        }

        @Override
        public int hashCode() {
            return 31 * center.hashCode() + requestId.hashCode();
        }
    }

    static final class Admission {
        private final Status status;
        private final ObjectNode cachedResult;

        Admission(Status status, ObjectNode cachedResult) {
            this.status = status;
            this.cachedResult = cachedResult;
        }

        Status status() {
            return status;
        }

        ObjectNode cachedResult() {
            return cachedResult;
        }
    }

    private static final class Completed {
        private final byte[] digest;
        private final ObjectNode result;
        private final long completedAt;

        Completed(byte[] digest, ObjectNode result, long completedAt) {
            this.digest = digest;
            this.result = result;
            this.completedAt = completedAt;
        }
    }

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
            if (!Arrays.equals(previous.digest, digest)) {
                return new Admission(Status.CONFLICT, null);
            }
            return new Admission(Status.CACHED, previous.result.deepCopy().put("cached", true));
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
        while (entries.hasNext() && now - entries.next().completedAt >= ttlNanos) {
            entries.remove();
        }
    }
}
