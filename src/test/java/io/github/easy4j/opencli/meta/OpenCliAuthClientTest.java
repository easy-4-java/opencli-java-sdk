package io.github.easy4j.opencli.meta;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import org.junit.jupiter.api.Test;

class OpenCliAuthClientTest {

    private final OpenCliExecutor executor = new OpenCliExecutor(new OpenCliProperties());
    private final OpenCliAuthClient auth = new OpenCliAuthClient(executor);

    @Test
    void shouldNotBeNull() {
        assertNotNull(auth);
    }
}
