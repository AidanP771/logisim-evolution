/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.wiring;

import com.cburch.logisim.data.Location;
import java.util.ArrayList;
import java.util.List;

/** Pure geometry for click-to-route wiring: orthogonal L-shaped legs between clicked points. */
public final class RoutePlan {
  private RoutePlan() {}

  /**
   * The corner of the L from {@code a} to {@code b}: horizontal first, or vertical first when
   * flipped. Returns null if the two points are already in line.
   */
  public static Location corner(Location a, Location b, boolean horizontalFirst) {
    if (a.getX() == b.getX() || a.getY() == b.getY()) return null;
    return horizontalFirst
        ? Location.create(b.getX(), a.getY(), false)
        : Location.create(a.getX(), b.getY(), false);
  }

  /** Points of the route: {@code fixed} points, then an L leg to {@code to} (if not null). */
  public static List<Location> path(List<Location> fixed, Location to, boolean horizontalFirst) {
    final var points = new ArrayList<>(fixed);
    if (to != null && !points.isEmpty()) {
      final var last = points.get(points.size() - 1);
      final var c = corner(last, to, horizontalFirst);
      if (c != null) points.add(c);
      if (!to.equals(last)) points.add(to);
    }
    return points;
  }

  /** Consecutive pairs of {@code points} as segments, skipping zero-length ones. */
  public static List<Location[]> segments(List<Location> points) {
    final var segs = new ArrayList<Location[]>();
    for (var i = 1; i < points.size(); i++) {
      final var a = points.get(i - 1);
      final var b = points.get(i);
      if (!a.equals(b)) segs.add(new Location[] {a, b});
    }
    return segs;
  }
}
