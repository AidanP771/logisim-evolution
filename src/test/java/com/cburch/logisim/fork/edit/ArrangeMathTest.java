/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.edit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.fork.edit.ArrangeActions.Edge;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ArrangeMathTest {
  private static final List<Bounds> THREE = List.of(
      Bounds.create(100, 100, 40, 40),
      Bounds.create(130, 200, 60, 20),
      Bounds.create(80, 50, 20, 30));

  @Test
  public void alignEdges() {
    assertArrayEquals(new int[] {-20, -50, 0}, ArrangeActions.alignOffsets(THREE, Edge.LEFT));
    assertArrayEquals(new int[] {50, 0, 90}, ArrangeActions.alignOffsets(THREE, Edge.RIGHT));
    assertArrayEquals(new int[] {-50, -150, 0}, ArrangeActions.alignOffsets(THREE, Edge.TOP));
    assertArrayEquals(new int[] {80, 0, 140}, ArrangeActions.alignOffsets(THREE, Edge.BOTTOM));
  }

  @Test
  public void alignCentresOnTheGroupMiddleAndSnapToGrid() {
    // Centre x: 120, 160, 90 -> middle (90+160)/2 = 125 -> offsets 5, -35, 35, snapped to the grid.
    final var offsets = ArrangeActions.alignOffsets(THREE, Edge.CENTER_X);
    assertArrayEquals(new int[] {10, -30, 40}, offsets);
    // Whatever the rounding, the centres end up on one line.
    for (var i = 0; i < THREE.size(); i++) {
      assertEquals(130, ArrangeActions.coordinate(THREE.get(i), Edge.CENTER_X) + offsets[i]);
    }
  }

  @Test
  public void distributeSpacesCentresEvenlyAndKeepsTheEnds() {
    final var row = List.of(
        Bounds.create(0, 0, 20, 20), // centre 10
        Bounds.create(40, 0, 20, 20), // centre 50
        Bounds.create(200, 0, 20, 20)); // centre 210
    // Ends stay (10 and 210); the middle goes to 110.
    assertArrayEquals(new int[] {0, 60, 0}, ArrangeActions.distributeOffsets(row, true));
    // Order in the list does not matter.
    final var shuffled = List.of(row.get(2), row.get(0), row.get(1));
    assertArrayEquals(new int[] {0, 0, 60}, ArrangeActions.distributeOffsets(shuffled, true));
  }

  @Test
  public void snapRoundsToGridSteps() {
    assertEquals(10, ArrangeActions.snap(5));
    assertEquals(0, ArrangeActions.snap(4));
    assertEquals(-10, ArrangeActions.snap(-6));
  }
}
