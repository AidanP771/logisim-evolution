/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.wiring;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Location;

/** Hit-testing for pins, wire ends and wires, with tolerances that grow when zoomed out. */
public final class PinFinder {
  private PinFinder() {}

  /**
   * Pick-up radius around pins and wire ends, in circuit units: about 9 screen pixels, between 6
   * (Logisim's own radius) and 12 units. Pins are 10 units apart and the nearest one wins.
   */
  public static int pinRadius(double zoom) {
    return (int) Math.max(6, Math.min(12, Math.round(9 / Math.max(zoom, 0.01))));
  }

  /** The pin or wire end nearest {@code at} within {@code radius}, or null. */
  public static Location nearestConnectable(Circuit circuit, Location at, int radius) {
    Location best = null;
    var bestDist = Long.MAX_VALUE;
    final var r2 = (long) radius * radius;
    for (final var comp : circuit.getNonWires()) {
      for (final var end : comp.getEnds()) {
        final var d = dist2(end.getLocation(), at);
        if (d <= r2 && d < bestDist) {
          bestDist = d;
          best = end.getLocation();
        }
      }
    }
    for (final var w : circuit.getWires()) {
      for (final var end : new Location[] {w.getEnd0(), w.getEnd1()}) {
        final var d = dist2(end, at);
        if (d <= r2 && d < bestDist) {
          bestDist = d;
          best = end;
        }
      }
    }
    return best;
  }

  /** The wire passing within {@code tolerance} of {@code at}, or null. */
  public static Wire wireNear(Circuit circuit, Location at, int tolerance) {
    Wire best = null;
    var bestDist = Integer.MAX_VALUE;
    for (final var w : circuit.getWires()) {
      final var e0 = w.getEnd0();
      final var e1 = w.getEnd1();
      final int d;
      if (w.isVertical()) {
        if (at.getY() < Math.min(e0.getY(), e1.getY()) - tolerance
            || at.getY() > Math.max(e0.getY(), e1.getY()) + tolerance) continue;
        d = Math.abs(at.getX() - e0.getX());
      } else {
        if (at.getX() < Math.min(e0.getX(), e1.getX()) - tolerance
            || at.getX() > Math.max(e0.getX(), e1.getX()) + tolerance) continue;
        d = Math.abs(at.getY() - e0.getY());
      }
      if (d <= tolerance && d < bestDist) {
        bestDist = d;
        best = w;
      }
    }
    return best;
  }

  /** Where a click-routed wire can end: a pin, a wire end, or a point on a wire. */
  public static boolean isConnectionPoint(Circuit circuit, Location loc) {
    if (!circuit.getComponents(loc).isEmpty()) return true;
    for (final var w : circuit.getWires()) {
      if (w.contains(loc)) return true;
    }
    return false;
  }

  private static long dist2(Location a, Location b) {
    final long dx = a.getX() - b.getX();
    final long dy = a.getY() - b.getY();
    return dx * dx + dy * dy;
  }
}
