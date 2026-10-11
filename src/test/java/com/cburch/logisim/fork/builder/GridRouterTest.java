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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.data.Location;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class GridRouterTest {
  /** A plain grid: blocked points, horizontal and vertical wire runs, square bounds. */
  private static final class TestGrid implements GridRouter.Grid {
    final Set<Location> blocked = new HashSet<>();
    final Set<Location> hWire = new HashSet<>();
    final Set<Location> vWire = new HashSet<>();
    final int size;

    TestGrid(int size) {
      this.size = size;
    }

    void blockRect(int x0, int y0, int x1, int y1) {
      for (var x = x0; x <= x1; x += 10) {
        for (var y = y0; y <= y1; y += 10) blocked.add(at(x, y));
      }
    }

    void wire(int x0, int y0, int x1, int y1) {
      blocked.add(at(x0, y0));
      blocked.add(at(x1, y1));
      if (x0 == x1) {
        for (var y = Math.min(y0, y1); y <= Math.max(y0, y1); y += 10) vWire.add(at(x0, y));
      } else {
        for (var x = Math.min(x0, x1); x <= Math.max(x0, x1); x += 10) hWire.add(at(x, y0));
      }
    }

    @Override
    public boolean isBlocked(int x, int y) {
      return blocked.contains(at(x, y));
    }

    @Override
    public boolean hasHorizontalWire(int x, int y) {
      return hWire.contains(at(x, y));
    }

    @Override
    public boolean hasVerticalWire(int x, int y) {
      return vWire.contains(at(x, y));
    }

    @Override
    public boolean inBounds(int x, int y) {
      return x >= 0 && y >= 0 && x <= size && y <= size;
    }
  }

  private static Location at(int x, int y) {
    return Location.create(x, y, false);
  }

  /** Every grid point the route passes through, ends included. */
  private static List<Location> points(List<Location> corners) {
    final var out = new ArrayList<Location>();
    out.add(corners.get(0));
    for (var i = 1; i < corners.size(); i++) {
      final var a = corners.get(i - 1);
      final var b = corners.get(i);
      assertTrue(a.getX() == b.getX() || a.getY() == b.getY(), "orthogonal");
      final var dx = Integer.signum(b.getX() - a.getX()) * 10;
      final var dy = Integer.signum(b.getY() - a.getY()) * 10;
      var p = a;
      while (!p.equals(b)) {
        p = at(p.getX() + dx, p.getY() + dy);
        out.add(p);
      }
    }
    return out;
  }

  @Test
  public void straightLineWhenNothingIsInTheWay() {
    final var route = GridRouter.route(at(0, 50), at(100, 50), new TestGrid(200));
    assertEquals(List.of(at(0, 50), at(100, 50)), route);
  }

  @Test
  public void oneBendForAnLShape() {
    final var route = GridRouter.route(at(0, 0), at(100, 80), new TestGrid(200));
    assertNotNull(route);
    assertEquals(3, route.size(), "one bend: " + route);
  }

  @Test
  public void detoursAroundAPartWithoutTouchingIt() {
    final var grid = new TestGrid(300);
    grid.blockRect(80, 20, 120, 120); // a part straddling the straight line
    final var route = GridRouter.route(at(0, 70), at(200, 70), grid);
    assertNotNull(route);
    for (final var p : points(route)) {
      assertFalse(grid.isBlocked(p.getX(), p.getY()), "passes through the part at " + p);
    }
  }

  @Test
  public void crossesAWireAtRightAnglesButNeverBendsOnOrFollowsIt() {
    final var grid = new TestGrid(300);
    grid.wire(100, 0, 100, 300); // a vertical wire across the whole area
    final var route = GridRouter.route(at(0, 150), at(200, 150), grid);
    assertEquals(List.of(at(0, 150), at(200, 150)), route, "straight across");

    // A diagonal trip over the wire: it may cross, but never bend on it or run along it.
    final var along = GridRouter.route(at(50, 20), at(150, 280), grid);
    assertNotNull(along);
    for (var i = 1; i < along.size() - 1; i++) {
      final var c = along.get(i);
      assertFalse(grid.hasVerticalWire(c.getX(), c.getY()), "bends on the wire at " + c);
    }
    final var pts = points(along);
    for (var i = 1; i < pts.size(); i++) {
      final var a = pts.get(i - 1);
      final var b = pts.get(i);
      assertFalse(a.getX() == b.getX() && grid.hasVerticalWire(a.getX(), a.getY())
          && grid.hasVerticalWire(b.getX(), b.getY()), "runs along the wire");
    }
  }

  @Test
  public void noRouteWhenTheTargetIsWalledIn() {
    final var grid = new TestGrid(200);
    grid.blockRect(80, 80, 120, 80);
    grid.blockRect(80, 120, 120, 120);
    grid.blockRect(80, 90, 80, 110);
    grid.blockRect(120, 90, 120, 110);
    assertNull(GridRouter.route(at(0, 0), at(100, 100), grid));
  }

  @Test
  public void offGridEndsAreRefused() {
    assertNull(GridRouter.route(at(0, 5), at(100, 50), new TestGrid(200)));
  }

  @Test
  public void prefersFewerBends() {
    final var grid = new TestGrid(400);
    grid.blockRect(150, 0, 150, 300); // a wall with a gap at the bottom
    final var route = GridRouter.route(at(0, 100), at(300, 100), grid);
    assertNotNull(route);
    // Straight down out of the start, across under the wall, straight up into the target: two
    // bends, never a staircase.
    assertEquals(4, route.size(), route.toString());
    assertTrue(route.get(1).getY() > 300, "goes under the wall: " + route);
  }
}
