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
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fork.wiring.RoutePlan;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.StringUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chain wiring ({@code build.chain}, Mod+Alt+W): the selected parts, ordered left to right (then
 * top to bottom), each get their output wired to the next part's first free input, routed by
 * {@link GridRouter}. All wires are one undo step; if any link can't be made, nothing is added.
 */
public final class ChainWiring {
  private ChainWiring() {}

  /** One planned link: corner points from an output to an input. */
  record Link(Component from, Component to, List<Location> route) {}

  /** Parts in chain order: by left edge, then by top edge. */
  static List<Component> order(List<Component> parts) {
    final var sorted = new ArrayList<>(parts);
    sorted.sort(Comparator.<Component>comparingInt(c -> c.getBounds().getX())
        .thenComparingInt(c -> c.getBounds().getY()));
    return sorted;
  }

  static Location firstOutput(Component c) {
    for (final var end : c.getEnds()) {
      if (end.isOutput() && !end.isInput()) return end.getLocation();
    }
    return null;
  }

  /** The first input with nothing attached (no wire, no other part's pin). */
  static Location firstFreeInput(Circuit circuit, Component c) {
    for (final var end : c.getEnds()) {
      if (!end.isInput() || end.isOutput()) continue;
      final var loc = end.getLocation();
      if (circuit.getWires(loc).isEmpty() && circuit.getComponents(loc).size() <= 1) return loc;
    }
    return null;
  }

  /**
   * Plans every link, each routed around the parts and the links before it. Returns the links,
   * or throws with the message to show when the chain can't be made.
   */
  static List<Link> plan(Circuit circuit, List<Component> parts) {
    final var ordered = order(parts);
    final var links = new ArrayList<Link>();
    final var planned = new ArrayList<Location[]>();
    for (var i = 0; i + 1 < ordered.size(); i++) {
      final var from = ordered.get(i);
      final var to = ordered.get(i + 1);
      final var out = firstOutput(from);
      final var in = firstFreeInput(circuit, to);
      if (out == null) throw new IllegalStateException(S.get("forkChainNoOutput", name(from)));
      if (in == null) throw new IllegalStateException(S.get("forkChainNoInput", name(to)));
      final var grid = CircuitGrid.around(circuit, out, in);
      for (final var seg : planned) grid.addWire(seg[0], seg[1]);
      final var route = GridRouter.route(out, in, grid);
      if (route == null) {
        throw new IllegalStateException(S.get("forkChainNoRoute", name(from), name(to)));
      }
      planned.addAll(RoutePlan.segments(route));
      links.add(new Link(from, to, route));
    }
    return links;
  }

  private static String name(Component c) {
    return c.getFactory().getDisplayName();
  }

  /** Whether the chain command applies: two or more parts selected. */
  public static boolean canChain(Canvas canvas) {
    return selectedParts(canvas).size() >= 2;
  }

  private static List<Component> selectedParts(Canvas canvas) {
    final var parts = new ArrayList<Component>();
    for (final var c : canvas.getSelection().getComponents()) {
      if (!(c instanceof Wire)) parts.add(c);
    }
    return parts;
  }

  /** Mod+Alt+W: wires the selection into a chain, or explains why it can't. */
  public static void chain(Canvas canvas, Project project) {
    final var circuit = canvas.getCircuit();
    if (circuit == null || !project.getLogisimFile().contains(circuit)) {
      canvas.setErrorMessage(S.getter("forkCannotModify"));
      return;
    }
    final var parts = selectedParts(canvas);
    if (parts.size() < 2) {
      canvas.setErrorMessage(S.getter("forkChainNeedTwo"));
      return;
    }
    final List<Link> links;
    try {
      links = plan(circuit, parts);
    } catch (IllegalStateException e) {
      canvas.setErrorMessage(StringUtil.constantGetter(e.getMessage()));
      return;
    }
    final var xn = new CircuitMutation(circuit);
    for (final var link : links) {
      for (final var s : RoutePlan.segments(link.route())) xn.add(Wire.create(s[0], s[1]));
    }
    project.doAction(xn.toAction(S.getter("forkChainAction", String.valueOf(links.size()))));
  }
}
