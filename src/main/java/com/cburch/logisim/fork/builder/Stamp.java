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

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fork.ForkColors;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.FactoryAttributes;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Stamp placement ({@code build.stamp}): with a part armed, press and drag to lay a row (or
 * column, whichever way the drag mostly goes) of copies at the part's width (or height) plus one
 * grid step. Released, they are added in one undo step and the tool stays armed. If the armed part
 * has a label, the copies continue it: A, B, C or X0, X1, X2.
 */
public final class Stamp {
  private final Canvas canvas;
  private AddTool tool;
  private Location start;
  private List<Location> spots = List.of();

  public Stamp(Canvas canvas) {
    this.canvas = canvas;
  }

  /** A press with {@code tool} armed, at circuit point {@code at} (not yet snapped). */
  public void press(AddTool tool, Location at) {
    this.tool = tool;
    this.start = snap(tool, at);
    spots = List.of(start);
  }

  public boolean isPressed() {
    return tool != null;
  }

  /** Two or more copies: this press is a stamp, not an ordinary placement. */
  public boolean isStamping() {
    return spots.size() >= 2;
  }

  public void drag(Location at) {
    if (tool == null) return;
    final var source = tool.getFactory();
    if (source == null) return;
    final var bounds = source.getOffsetBounds(attrs(tool));
    spots = spots(start, at, bounds);
    canvas.repaint();
  }

  /**
   * Where the copies go: along the drag's main axis, one part (plus a grid step) apart, as many
   * as fit between the press and the pointer (always including the press point).
   */
  static List<Location> spots(Location start, Location at, Bounds offsetBounds) {
    final var dx = at.getX() - start.getX();
    final var dy = at.getY() - start.getY();
    final var horizontal = Math.abs(dx) >= Math.abs(dy);
    final var size = horizontal ? offsetBounds.getWidth() : offsetBounds.getHeight();
    final var step = roundUp(size + 10);
    final var span = Math.abs(horizontal ? dx : dy);
    final var count = span / step + 1;
    final var sign = (horizontal ? dx : dy) < 0 ? -1 : 1;
    final var out = new ArrayList<Location>();
    for (var i = 0; i < count; i++) {
      out.add(horizontal
          ? Location.create(start.getX() + sign * i * step, start.getY(), false)
          : Location.create(start.getX(), start.getY() + sign * i * step, false));
    }
    return out;
  }

  private static int roundUp(int v) {
    return (v + 9) / 10 * 10;
  }

  /** The armed part's own attributes (the tool wraps them until the factory is loaded). */
  private static AttributeSet attrs(AddTool tool) {
    final var attrs = tool.getAttributeSet();
    return attrs instanceof FactoryAttributes wrapped ? wrapped.getBase() : attrs;
  }

  private static Location snap(AddTool tool, Location at) {
    final var source = tool.getFactory();
    final var snap = source == null ? null : source.getFeature(ComponentFactory.SHOULD_SNAP,
        attrs(tool));
    if (snap instanceof Boolean b && !b) return at;
    return Location.create(Canvas.snapXToGrid(at.getX()), Canvas.snapYToGrid(at.getY()), false);
  }

  /**
   * The label after {@code label}: a trailing number counts up keeping its width (X0, X1; R09,
   * R10), else a trailing letter moves on (A, B; Z, AA). A label with neither gets a number.
   */
  static String nextLabel(String label) {
    if (label == null || label.isEmpty()) return label;
    var i = label.length();
    while (i > 0 && Character.isDigit(label.charAt(i - 1))) i--;
    if (i < label.length()) {
      final var digits = label.substring(i);
      final var next = String.valueOf(Long.parseLong(digits) + 1);
      final var padded = next.length() < digits.length()
          ? "0".repeat(digits.length() - next.length()) + next : next;
      return label.substring(0, i) + padded;
    }
    final var last = label.charAt(label.length() - 1);
    if ((last >= 'A' && last < 'Z') || (last >= 'a' && last < 'z')) {
      return label.substring(0, label.length() - 1) + (char) (last + 1);
    }
    if (last == 'Z' || last == 'z') {
      final var a = last == 'Z' ? 'A' : 'a';
      return nextLetters(label.substring(0, label.length() - 1), a) + a;
    }
    return label + "1";
  }

  private static String nextLetters(String prefix, char a) {
    if (prefix.isEmpty()) return String.valueOf(a);
    final var last = prefix.charAt(prefix.length() - 1);
    if (Character.isLetter(last) && last != (char) (a + 25)) {
      return prefix.substring(0, prefix.length() - 1) + (char) (last + 1);
    }
    if (last == (char) (a + 25)) return nextLetters(prefix.substring(0, prefix.length() - 1), a) + a;
    return prefix + a;
  }

  public void cancel() {
    tool = null;
    start = null;
    spots = List.of();
    canvas.repaint();
  }

  /** Adds the copies as one action; the press location gets the armed part itself. */
  public void place() {
    final var armed = tool;
    final var where = spots;
    cancel();
    if (armed == null || where.size() < 2) return;
    final var circ = canvas.getCircuit();
    final var project = canvas.getProject();
    if (!project.getLogisimFile().contains(circ)) {
      canvas.setErrorMessage(S.getter("forkCannotModify"));
      return;
    }
    final var source = armed.getFactory();
    if (source == null) return;
    final var base = attrs(armed);
    var label = base.containsAttribute(StdAttr.LABEL) ? base.getValue(StdAttr.LABEL) : null;
    final var xn = new CircuitMutation(circ);
    for (final var loc : where) {
      final var attrs = (AttributeSet) base.clone();
      if (label != null && !label.isEmpty()) {
        attrs.setValue(StdAttr.LABEL, label);
        label = nextLabel(label);
      }
      final var comp = source.createComponent(loc, attrs);
      if (circ.hasConflict(comp)) {
        canvas.setErrorMessage(S.getter("forkStampConflict"));
        return;
      }
      final var b = comp.getBounds();
      if (b.getX() < 0 || b.getY() < 0) {
        canvas.setErrorMessage(S.getter("forkStampNegative"));
        return;
      }
      xn.add(comp);
    }
    project.doAction(xn.toAction(S.getter("forkStampAction", String.valueOf(where.size()))));
    // The next part placed by hand continues the sequence.
    if (label != null && base.containsAttribute(StdAttr.LABEL)) base.setValue(StdAttr.LABEL, label);
  }

  /** Outlines where each copy will go. */
  public void paint(Graphics2D g) {
    if (!isStamping()) return;
    final var source = tool.getFactory();
    if (source == null) return;
    final var b = source.getOffsetBounds(attrs(tool));
    final var g2 = (Graphics2D) g.create();
    try {
      g2.setColor(ForkColors.accent());
      g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
          new float[] {5f, 3f}, 0f));
      for (final var loc : spots) {
        g2.drawRoundRect(loc.getX() + b.getX(), loc.getY() + b.getY(), b.getWidth(),
            b.getHeight(), 6, 6);
      }
      g2.drawString("× " + spots.size(), spots.get(spots.size() - 1).getX() + b.getX(),
          spots.get(spots.size() - 1).getY() + b.getY() + b.getHeight() + 14);
    } finally {
      g2.dispose();
    }
  }
}
