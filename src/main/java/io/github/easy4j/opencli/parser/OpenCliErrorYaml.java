package io.github.easy4j.opencli.parser;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.events.CollectionStartEvent;
import org.yaml.snakeyaml.events.DocumentStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.NodeEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

/** Reads the upstream YAML subset with bounded structure and JSON-style scalar types. */
final class OpenCliErrorYaml {
    private static final Resolver WIRE_SCALARS = new WireResolver();

    private OpenCliErrorYaml() {
    }

    static Object read(String input, int maxDepth, int maxLength) {
        LoaderOptions settings = new LoaderOptions();
        settings.setAllowDuplicateKeys(false);
        settings.setAllowRecursiveKeys(false);
        settings.setMaxAliasesForCollections(0);
        settings.setCodePointLimit(maxLength);
        settings.setNestingDepthLimit(maxDepth);
        DumperOptions output = new DumperOptions();
        Yaml yaml = new Yaml(new WireConstructor(settings), new Representer(output), output, settings, WIRE_SCALARS);
        int documents = 0;
        int depth = 0;
        for (Event event : yaml.parse(new StringReader(input))) {
            if (event instanceof DocumentStartEvent && ++documents != 1) {
                throw new IllegalArgumentException("Unexpected YAML document count");
            }
            if (event instanceof NodeEvent && Objects.nonNull(((NodeEvent) event).getAnchor())) {
                throw new IllegalArgumentException("YAML anchors and aliases are unsupported");
            }
            if ((event instanceof ScalarEvent && Objects.nonNull(((ScalarEvent) event).getTag()))
                || (event instanceof CollectionStartEvent && Objects.nonNull(((CollectionStartEvent) event).getTag()))) {
                throw new IllegalArgumentException("Explicit YAML tags are unsupported");
            }
            if (event instanceof CollectionStartEvent && ++depth > maxDepth) {
                throw new IllegalArgumentException("YAML nesting depth exceeded");
            }
            if (event.is(Event.ID.MappingEnd) || event.is(Event.ID.SequenceEnd)) {
                depth--;
            }
        }
        return yaml.load(input);
    }

    private static final class WireResolver extends Resolver {
        @Override
        protected void addImplicitResolvers() {
            addImplicitResolver(Tag.BOOL, Pattern.compile("true|false"), "tf");
            addImplicitResolver(Tag.NULL, Pattern.compile("null"), "n");
            addImplicitResolver(Tag.INT, Pattern.compile("-?(?:0|[1-9][0-9]*)"), "-0123456789");
            addImplicitResolver(Tag.FLOAT,
                Pattern.compile("-?(?:0|[1-9][0-9]*)(?:[.][0-9]+)?(?:[eE][+-]?[0-9]+)?"), "-0123456789");
        }
    }

    private static final class WireConstructor extends SafeConstructor {
        WireConstructor(LoaderOptions settings) {
            super(settings);
            yamlConstructors.put(Tag.FLOAT, new AbstractConstruct() {
                @Override
                public Object construct(Node node) {
                    return new BigDecimal(((ScalarNode) node).getValue());
                }
            });
        }
    }
}
