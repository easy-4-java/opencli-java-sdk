package io.github.hiwepy.opencli.core;

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
        List<String> out = OpenCliArgSupport.merge(prefix, List.of("--limit", "5"));
        assertEquals(List.of("search", "q", "--limit", "5"), out);
    }

    @Test
    void addOptionEqualsProducesKeyValueToken() {
        List<String> t = new java.util.ArrayList<>(List.of("x"));
        OpenCliArgSupport.addOptionEquals(t, "--limit", "10");
        assertEquals(List.of("x", "--limit=10"), t);
    }
}
