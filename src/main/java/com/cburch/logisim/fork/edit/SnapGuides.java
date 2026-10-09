/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.edit;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.gui.main.Canvas;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Alignment guides while dragging components: a dashed line when a moving pin lines up
 * horizontally or vertically with a pin of another component. Drawing only; the drop position is
 * still Logisim's own grid-snapped one.
 */
public final class SnapGuides {
  private static final Color GUIDE = new Color(0xC2, 0x18, 0x5B);
  /** Pins further apart than this (circuit units) do not get a guide. */
  private static final int REACH = 400;

  private final Canvas canvas;
  private final List<Location> moving = new ArrayList<>();
  private final List<Location> others = new ArrayList<>();
  private int dx;
  private int dy;

  public SnapGuides(Canvas canvas) {
    this.canvas = canvas;
  }

  public boolean isActive() {
    return !moving.isEmpty();
  }

  /** Starts guides for dragging {@code comps}. */
  public void begin(Collection<Component> comps) {
    end();
    if (!ForkPreferences.SNAP_GUIDES.isEnabled()) return;
    for (final var comp : comps) {
      if (comp instanceof Wire) continue;
      for (final var e : comp.getEnds()) moving.add(e.getLocation());
    }
    for (final var comp : canvas.getCircuit().getNonWires()) {
      if (comps.contains(comp)) continue;
      for (final var e : comp.getEnds()) others.add(e.getLocation());
    }
    dx = 0;
    dy = 0;
  }

  /** The pointer moved by (rawDx, rawDy) circuit units since the press. */
  public void drag(int rawDx, int rawDy) {
    if (!isActive()) return;
    final var ndx = ArrangeActions.snap(rawDx);
    final var ndy = ArrangeActions.snap(rawDy);
    if (ndx == dx && ndy == dy) return;
    dx = ndx;
    dy = ndy;
    canvas.repaint();
  }

  public void end() {
    final var was = isActive();
    moving.clear();
    others.clear();
    if (was) canvas.repaint();
  }

  /** Guides as {x0, y0, x1, y1} lines (also used by tests). */
  List<int[]> lines() {
    final var lines = new ArrayList<int[]>();
    for (final var m : moving) {
      final var x = m.getX() + dx;
      final var y = m.getY() + dy;
      Location bestV = null;
      Location bestH = null;
      for (final var o : others) {
        if (o.getX() == x && Math.abs(o.getY() - y) <= REACH && o.getY() != y
            && (bestV == null || Math.abs(o.getY() - y) < Math.abs(bestV.getY() - y))) bestV = o;
        if (o.getY() == y && Math.abs(o.getX() - x) <= REACH && o.getX() != x
            && (bestH == null || Math.abs(o.getX() - x) < Math.abs(bestH.getX() - x))) bestH = o;
      }
      if (bestV != null) lines.add(new int[] {x, y, bestV.getX(), bestV.getY()});
      if (bestH != null) lines.add(new int[] {x, y, bestH.getX(), bestH.getY()});
    }
    return lines;
  }

  public void paint(Graphics2D g) {
    if (!isActive() || (dx == 0 && dy == 0)) return;
    final var g2 = (Graphics2D) g.create();
    try {
      g2.setColor(GUIDE);
      g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f,
          new float[] {4f, 3f}, 0f));
      for (final var l : lines()) g2.drawLine(l[0], l[1], l[2], l[3]);
    } finally {
      g2.dispose();
    }
  }
}
