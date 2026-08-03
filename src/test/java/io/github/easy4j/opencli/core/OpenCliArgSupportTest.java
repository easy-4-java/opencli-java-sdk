package io.github.easy4j.opencli.core;

import io.github.easy4j.opencli.util.OpenCliLists;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link OpenCliArgSupport} 单元测试。
 */
class OpenCliArgSupportTest {

    @Test
    void mergeFiltersBlanksAndConcatenates() {
        List<String> prefix = new java.util.ArrayList<>();
        prefix.add("search");
        prefix.add("  q  ");
        prefix.add("");
        prefix.add(null);
        List<String> out = OpenCliArgSupport.merge(prefix, OpenCliLists.of("--limit", "5"));
        assertEquals(OpenCliLists.of("search", "q", "--limit", "5"), out);
    }

    @Test
    void addOptionEqualsProducesKeyValueToken() {
        List<String> t = new java.util.ArrayList<>(OpenCliLists.of("x"));
        OpenCliArgSupport.addOptionEquals(t, "--limit", "10");
        assertEquals(OpenCliLists.of("x", "--limit=10"), t);
    }
}
