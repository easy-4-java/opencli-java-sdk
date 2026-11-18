package io.github.easy4j.opencli.center.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static io.github.easy4j.opencli.center.ws.OpenCliWsCollectRequests.Status.ACCEPTED;
import static io.github.easy4j.opencli.center.ws.OpenCliWsCollectRequests.Status.CACHED;
import static io.github.easy4j.opencli.center.ws.OpenCliWsCollectRequests.Status.CONFLICT;
import static io.github.easy4j.opencli.center.ws.OpenCliWsCollectRequests.Status.IN_FLIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenCliWsCollectRequestsTest {

    @Test
    void evictsAndExpiresCompletedResultsWithoutDroppingActiveReservations() {
        AtomicLong clock = new AtomicLong();
        OpenCliWsCollectRequests requests = new OpenCliWsCollectRequests(1, 100, clock::get);
        OpenCliWsCollectRequests.Key active = new OpenCliWsCollectRequests.Key("center", "active");
        OpenCliWsCollectRequests.Key first = new OpenCliWsCollectRequests.Key("center", "first");
        OpenCliWsCollectRequests.Key second = new OpenCliWsCollectRequests.Key("center", "second");
        byte[] digest = {1};
        ObjectNode result = new ObjectMapper().createObjectNode().put("success", true);

        assertEquals(ACCEPTED, requests.reserve(active, digest).status());
        assertEquals(ACCEPTED, requests.reserve(first, digest).status());
        requests.complete(first, result);
        assertEquals(ACCEPTED, requests.reserve(second, digest).status());
        requests.complete(second, result);

        assertEquals(IN_FLIGHT, requests.reserve(active, digest).status());
        assertEquals(CONFLICT, requests.reserve(active, new byte[] {2}).status());
        assertEquals(ACCEPTED, requests.reserve(first, digest).status());
        assertEquals(CACHED, requests.reserve(second, digest).status());
        assertTrue(requests.reserve(second, digest).cachedResult().path("cached").asBoolean());
        assertFalse(result.has("cached"));

        clock.set(100);
        assertEquals(ACCEPTED, requests.reserve(second, digest).status());
        assertEquals(IN_FLIGHT, requests.reserve(active, digest).status());
    }
}
