/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.keymap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class MiniJsonTest {
  @Test
  public void parsesNestedStructures() {
    final var value = MiniJson.parse("""
        { "bindings": { "a": "F", "b": ["X", "Shift+X"], "c": null },
          "n": -1.5e1, "t": true, "u": "\\u0041\\n" }""");
    final var map = (Map<?, ?>) value;
    final var bindings = (Map<?, ?>) map.get("bindings");
    assertEquals("F", bindings.get("a"));
    assertEquals(List.of("X", "Shift+X"), bindings.get("b"));
    assertTrue(bindings.containsKey("c"));
    assertNull(bindings.get("c"));
    assertEquals(-15.0, map.get("n"));
    assertEquals(Boolean.TRUE, map.get("t"));
    assertEquals("A\n", map.get("u"));
  }

  @Test
  public void reportsWhereItFailed() {
    final var e = assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("{\n  \"a\": [1,,2]\n}"));
    assertTrue(e.getMessage().contains("line 2"), e.getMessage());
    assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("{\"a\": \"unterminated}"));
    assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("{} trailing"));
    assertThrows(IllegalArgumentException.class, () -> MiniJson.parse(""));
  }
}
