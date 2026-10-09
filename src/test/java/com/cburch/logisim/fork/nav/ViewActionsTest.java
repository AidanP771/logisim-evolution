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

import com.cburch.logisim.data.Bounds;
import java.awt.Point;
import org.junit.jupiter.api.Test;

public class ViewActionsTest {
  @Test
  public void centredScrollPutsBoundsCentreInMiddleOfView() {
    // Bounds centre (150, 100) at zoom 2 is pixel (300, 200); an 400x300 view centred on it
    // starts at (100, 50).
    final var bounds = Bounds.create(100, 50, 100, 100);
    assertEquals(new Point(100, 50), ViewActions.centredScroll(bounds, 2.0, 400, 300));
  }

  @Test
  public void centredScrollCanBeNegativeForSmallCircuitsNearTheOrigin() {
    // The canvas clamps this to 0; the helper itself just reports the ideal position.
    final var bounds = Bounds.create(0, 0, 20, 20);
    assertEquals(new Point(-190, -140), ViewActions.centredScroll(bounds, 1.0, 400, 300));
  }
}
