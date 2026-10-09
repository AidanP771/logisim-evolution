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

public class MruListTest {
  @Test
  public void mostRecentFirstWithoutDuplicates() {
    final var mru = new MruList<String>(10);
    mru.touch("a");
    mru.touch("b");
    mru.touch("c");
    mru.touch("a");
    assertEquals(List.of("a", "c", "b"), mru.items(s -> true));
  }

  @Test
  public void boundedByCapacity() {
    final var mru = new MruList<Integer>(3);
    for (var i = 0; i < 5; i++) mru.touch(i);
    assertEquals(List.of(4, 3, 2), mru.items(s -> true));
  }

  @Test
  public void dropsItemsThatAreNoLongerAlive() {
    final var mru = new MruList<String>(10);
    mru.touch("keep");
    mru.touch("deleted");
    mru.touch(null); // ignored
    assertEquals(List.of("keep"), mru.items(s -> !s.equals("deleted")));
    assertEquals(List.of("keep"), mru.items(s -> true)); // removal is permanent
  }
}
