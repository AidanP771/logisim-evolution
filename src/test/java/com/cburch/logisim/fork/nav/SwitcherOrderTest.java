/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.nav;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

public class SwitcherOrderTest {
  @Test
  public void recentFirstThenTheRestInProjectOrder() {
    final var all = List.of("main", "adder", "mux", "alu");
    assertEquals(List.of("mux", "main", "adder", "alu"), SwitcherOrder.order(List.of("mux", "main"), all));
  }

  @Test
  public void deletedCircuitsAreLeftOut() {
    assertEquals(List.of("b", "a"), SwitcherOrder.order(List.of("gone", "b"), List.of("a", "b")));
  }

  @Test
  public void noHistoryKeepsProjectOrder() {
    assertEquals(List.of("a", "b"), SwitcherOrder.order(List.of(), List.of("a", "b")));
  }
}
