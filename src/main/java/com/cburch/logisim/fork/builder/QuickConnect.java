/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.builder;

import static com.cburch.logisim.fork.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fork.ForkColors;
import com.cburch.logisim.fork.wiring.PinFinder;
import com.cburch.logisim.fork.wiring.RoutePlan;
import com.cburch.logisim.gui.main.Canvas;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Quick connect ({@code build.quickConnect}): drag from a pin with the Edit tool and release on
 * another pin, and the wire is routed around parts by {@link GridRouter}, with a preview while
 * dragging. Released anywhere else, it draws the same L-shaped wire Logisim's own drag does. One
 * undo step either way.
 */
public final class QuickConnect {
  private final Canvas canvas;
  private Circuit circuit;
  private Location start;
  private Location cursor;
  private Location target;
  private List<Location> route;
  private Boolean horizontalFirst;

  public QuickConnect(Canvas canvas) {
    this.canvas = canvas;
  }

  public boolean isActive() {
    return start != null;
  }

  public void begin(Location from) {
    circuit = canvas.getCircuit();
    start = from;
    cursor = from;
    target = null;
    route = null;
    horizontalFirst = null;
  }

  /** The pointer moved (circuit coordinates, not snapped) at {@code zoom}. */
  public void drag(Location at, double zoom) {
    if (!isActive()) return;
    final var snapped = Location.create(Canvas.snapXToGrid(at.getX()), Canvas.snapYToGrid(at.getY()),
        false);
    if (horizontalFirst == null && !snapped.equals(start)) {
      // Like Logisim's drag: the first leg follows the way the pointer first moved.
      horizontalFirst = Math.abs(at.getX() - start.getX()) >= Math.abs(at.getY() - start.getY());
    }
    final var pin = PinFinder.nearestConnectable(circuit, at, PinFinder.pinRadius(zoom));
    final var newTarget = pin != null && !pin.equals(start) ? pin : null;
    if (newTarget != null && !newTarget.equals(target)) {
      route = GridRouter.route(start, newTarget, CircuitGrid.around(circuit, start, newTarget));
    }
    target = newTarget;
    if (target == null) route = null;
    cursor = snapped;
    canvas.repaint();
  }

  public void cancel() {
    start = null;
    target = null;
    route = null;
    canvas.repaint();
  }

  /** The button was released: draw the wire. */
  public void finish() {
    if (!isActive()) return;
    final var points = currentPoints();
    final var into = circuit;
    final var routed = target != null && route != null;
    cancel();
    if (points == null || points.size() < 2 || into != canvas.getCircuit()) return;
    final var project = canvas.getProject();
    if (!project.getLogisimFile().contains(into)) {
      canvas.setErrorMessage(S.getter("forkCannotModify"));
      return;
    }
    final var xn = new CircuitMutation(into);
    for (final var s : RoutePlan.segments(points)) xn.add(Wire.create(s[0], s[1]));
    project.doAction(xn.toAction(S.getter(routed ? "forkQuickConnectAction" : "forkRouteWiresAction")));
  }

  /** The routed path to a pin, or the plain L to the pointer; null if there is nothing to draw. */
  private List<Location> currentPoints() {
    if (target != null) return route;
    if (cursor == null || cursor.equals(start)) return null;
    return RoutePlan.path(List.of(start), cursor, horizontalFirst == null || horizontalFirst);
  }

  /** Preview: solid when routed to a pin, dashed for the plain L; red ring if a pin can't be reached. */
  public void paint(Graphics2D g) {
    if (!isActive()) return;
    final var g2 = (Graphics2D) g.create();
    try {
      final var points = currentPoints();
      if (target != null && route == null) {
        g2.setColor(ForkColors.unconnected());
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(target.getX() - 6, target.getY() - 6, 12, 12);
        return;
      }
      if (points == null) return;
      g2.setColor(ForkColors.accent());
      if (target != null) {
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      } else {
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
            new float[] {6f, 4f}, 0f));
      }
      for (var i = 1; i < points.size(); i++) {
        final var a = points.get(i - 1);
        final var b = points.get(i);
        g2.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
      }
      if (target != null) g2.drawOval(target.getX() - 5, target.getY() - 5, 10, 10);
    } finally {
      g2.dispose();
    }
  }
}
