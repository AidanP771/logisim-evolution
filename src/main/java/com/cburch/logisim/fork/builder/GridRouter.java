/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.builder;

import com.cburch.logisim.data.Location;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Orthogonal auto-router: A* over the 10-unit grid from one point to another, minimising length
 * plus a penalty per bend.
 *
 * <p>The result must never connect to anything by accident. In Logisim a wire end touching a wire
 * or pin connects to it, and overlapping wires merge, so the route never passes through a part or
 * a pin, never touches a wire end, never runs along an existing wire and never bends on one. It
 * may cross other wires at right angles (no connection is made there).
 */
public final class GridRouter {
  /** Grid spacing in circuit units. */
  public static final int STEP = 10;
  /** A bend costs as much as this many grid steps, so routes prefer fewer, longer runs. */
  static final int BEND_COST = 4;
  /** Give up after expanding this many states (a route that far away is not worth drawing). */
  static final int MAX_EXPANSIONS = 400_000;

  /** What the router must avoid, in circuit coordinates (grid points). */
  public interface Grid {
    /** Inside a part, on a pin, or on a wire end: never pass through. */
    boolean isBlocked(int x, int y);

    /** An existing horizontal wire covers this point (moving along it would merge). */
    boolean hasHorizontalWire(int x, int y);

    /** An existing vertical wire covers this point. */
    boolean hasVerticalWire(int x, int y);

    /** Limits the search area: points outside are never visited. */
    boolean inBounds(int x, int y);
  }

  private static final int[] DX = {STEP, -STEP, 0, 0};
  private static final int[] DY = {0, 0, STEP, -STEP};

  private GridRouter() {}

  private static boolean horizontal(int dir) {
    return dir < 2;
  }

  private record State(int x, int y, int dir) {}

  private record Entry(State state, int f, int g) {}

  /**
   * The route from {@code from} to {@code to} as its corner points (first is {@code from}, last is
   * {@code to}), or {@code null} if there is none. Both ends must be on the grid; they may be
   * blocked (they are the pins being joined).
   */
  public static List<Location> route(Location from, Location to, Grid grid) {
    if (from.equals(to)) return null;
    if (from.getX() % STEP != 0 || from.getY() % STEP != 0
        || to.getX() % STEP != 0 || to.getY() % STEP != 0) {
      return null;
    }
    final var best = new HashMap<State, Integer>();
    final var parent = new HashMap<State, State>();
    final var open = new PriorityQueue<Entry>(
        (a, b) -> a.f() != b.f() ? Integer.compare(a.f(), b.f()) : Integer.compare(b.g(), a.g()));
    for (var d = 0; d < 4; d++) {
      final var s = new State(from.getX(), from.getY(), d);
      best.put(s, 0);
      open.add(new Entry(s, heuristic(from.getX(), from.getY(), d, to), 0));
    }
    var expansions = 0;
    while (!open.isEmpty()) {
      final var entry = open.poll();
      final var cur = entry.state();
      if (entry.g() > best.getOrDefault(cur, Integer.MAX_VALUE)) continue;
      if (cur.x() == to.getX() && cur.y() == to.getY()) return corners(cur, parent, from);
      if (++expansions > MAX_EXPANSIONS) return null;
      for (var d = 0; d < 4; d++) {
        if (d == (cur.dir() ^ 1)) continue; // no doubling back
        final var turning = d != cur.dir() && !(cur.x() == from.getX() && cur.y() == from.getY());
        if (turning && onWire(grid, cur.x(), cur.y())) continue; // a bend here would connect
        final var nx = cur.x() + DX[d];
        final var ny = cur.y() + DY[d];
        if (!grid.inBounds(nx, ny)) continue;
        final var target = nx == to.getX() && ny == to.getY();
        if (!target && grid.isBlocked(nx, ny)) continue;
        // Never run along an existing wire: that would merge with it.
        if (horizontal(d) && grid.hasHorizontalWire(nx, ny)
            && grid.hasHorizontalWire(cur.x(), cur.y())) {
          continue;
        }
        if (!horizontal(d) && grid.hasVerticalWire(nx, ny)
            && grid.hasVerticalWire(cur.x(), cur.y())) {
          continue;
        }
        final var g = entry.g() + 1 + (turning ? BEND_COST : 0);
        final var next = new State(nx, ny, d);
        if (g < best.getOrDefault(next, Integer.MAX_VALUE)) {
          best.put(next, g);
          parent.put(next, cur);
          open.add(new Entry(next, g + heuristic(nx, ny, d, to), g));
        }
      }
    }
    return null;
  }

  private static boolean onWire(Grid grid, int x, int y) {
    return grid.hasHorizontalWire(x, y) || grid.hasVerticalWire(x, y);
  }

  /** Grid steps still needed, plus one bend if the target is not straight ahead. */
  private static int heuristic(int x, int y, int dir, Location to) {
    final var dx = Math.abs(to.getX() - x) / STEP;
    final var dy = Math.abs(to.getY() - y) / STEP;
    var h = dx + dy;
    if (dx != 0 && dy != 0) {
      h += BEND_COST;
    } else if ((dx != 0 && !horizontal(dir)) || (dy != 0 && horizontal(dir))) {
      h += BEND_COST;
    }
    return h;
  }

  /** Walks back from the goal, keeping only the points where the direction changes. */
  private static List<Location> corners(State goal, Map<State, State> parent, Location from) {
    final var path = new ArrayList<State>();
    for (var s = goal; s != null; s = parent.get(s)) path.add(0, s);
    final var points = new ArrayList<Location>();
    points.add(from);
    for (var i = 1; i < path.size() - 1; i++) {
      if (path.get(i).dir() != path.get(i + 1).dir()) {
        points.add(Location.create(path.get(i).x(), path.get(i).y(), false));
      }
    }
    final var last = path.get(path.size() - 1);
    points.add(Location.create(last.x(), last.y(), false));
    return points;
  }
}
