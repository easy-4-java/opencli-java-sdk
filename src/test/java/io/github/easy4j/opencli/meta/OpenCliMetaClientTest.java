package io.github.easy4j.opencli.meta;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import org.junit.jupiter.api.Test;

class OpenCliMetaClientTest {

    private final OpenCliExecutor executor = new OpenCliExecutor(new OpenCliProperties());
    private final OpenCliMetaClient meta = new OpenCliMetaClient(executor);

    @Test
    void shouldCreateSubClients() {
        assertNotNull(meta.plugin());
        assertNotNull(meta.adapter());
        assertNotNull(meta.profile());
        assertNotNull(meta.daemon());
        assertNotNull(meta.external());
        assertNotNull(meta.skills());
        assertNotNull(meta.auth());
        assertNotNull(meta.antigravity());
    }
}
