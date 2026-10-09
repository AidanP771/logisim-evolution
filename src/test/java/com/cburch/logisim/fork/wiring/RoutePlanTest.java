/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.wiring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.cburch.logisim.data.Location;
import java.util.List;
import org.junit.jupiter.api.Test;

public class RoutePlanTest {
  private static Location at(int x, int y) {
    return Location.create(x, y, false);
  }

  @Test
  public void cornerFollowsTheBendDirection() {
    assertEquals(at(50, 10), RoutePlan.corner(at(10, 10), at(50, 40), true));
    assertEquals(at(10, 40), RoutePlan.corner(at(10, 10), at(50, 40), false));
    assertNull(RoutePlan.corner(at(10, 10), at(10, 40), true));
  }

  @Test
  public void pathAddsAnLegToThePointer() {
    assertEquals(List.of(at(0, 0), at(30, 0), at(30, 20)), RoutePlan.path(List.of(at(0, 0)), at(30, 20), true));
    assertEquals(List.of(at(0, 0), at(0, 20), at(30, 20)), RoutePlan.path(List.of(at(0, 0)), at(30, 20), false));
    assertEquals(List.of(at(0, 0), at(30, 0)), RoutePlan.path(List.of(at(0, 0)), at(30, 0), true));
  }

  @Test
  public void segmentsAreOrthogonalAndSkipZeroLength() {
    final var segs = RoutePlan.segments(List.of(at(0, 0), at(30, 0), at(30, 0), at(30, 20)));
    assertEquals(2, segs.size());
    for (final var s : segs) {
      assertEquals(true, s[0].getX() == s[1].getX() || s[0].getY() == s[1].getY());
    }
  }
}
