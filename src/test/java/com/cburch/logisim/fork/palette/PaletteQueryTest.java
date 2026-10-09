/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.fork.palette.PaletteQuery.Scope;
import org.junit.jupiter.api.Test;

public class PaletteQueryTest {
  @Test
  public void plainText() {
    assertEquals(new PaletteQuery(Scope.ALL, "adder", null), PaletteQuery.parse("  adder "));
    assertEquals(new PaletteQuery(Scope.ALL, "", null), PaletteQuery.parse(""));
  }

  @Test
  public void prefixesSelectScope() {
    assertEquals(new PaletteQuery(Scope.ACTIONS, "zoom", null), PaletteQuery.parse(">zoom"));
    assertEquals(new PaletteQuery(Scope.ACTIONS, "", null), PaletteQuery.parse(">"));
    assertEquals(new PaletteQuery(Scope.CIRCUITS, "half adder", null), PaletteQuery.parse("@ half adder"));
    // No parameters with a prefix: "@ alu 2" is a circuit name.
    assertEquals(new PaletteQuery(Scope.CIRCUITS, "alu 2", null), PaletteQuery.parse("@alu 2"));
  }

  @Test
  public void trailingNumberIsAParameter() {
    assertEquals(new PaletteQuery(Scope.ALL, "and", 3), PaletteQuery.parse("and 3"));
    assertEquals(new PaletteQuery(Scope.ALL, "mux", 4), PaletteQuery.parse("mux   4"));
    assertEquals(new PaletteQuery(Scope.ALL, "shift register", 8), PaletteQuery.parse("shift register 8"));
  }

  @Test
  public void numbersThatAreNotParameters() {
    assertEquals(new PaletteQuery(Scope.ALL, "74161", null), PaletteQuery.parse("74161"));
    assertEquals(new PaletteQuery(Scope.ALL, "and 0", null), PaletteQuery.parse("and 0"));
    assertEquals(new PaletteQuery(Scope.ALL, "ram 99999", null), PaletteQuery.parse("ram 99999"));
    assertEquals(new PaletteQuery(Scope.ALL, "and 3x", null), PaletteQuery.parse("and 3x"));
  }
}
