/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.wiring;

import static com.cburch.logisim.fork.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.gui.main.Canvas;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Click-to-route wiring: click a pin to start, click to add bends, click a pin or wire (or
 * double-click, or press Enter) to finish. Space or "/" flips which way the next bend goes,
 * Backspace removes the last bend, Esc cancels. The whole route is one undo step and produces
 * ordinary wire segments.
 */
public final class WireRouter {
  private static final Color PREVIEW = new Color(0x1E, 0x6B, 0xB8);

  private final Canvas canvas;
  private final List<Location> points = new ArrayList<>();
  /** How many points each click added (1, or 2 with its corner), so Backspace can undo it. */
  private final List<Integer> added = new ArrayList<>();
  private Circuit circuit;
  private Location cursor;
  private boolean horizontalFirst = true;

  public WireRouter(Canvas canvas) {
    this.canvas = canvas;
  }

  public boolean isActive() {
    return !points.isEmpty();
  }

  /** Keys the router takes while active (so keymap shortcuts leave them alone). */
  public static boolean isRouteKey(KeyEvent e) {
    return switch (e.getKeyCode()) {
      case KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE, KeyEvent.VK_SPACE, KeyEvent.VK_SLASH,
          KeyEvent.VK_ENTER -> true;
      default -> e.getID() == KeyEvent.KEY_TYPED && (e.getKeyChar() == ' ' || e.getKeyChar() == '/');
    };
  }

  public void start(Location at) {
    points.clear();
    added.clear();
    points.add(at);
    circuit = canvas.getCircuit();
    cursor = at;
    horizontalFirst = true;
    hint();
    canvas.repaint();
  }

  /** Whether the route still belongs to what the canvas shows (else it is abandoned). */
  public boolean isStale() {
    return isActive() && canvas.getCircuit() != circuit;
  }

  public void moveCursor(Location at) {
    if (at.equals(cursor)) return;
    cursor = at;
    canvas.repaint();
  }

  /**
   * A click while routing: ends the route on a pin or wire (or on a double-click), otherwise adds
   * a bend there.
   */
  public void click(Location at, boolean doubleClick) {
    if (doubleClick || (!at.equals(points.get(0)) && PinFinder.isConnectionPoint(circuit, at))) {
      finish(at);
      return;
    }
    final var last = points.get(points.size() - 1);
    if (at.equals(last)) return;
    final var c = RoutePlan.corner(last, at, horizontalFirst);
    if (c != null) points.add(c);
    points.add(at);
    added.add(c != null ? 2 : 1);
    canvas.repaint();
  }

  /** Space or "/": the next leg bends the other way. */
  public void flip() {
    horizontalFirst = !horizontalFirst;
    canvas.repaint();
  }

  /** Backspace: removes the last click (and its corner); with none left, cancels. */
  public void undoPoint() {
    if (added.isEmpty()) {
      cancel();
      return;
    }
    final var count = added.remove(added.size() - 1);
    for (var k = 0; k < count; k++) points.remove(points.size() - 1);
    canvas.repaint();
  }

  public void cancel() {
    points.clear();
    added.clear();
    cursor = null;
    clearHint();
    canvas.repaint();
  }

  /** Enter: finishes at the pointer. */
  public void finishAtCursor() {
    finish(cursor != null ? cursor : points.get(points.size() - 1));
  }

  private void finish(Location end) {
    final var route = RoutePlan.path(points, end, horizontalFirst);
    final var segs = RoutePlan.segments(route);
    final var target = circuit;
    cancel();
    if (segs.isEmpty() || target != canvas.getCircuit()) return;
    final var project = canvas.getProject();
    if (!project.getLogisimFile().contains(target)) {
      canvas.setErrorMessage(S.getter("forkCannotModify"));
      return;
    }
    final var xn = new CircuitMutation(target);
    for (final var s : segs) xn.add(Wire.create(s[0], s[1]));
    project.doAction(xn.toAction(S.getter(segs.size() == 1 ? "forkRouteWireAction" : "forkRouteWiresAction")));
  }

  /** Draws the route so far (solid) and the next leg to the pointer (dashed). */
  public void paint(Graphics2D g) {
    if (!isActive()) return;
    final var g2 = (Graphics2D) g.create();
    try {
      g2.setColor(PREVIEW);
      g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      drawPolyline(g2, points);
      final var last = points.get(points.size() - 1);
      if (cursor != null) {
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
            new float[] {6f, 4f}, 0f));
        final var leg = new ArrayList<Location>();
        leg.add(last);
        final var c = RoutePlan.corner(last, cursor, horizontalFirst);
        if (c != null) leg.add(c);
        leg.add(cursor);
        drawPolyline(g2, leg);
      }
      for (final var p : points) g2.fillOval(p.getX() - 3, p.getY() - 3, 7, 7);
    } finally {
      g2.dispose();
    }
  }

  private static void drawPolyline(Graphics2D g, List<Location> pts) {
    for (var i = 1; i < pts.size(); i++) {
      final var a = pts.get(i - 1);
      final var b = pts.get(i);
      g.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
    }
  }

  private void hint() {
    if (ForkPreferences.STATUS_HINTS.isEnabled()) {
      canvas.setErrorMessage(S.getter("forkHintRouting"), WireActions.INFO_COLOR);
    }
  }

  private void clearHint() {
    if (ForkPreferences.STATUS_HINTS.isEnabled()) canvas.setErrorMessage(null);
  }
}
