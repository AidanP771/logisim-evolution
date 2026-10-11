/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import java.util.List;
import org.junit.jupiter.api.Test;

public class StampTest {
  private static Location at(int x, int y) {
    return Location.create(x, y, false);
  }

  @Test
  public void labelsContinueTheSequence() {
    assertEquals("B", Stamp.nextLabel("A"));
    assertEquals("X1", Stamp.nextLabel("X0"));
    assertEquals("R10", Stamp.nextLabel("R09"));
    assertEquals("in100", Stamp.nextLabel("in99"));
    assertEquals("AA", Stamp.nextLabel("Z"));
    assertEquals("BA", Stamp.nextLabel("AZ"));
    assertEquals("AAA", Stamp.nextLabel("ZZ"));
    assertEquals("b", Stamp.nextLabel("a"));
    assertEquals("q_1", Stamp.nextLabel("q_"));
    assertEquals("", Stamp.nextLabel(""));
  }

  @Test
  public void copiesGoAlongTheMainDragAxisOnePartApart() {
    // A 50x50 part: copies 60 apart (size plus one grid step).
    final var part = Bounds.create(-50, -25, 50, 50);
    assertEquals(List.of(at(100, 100)), Stamp.spots(at(100, 100), at(150, 110), part));
    assertEquals(List.of(at(100, 100), at(160, 100), at(220, 100)),
        Stamp.spots(at(100, 100), at(225, 130), part));
    assertEquals(List.of(at(100, 300), at(100, 240)),
        Stamp.spots(at(100, 300), at(90, 235), part), "upwards");
  }

  @Test
  public void spacingRoundsUpToTheGrid() {
    final var odd = Bounds.create(0, 0, 33, 20);
    assertEquals(at(150, 0), Stamp.spots(at(100, 0), at(160, 0), odd).get(1));
  }
}
