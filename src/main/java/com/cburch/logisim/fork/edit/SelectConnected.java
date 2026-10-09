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
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.SelectionActions;
import java.util.LinkedHashSet;

/** Double-click selection of whole nets, and of a component together with what it is wired to. */
public final class SelectConnected {
  private SelectConnected() {}

  /** Double-click a wire: selects every segment of its net (across junctions, not through parts). */
  public static void selectNet(Canvas canvas, Wire wire) {
    final var circuit = canvas.getCircuit();
    final var set = circuit.getWireSet(wire);
    final var net = new LinkedHashSet<Component>();
    for (final var w : circuit.getWires()) {
      if (set.containsWire(w)) net.add(w);
    }
    replaceSelection(canvas, net);
  }

  /** Shift+double-click a component: selects it, the wires on its pins and the parts at their ends. */
  public static void selectWithNeighbours(Canvas canvas, Component comp) {
    final var circuit = canvas.getCircuit();
    final var picked = new LinkedHashSet<Component>();
    picked.add(comp);
    for (final var end : comp.getEnds()) {
      for (final var w : circuit.getWires(end.getLocation())) {
        final var set = circuit.getWireSet(w);
        for (final var other : circuit.getWires()) {
          if (!set.containsWire(other)) continue;
          picked.add(other);
          for (final var at : new com.cburch.logisim.data.Location[] {other.getEnd0(), other.getEnd1()}) {
            picked.addAll(circuit.getNonWires(at));
          }
        }
      }
      // Parts plugged straight into this pin, with no wire between.
      picked.addAll(circuit.getNonWires(end.getLocation()));
    }
    replaceSelection(canvas, picked);
  }

  private static void replaceSelection(Canvas canvas, java.util.Collection<Component> comps) {
    final var project = canvas.getProject();
    final var drop = SelectionActions.dropAll(canvas.getSelection());
    if (drop != null) project.doAction(drop);
    canvas.getSelection().addAll(comps);
    canvas.repaint();
  }
}
