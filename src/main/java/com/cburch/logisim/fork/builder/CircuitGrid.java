/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.builder;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * The {@link GridRouter.Grid} of a circuit: parts (their whole bounds, edges included, since pins
 * sit on the edges), every pin and every wire end are blocked; wire runs are recorded so the
 * router can cross them but not follow them. Built for a search area around the two ends.
 */
public final class CircuitGrid implements GridRouter.Grid {
  /** How far beyond the two ends the router may wander, in circuit units. */
  static final int MARGIN = 300;

  private final Set<Long> blocked = new HashSet<>();
  private final Set<Long> horizontal = new HashSet<>();
  private final Set<Long> vertical = new HashSet<>();
  private final int minX;
  private final int minY;
  private final int maxX;
  private final int maxY;

  private static long key(int x, int y) {
    return ((long) x << 32) ^ (y & 0xFFFFFFFFL);
  }

  /** The grid of {@code circuit} around {@code a} and {@code b}. */
  public static CircuitGrid around(Circuit circuit, Location a, Location b) {
    return new CircuitGrid(circuit.getNonWires(), circuit.getWires(),
        Math.min(a.getX(), b.getX()) - MARGIN, Math.min(a.getY(), b.getY()) - MARGIN,
        Math.max(a.getX(), b.getX()) + MARGIN, Math.max(a.getY(), b.getY()) + MARGIN);
  }

  CircuitGrid(Collection<? extends Component> parts, Collection<Wire> wires,
      int minX, int minY, int maxX, int maxY) {
    this.minX = floor(minX);
    this.minY = floor(minY);
    this.maxX = maxX;
    this.maxY = maxY;
    for (final var part : parts) {
      final var b = part.getBounds();
      if (b.getX() > maxX || b.getY() > maxY
          || b.getX() + b.getWidth() < minX || b.getY() + b.getHeight() < minY) {
        continue;
      }
      final var x0 = Math.max(ceil(b.getX()), this.minX);
      final var x1 = Math.min(b.getX() + b.getWidth(), maxX);
      final var y0 = Math.max(ceil(b.getY()), this.minY);
      final var y1 = Math.min(b.getY() + b.getHeight(), maxY);
      for (var x = x0; x <= x1; x += GridRouter.STEP) {
        for (var y = y0; y <= y1; y += GridRouter.STEP) blocked.add(key(x, y));
      }
      for (final var end : part.getEnds()) {
        blocked.add(key(end.getLocation().getX(), end.getLocation().getY()));
      }
    }
    for (final var w : wires) addWire(w.getEnd0(), w.getEnd1());
  }

  /** Records a wire from {@code e0} to {@code e1}, existing or only planned (e.g. a chain). */
  public void addWire(Location e0, Location e1) {
    blocked.add(key(e0.getX(), e0.getY()));
    blocked.add(key(e1.getX(), e1.getY()));
    if (e0.getX() == e1.getX()) {
      final var lo = Math.max(Math.min(e0.getY(), e1.getY()), minY);
      final var hi = Math.min(Math.max(e0.getY(), e1.getY()), maxY);
      for (var y = ceil(lo); y <= hi; y += GridRouter.STEP) vertical.add(key(e0.getX(), y));
    } else {
      final var lo = Math.max(Math.min(e0.getX(), e1.getX()), minX);
      final var hi = Math.min(Math.max(e0.getX(), e1.getX()), maxX);
      for (var x = ceil(lo); x <= hi; x += GridRouter.STEP) horizontal.add(key(x, e0.getY()));
    }
  }

  private static int floor(int v) {
    return Math.floorDiv(v, GridRouter.STEP) * GridRouter.STEP;
  }

  private static int ceil(int v) {
    return -Math.floorDiv(-v, GridRouter.STEP) * GridRouter.STEP;
  }

  /** Marks extra points as blocked (e.g. pins of parts about to be placed). */
  public void block(Location loc) {
    blocked.add(key(loc.getX(), loc.getY()));
  }

  @Override
  public boolean isBlocked(int x, int y) {
    return blocked.contains(key(x, y));
  }

  @Override
  public boolean hasHorizontalWire(int x, int y) {
    return horizontal.contains(key(x, y));
  }

  @Override
  public boolean hasVerticalWire(int x, int y) {
    return vertical.contains(key(x, y));
  }

  @Override
  public boolean inBounds(int x, int y) {
    return x >= minX && x <= maxX && y >= minY && y <= maxY;
  }
}
