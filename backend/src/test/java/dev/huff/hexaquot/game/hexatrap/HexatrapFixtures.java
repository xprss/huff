package dev.huff.hexaquot.game.hexatrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/** Hand-written cases shared by the engine and service tests. */
public final class HexatrapFixtures {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    private HexatrapFixtures() {}

    public static ObjectNode lanternaJson() {
        try (InputStream input = HexatrapFixtures.class.getResourceAsStream("/hexatrap/case-lanterna.json")) {
            return (ObjectNode) MAPPER.readTree(input);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    public static HexatrapCase.CaseDefinition lanterna() {
        return lanterna(json -> {});
    }

    public static HexatrapCase.CaseDefinition lanterna(Consumer<ObjectNode> mutation) {
        ObjectNode json = lanternaJson();
        mutation.accept(json);
        try {
            return MAPPER.treeToValue(json, HexatrapCase.CaseDefinition.class);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    /** Finds an item by id in any item list of the JSON case. */
    public static ObjectNode item(ObjectNode json, String id) {
        for (String list : new String[] {"certainties", "statements", "questions", "clues"}) {
            for (var node : json.withArray(list)) if (id.equals(node.path("id").asText())) return (ObjectNode) node;
        }
        throw new IllegalArgumentException(id);
    }
}
