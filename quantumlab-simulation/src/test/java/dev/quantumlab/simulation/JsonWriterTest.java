package dev.quantumlab.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JsonWriterTest {

    @Test
    @DisplayName("a nested document round-trips through a strict parser")
    void nestedRoundTrip() {
        String json =
                new JsonWriter()
                        .startObject()
                        .key("name").value("x")
                        .key("n").value(3L)
                        .key("flag").value(true)
                        .key("pi").value(3.141592653589793)
                        .key("nothing").nullValue()
                        .key("list").startArray().value(1L).value(2L).startObject().key("k").value("v").endObject().endArray()
                        .key("empty").startObject().endObject()
                        .key("emptyList").startArray().endArray()
                        .endObject()
                        .toString();
        Map<String, Object> m = MiniJson.object(MiniJson.parse(json));
        assertEquals("x", m.get("name"));
        assertEquals(3.0, MiniJson.number(m.get("n")), 0.0);
        assertEquals(Boolean.TRUE, m.get("flag"));
        assertEquals(3.141592653589793, MiniJson.number(m.get("pi")), 0.0);
        assertNull(m.get("nothing"));
        assertTrue(m.containsKey("nothing"));
        List<Object> list = MiniJson.array(m.get("list"));
        assertEquals(3, list.size());
        assertEquals("v", MiniJson.object(list.get(2)).get("k"));
        assertTrue(MiniJson.object(m.get("empty")).isEmpty());
        assertTrue(MiniJson.array(m.get("emptyList")).isEmpty());
    }

    @Test
    @DisplayName("strings are escaped and read back identically")
    void escaping() {
        String nasty = "quote\" backslash\\ newline\n tab\t ctrl\u0001 unicode |0⟩ σ";
        String json = new JsonWriter().startObject().key("s").value(nasty).endObject().toString();
        assertEquals(nasty, MiniJson.object(MiniJson.parse(json)).get("s"));
    }

    @Test
    @DisplayName("keys are escaped too")
    void keyEscaping() {
        String json = new JsonWriter().startObject().key("a\"b").value(1L).endObject().toString();
        assertTrue(MiniJson.object(MiniJson.parse(json)).containsKey("a\"b"));
    }

    @Test
    @DisplayName("non-finite doubles become null (JSON has no NaN or Infinity)")
    void nonFinite() {
        String json =
                new JsonWriter()
                        .startArray()
                        .value(Double.NaN)
                        .value(Double.POSITIVE_INFINITY)
                        .value(1.5)
                        .endArray()
                        .toString();
        List<Object> list = MiniJson.array(MiniJson.parse(json));
        assertNull(list.get(0));
        assertNull(list.get(1));
        assertEquals(1.5, MiniJson.number(list.get(2)), 0.0);
    }

    @Test
    @DisplayName("doubles round-trip exactly, including tiny and huge magnitudes")
    void doubleRoundTrip() {
        double[] values = {0.1, 1e-300, 1.7976931348623157e308, -2.5e-17, 123456789.123456789, 0.0};
        JsonWriter w = new JsonWriter().startArray();
        for (double v : values) {
            w.value(v);
        }
        List<Object> parsed = MiniJson.array(MiniJson.parse(w.endArray().toString()));
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], MiniJson.number(parsed.get(i)), 0.0);
        }
    }

    @Test
    @DisplayName("an unclosed container is detected")
    void unclosed() {
        JsonWriter w = new JsonWriter().startObject();
        assertThrows(IllegalStateException.class, w::toString);
    }

    @Test
    @DisplayName("closing with nothing open is detected")
    void overClose() {
        assertThrows(IllegalStateException.class, () -> new JsonWriter().endObject());
    }

    @Test
    @DisplayName("MiniJson itself rejects malformed input (so the round-trip tests are meaningful)")
    void parserIsStrict() {
        assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("{\"a\": 1,}"));
        assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("{\"a\" 1}"));
        assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("[1, 2"));
        assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("{} extra"));
        assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("{\"a\": NaN}"));
    }
}
