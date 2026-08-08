package io.github.easy4j.opencli.center.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.opencli.OpenCliProperties;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 反射测试：覆盖 {@link OpenCliWsReverseAgentClient} 私有工具方法，避免启动真实 WS。
 */
class OpenCliWsReverseAgentClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static OpenCliWsAgentConnectionProperties validProps() {
        OpenCliWsAgentConnectionProperties p = new OpenCliWsAgentConnectionProperties();
        p.setCentralApiBaseUrl("http://center:8031");
        p.setAgentAdvertiseUrl("http://node:19823");
        p.setWebSocketPath(OpenCliCenterWebSocketPath.NODES_WS);
        p.setMode("cdp");
        p.setNodeType("shell");
        p.setLabel("test");
        p.setHandshakeTimeoutMillis(1000L);
        return p;
    }

    private static Method method(String name, Class<?>... args) throws NoSuchMethodException {
        Method m = OpenCliWsReverseAgentClient.class.getDeclaredMethod(name, args);
        m.setAccessible(true);
        return m;
    }

    @Test
    void validateConnectionPropertiesAllBranches() throws Exception {
        Method validate = method("validateConnectionProperties");
        // centralApiBaseUrl blank
        OpenCliWsAgentConnectionProperties pBlankCentral = validProps();
        pBlankCentral.setCentralApiBaseUrl(" ");
        assertInvocationThrows(pBlankCentral, validate);

        // agentAdvertiseUrl blank
        OpenCliWsAgentConnectionProperties pBlankAdvertise = validProps();
        pBlankAdvertise.setAgentAdvertiseUrl("");
        assertInvocationThrows(pBlankAdvertise, validate);

        // agentAdvertiseUrl not http(s)
        OpenCliWsAgentConnectionProperties pBadScheme = validProps();
        pBadScheme.setAgentAdvertiseUrl("tcp://foo");
        assertInvocationThrows(pBadScheme, validate);

        // mode invalid
        OpenCliWsAgentConnectionProperties pBadMode = validProps();
        pBadMode.setMode("foo");
        assertInvocationThrows(pBadMode, validate);

        // NODES_WS + nodeType invalid
        OpenCliWsAgentConnectionProperties pBadNodeType = validProps();
        pBadNodeType.setNodeType("kubernetes");
        assertInvocationThrows(pBadNodeType, validate);

        // happy path
        validate.invoke(newClient(validProps()));
    }

    private static void assertInvocationThrows(OpenCliWsAgentConnectionProperties p, Method m) throws Exception {
        OpenCliWsReverseAgentClient client = new OpenCliWsReverseAgentClient(new OpenCliProperties(), p);
        try {
            m.invoke(client);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            assertTrue(ex.getCause() instanceof IllegalStateException);
            return;
        }
        org.junit.jupiter.api.Assertions.fail("expected IllegalStateException");
    }

    @Test
    void buildRegisterJsonDefaultsAndCustom() throws Exception {
        OpenCliWsAgentConnectionProperties p = validProps();
        p.setMode("");
        p.setNodeType("");
        p.setLabel(null);
        OpenCliWsReverseAgentClient c = newClient(p);
        String json = (String) method("buildRegisterJson").invoke(c);
        JsonNode tree = MAPPER.readTree(json);
        assertEquals("register", tree.get("type").asText());
        assertEquals("cdp", tree.get("mode").asText());
        assertEquals("shell", tree.get("node_type").asText());
        assertEquals("http://node:19823", tree.get("agent_url").asText());

        p = validProps();
        p.setMode("bridge");
        p.setNodeType("docker");
        p.setLabel("hello");
        c = newClient(p);
        json = (String) method("buildRegisterJson").invoke(c);
        tree = MAPPER.readTree(json);
        assertEquals("bridge", tree.get("mode").asText());
        assertEquals("docker", tree.get("node_type").asText());
        assertEquals("hello", tree.get("label").asText());
    }

    @Test
    void readArgsMapReadsAllValueTypes() throws Exception {
        String payload = "{\"args\":{\"a\":true,\"b\":1,\"c\":2,\"d\":1.5,\"e\":\"x\",\"f\":[1],\"g\":null}}";
        JsonNode root = MAPPER.readTree(payload);
        Map<String, Object> m = (Map<String, Object>) method("readArgsMap", JsonNode.class)
            .invoke(newClient(validProps()), root.get("args"));
        assertEquals(Boolean.TRUE, m.get("a"));
        assertEquals(1, m.get("b"));
        assertEquals(Integer.valueOf(2), m.get("c"));
        assertEquals(1.5d, m.get("d"));
        assertEquals("x", m.get("e"));
        // 非标量走 toString
        assertNotNull(m.get("f"));
        assertEquals(null, m.get("g"));

        // 非对象输入返回空 map
        Map<String, Object> empty = (Map<String, Object>) method("readArgsMap", JsonNode.class)
            .invoke(newClient(validProps()), MAPPER.readTree("[]"));
        assertTrue(empty.isEmpty());
    }

    @Test
    void readPositionalListSkipsNullsAndNonArray() throws Exception {
        String payload = "{\"positional_args\":[\"a\",null,\"c\"]}";
        JsonNode root = MAPPER.readTree(payload);
        java.util.List<String> list = (java.util.List<String>) method("readPositionalList", JsonNode.class)
            .invoke(newClient(validProps()), root.get("positional_args"));
        assertEquals(java.util.Arrays.asList("a", "c"), list);

        java.util.List<String> empty = (java.util.List<String>) method("readPositionalList", JsonNode.class)
            .invoke(newClient(validProps()), (JsonNode) null);
        assertTrue(empty.isEmpty());

        empty = (java.util.List<String>) method("readPositionalList", JsonNode.class)
            .invoke(newClient(validProps()), MAPPER.readTree("{}"));
        assertTrue(empty.isEmpty());
    }

    @Test
    void closeStopsAndShutsDownPool() throws Exception {
        OpenCliWsReverseAgentClient client = newClient(validProps());
        Field poolField = OpenCliWsReverseAgentClient.class.getDeclaredField("collectPool");
        poolField.setAccessible(true);
        java.util.concurrent.ExecutorService pool = (java.util.concurrent.ExecutorService) poolField.get(client);
        client.close();
        assertTrue(pool.isShutdown());
    }

    @Test
    void startIsIdempotent() throws Exception {
        OpenCliWsReverseAgentClient client = newClient(validProps());
        client.start();
        Field runnerField = OpenCliWsReverseAgentClient.class.getDeclaredField("runnerThread");
        runnerField.setAccessible(true);
        Thread t = (Thread) runnerField.get(client);
        // second start should be no-op
        client.start();
        assertEquals(t, runnerField.get(client));
        client.stop();
        Field f = OpenCliWsReverseAgentClient.class.getDeclaredField("shutdownRequested");
        f.setAccessible(true);
        assertTrue((Boolean) f.getBoolean(client));
    }

    @Test
    void stopClearsActiveClient() throws Exception {
        OpenCliWsReverseAgentClient client = newClient(validProps());
        client.stop();
        Field f = OpenCliWsReverseAgentClient.class.getDeclaredField("activeClient");
        f.setAccessible(true);
        assertEquals(null, f.get(client));
        assertFalse(Thread.getAllStackTraces().keySet().stream()
            .anyMatch(thread -> thread.getName().equals("opencli-ws-agent-runner")));
    }

    private OpenCliWsReverseAgentClient newClient(OpenCliWsAgentConnectionProperties p) {
        return new OpenCliWsReverseAgentClient(new OpenCliProperties(), p);
    }
}