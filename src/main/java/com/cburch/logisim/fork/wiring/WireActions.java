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
import com.cburch.logisim.fork.edit.CircuitEditAction;
import com.cburch.logisim.fork.keymap.ActionContext;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.proj.Project;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Wire clean-up and deletion helpers. Every change is one undoable action. */
public final class WireActions {
  /** Colour for informational canvas messages (Logisim's own message area). */
  static final Color INFO_COLOR = new Color(0x1E, 0x6B, 0xB8);

  private WireActions() {}

  /**
   * "Clean up wires": removes wire stubs that lead nowhere. Merging touching straight segments and
   * dropping zero-length ones already happens on every edit (Logisim's WireRepair). The result is
   * checked before anything changes: if the pin-to-pin connections would differ, nothing is done.
   */
  public static void cleanUp(ActionContext ctx) {
    final var canvas = ctx.canvas();
    final var circuit = canvas.getCircuit();
    if (!ctx.project().getLogisimFile().contains(circuit)) {
      canvas.setErrorMessage(S.getter("forkCannotModify"));
      return;
    }
    final var graph = WireGraph.of(circuit);
    final var stubs = graph.danglingStubs();
    if (stubs.isEmpty()) {
      canvas.setErrorMessage(S.getter("forkCleanupNothing"), INFO_COLOR);
      return;
    }
    if (!graph.pinNets().equals(graph.without(stubs).pinNets())) {
      // Should be impossible; refuse rather than risk changing the circuit.
      canvas.setErrorMessage(S.getter("forkCleanupRefused"));
      return;
    }
    final var xn = new CircuitMutation(circuit);
    xn.removeAll(stubs);
    ctx.project().doAction(xn.toAction(S.getter("forkCleanupAction")));
    canvas.setErrorMessage(S.getter("forkCleanupDone", String.valueOf(stubs.size())), INFO_COLOR);
  }

  /** Alt+click: deletes the run of wire through {@code wire} between junctions, pins or ends. */
  public static void deleteSegment(Canvas canvas, Project project, Wire wire) {
    final var circuit = canvas.getCircuit();
    if (!project.getLogisimFile().contains(circuit)) {
      canvas.setErrorMessage(S.getter("forkCannotModify"));
      return;
    }
    final var chain = WireGraph.of(circuit).chainThrough(wire);
    if (chain.isEmpty()) return;
    final var xn = new CircuitMutation(circuit);
    xn.removeAll(chain);
    project.doAction(xn.toAction(S.getter("forkDeleteSegmentAction")));
  }

  /**
   * Deletes the selection and, in the same undo step, wires left leading only to the deleted
   * components. Wires that were already dangling elsewhere are left alone.
   *
   * @return true if handled; false lets Logisim's own delete run
   */
  public static boolean deleteSelection(Canvas canvas, Project project) {
    if (!ForkPreferences.DELETE_DANGLING.isEnabled()) return false;
    final var sel = canvas.getSelection();
    final var pins = new LinkedHashSet<Location>();
    for (final var comp : sel.getComponents()) {
      if (comp instanceof Wire) continue;
      for (final var end : comp.getEnds()) pins.add(end.getLocation());
    }
    if (pins.isEmpty()) return false;
    project.doAction(SelectionActions.clear(sel).append(new RemoveNewlyDangling(canvas, pins)));
    return true;
  }

  public static boolean canDeleteSelection(ActionContext ctx) {
    if (!ctx.isEditTool() || !ctx.hasSelection()) return false;
    for (final var comp : ctx.canvas().getSelection().getComponents()) {
      if (!(comp instanceof Wire)) return true;
    }
    return false;
  }

  /** Runs after the delete, so it sees which of the old pin locations now end in mid-air. */
  private static final class RemoveNewlyDangling extends CircuitEditAction {
    private final Canvas canvas;
    private final List<Location> pins;

    RemoveNewlyDangling(Canvas canvas, LinkedHashSet<Location> pins) {
      super(S.getter("forkDeleteAction"));
      this.canvas = canvas;
      this.pins = new ArrayList<>(pins);
    }

    @Override
    protected Circuit circuit(Project proj) {
      return canvas.getCircuit();
    }

    @Override
    protected void build(Circuit circuit, CircuitMutation xn) {
      final var graph = WireGraph.of(circuit);
      final var remove = new LinkedHashSet<Wire>();
      for (final var loc : pins) {
        if (!graph.isDanglingEnd(loc)) continue;
        for (final var w : circuit.getWires(loc)) remove.addAll(graph.chainThrough(w));
      }
      xn.removeAll(remove);
    }
  }
}
