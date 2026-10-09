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

public class ZoomStepsTest {
  private static final List<Double> OPTIONS = List.of(25.0, 50.0, 100.0, 200.0, 400.0);

  @Test
  public void stepsToNeighbouringLevels() {
    assertEquals(2.0, ZoomSteps.next(1.0, OPTIONS, true), 1e-9);
    assertEquals(0.5, ZoomSteps.next(1.0, OPTIONS, false), 1e-9);
  }

  @Test
  public void offGridZoomSnapsToNextLevel() {
    assertEquals(1.0, ZoomSteps.next(0.73, OPTIONS, true), 1e-9);
    assertEquals(0.5, ZoomSteps.next(0.73, OPTIONS, false), 1e-9);
  }

  @Test
  public void staysPutAtTheEnds() {
    assertEquals(4.0, ZoomSteps.next(4.0, OPTIONS, true), 1e-9);
    assertEquals(0.25, ZoomSteps.next(0.25, OPTIONS, false), 1e-9);
    assertEquals(1.0, ZoomSteps.next(1.0, List.of(), true), 1e-9);
  }

  @Test
  public void wheelAccumulatorEmitsWholeSteps() {
    final var acc = new ZoomSteps.WheelAccumulator();
    assertEquals(0, acc.add(-0.4));
    assertEquals(0, acc.add(-0.4));
    assertEquals(-1, acc.add(-0.4)); // -1.2 total
    assertEquals(0, acc.add(-0.7)); // -0.9 pending
    assertEquals(0, acc.add(0.5)); // direction change drops the pending -0.9
    assertEquals(1, acc.add(0.6));
    assertEquals(3, acc.add(3.0)); // a mouse wheel notch can be several steps at once
  }
}
