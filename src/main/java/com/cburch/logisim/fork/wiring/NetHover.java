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
import com.cburch.logisim.circuit.WireSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.gui.main.Canvas;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only wiring feedback: highlights the net under the pointer (using Logisim's own
 * wire-highlight drawing) with rings on its pins, and marks unconnected pins and dangling wire
 * ends. Nothing here changes the circuit.
 */
public final class NetHover {
  private static final Color NET_PIN = new Color(0x1E, 0x6B, 0xB8);
  private static final Color UNCONNECTED = new Color(0xD3, 0x2F, 0x2F);

  private final Canvas canvas;
  private Wire hovered;
  private List<Location> netPins = List.of();

  public NetHover(Canvas canvas) {
    this.canvas = canvas;
  }

  /** Called on pointer moves (circuit coordinates); {@code enabled} if the tool allows it. */
  public void update(Location at, double zoom, boolean enabled) {
    final var circuit = canvas.getCircuit();
    if (!enabled || circuit == null || !ForkPreferences.NET_HIGHLIGHT.isEnabled()) {
      clear();
      return;
    }
    final var wire = PinFinder.wireNear(circuit, at, (int) Math.max(3, Math.round(5 / zoom)));
    if (wire == null) {
      clear();
      return;
    }
    if (hovered != null && circuit.getWireSet(hovered).containsWire(wire)) return; // same net
    hovered = wire;
    final var set = circuit.getWireSet(wire);
    canvas.setHighlightedWires(set);
    netPins = pinsOf(circuit, set);
    canvas.repaint();
  }

  public void clear() {
    if (hovered == null) return;
    hovered = null;
    netPins = List.of();
    canvas.setHighlightedWires(WireSet.EMPTY);
    canvas.repaint();
  }

  private static List<Location> pinsOf(Circuit circuit, WireSet set) {
    final var pins = new ArrayList<Location>();
    for (final var w : circuit.getWires()) {
      if (!set.containsWire(w)) continue;
      for (final var end : new Location[] {w.getEnd0(), w.getEnd1()}) {
        if (!circuit.getNonWires(end).isEmpty() && !pins.contains(end)) pins.add(end);
      }
    }
    return pins;
  }

  /** Rings on the pins of the hovered net. */
  public void paint(Graphics2D g) {
    if (hovered == null || netPins.isEmpty()) return;
    final var g2 = (Graphics2D) g.create();
    try {
      g2.setColor(NET_PIN);
      g2.setStroke(new BasicStroke(2f));
      for (final var p : netPins) g2.drawOval(p.getX() - 5, p.getY() - 5, 10, 10);
    } finally {
      g2.dispose();
    }
  }

  /** Red markers on unconnected pins (ring) and dangling wire ends (cross), within {@code clip}. */
  public static void paintUnconnected(Graphics2D g, Circuit circuit, Rectangle clip) {
    if (circuit == null || !ForkPreferences.SHOW_UNCONNECTED.isEnabled()) return;
    final var graph = WireGraph.of(circuit);
    final var g2 = (Graphics2D) g.create();
    try {
      g2.setColor(UNCONNECTED);
      g2.setStroke(new BasicStroke(1.5f));
      for (final var comp : circuit.getNonWires()) {
        for (final var end : comp.getEnds()) {
          final var p = end.getLocation();
          if ((clip == null || clip.contains(p.getX(), p.getY())) && graph.isUnconnectedPin(p)) {
            g2.drawOval(p.getX() - 4, p.getY() - 4, 8, 8);
          }
        }
      }
      for (final var s : graph.getSegments()) {
        for (final var p : new Location[] {s.a(), s.b()}) {
          if ((clip == null || clip.contains(p.getX(), p.getY())) && graph.isDanglingEnd(p)) {
            g2.drawLine(p.getX() - 4, p.getY() - 4, p.getX() + 4, p.getY() + 4);
            g2.drawLine(p.getX() - 4, p.getY() + 4, p.getX() + 4, p.getY() - 4);
          }
        }
      }
    } finally {
      g2.dispose();
    }
  }
}
