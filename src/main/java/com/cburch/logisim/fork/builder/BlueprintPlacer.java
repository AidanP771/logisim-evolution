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
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fork.ForkColors;
import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.fork.keymap.ActionContext;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.util.StringUtil;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JOptionPane;

/**
 * Saving the selection as a blueprint, and placing one: once armed from the palette, a ghost of
 * the blueprint follows the pointer; a click places every part and wire in one undo step, Esc or
 * right-click cancels. Held on the canvas as a client property, so the palette and the canvas
 * input handler share it.
 */
public final class BlueprintPlacer {
  private static final String KEY = "fork.blueprintPlacer";

  private final Blueprint blueprint;
  private final Blueprint.Placement placement;
  private Location at;

  private BlueprintPlacer(Blueprint blueprint, Blueprint.Placement placement) {
    this.blueprint = blueprint;
    this.placement = placement;
  }

  /** The blueprint armed on {@code canvas}, or null. */
  public static BlueprintPlacer of(Canvas canvas) {
    return canvas.getClientProperty(KEY) instanceof BlueprintPlacer p ? p : null;
  }

  /** Arms {@code blueprint} on {@code canvas}, or explains why its parts can't be found. */
  public static void arm(Canvas canvas, Blueprint blueprint) {
    final Blueprint.Placement placement;
    try {
      placement = blueprint.resolve(canvas.getProject().getLogisimFile());
    } catch (IllegalArgumentException e) {
      canvas.setErrorMessage(StringUtil.constantGetter(e.getMessage()));
      return;
    }
    canvas.putClientProperty(KEY, new BlueprintPlacer(blueprint, placement));
    if (ForkPreferences.STATUS_HINTS.isEnabled()) {
      canvas.setErrorMessage(S.getter("forkHintBlueprint", blueprint.name()), ForkColors.accent());
    }
    canvas.requestFocusInWindow();
  }

  public static void disarm(Canvas canvas) {
    if (of(canvas) == null) return;
    canvas.putClientProperty(KEY, null);
    if (ForkPreferences.STATUS_HINTS.isEnabled()) canvas.setErrorMessage(null);
    canvas.repaint();
  }

  /** The pointer moved to {@code pointer} (circuit coordinates). */
  public void move(Canvas canvas, Location pointer) {
    final var snapped = Location.create(Canvas.snapXToGrid(pointer.getX()),
        Canvas.snapYToGrid(pointer.getY()), false);
    if (snapped.equals(at)) return;
    at = snapped;
    canvas.repaint();
  }

  /** Places the blueprint with its origin at {@code pointer}; stays armed on failure. */
  public void place(Canvas canvas, Location pointer) {
    move(canvas, pointer);
    final var circuit = canvas.getCircuit();
    final var project = canvas.getProject();
    if (circuit == null || !project.getLogisimFile().contains(circuit)) {
      canvas.setErrorMessage(S.getter("forkCannotModify"));
      return;
    }
    final var xn = new CircuitMutation(circuit);
    for (final var part : placement.parts()) {
      if (part.factory() instanceof SubcircuitFactory sub
          && (sub.getSubcircuit() == circuit
              || !project.getDependencies().canAdd(circuit, sub.getSubcircuit()))) {
        canvas.setErrorMessage(S.getter("forkBlueprintRecursive"));
        return;
      }
      final var loc = Location.create(at.getX() + part.dx(), at.getY() + part.dy(), false);
      final Component comp = part.factory().createComponent(loc,
          (AttributeSet) part.attrs().clone());
      if (circuit.hasConflict(comp)) {
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
    for (final var w : placement.wires()) {
      final var ends = Blueprint.place(w, at);
      if (ends[0].getX() < 0 || ends[0].getY() < 0 || ends[1].getX() < 0 || ends[1].getY() < 0) {
        canvas.setErrorMessage(S.getter("forkStampNegative"));
        return;
      }
      xn.add(Wire.create(ends[0], ends[1]));
    }
    disarm(canvas);
    project.doAction(xn.toAction(S.getter("forkBlueprintPlaceAction", blueprint.name())));
  }

  /** Outlines of the parts and the wires, at the pointer. */
  public void paint(Graphics2D g) {
    if (at == null) return;
    final var g2 = (Graphics2D) g.create();
    try {
      g2.setColor(ForkColors.accent());
      g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
          new float[] {5f, 3f}, 0f));
      for (final var part : placement.parts()) {
        final var b = part.factory().getOffsetBounds(part.attrs());
        g2.drawRoundRect(at.getX() + part.dx() + b.getX(), at.getY() + part.dy() + b.getY(),
            b.getWidth(), b.getHeight(), 6, 6);
      }
      g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      for (final var w : placement.wires()) {
        final var ends = Blueprint.place(w, at);
        g2.drawLine(ends[0].getX(), ends[0].getY(), ends[1].getX(), ends[1].getY());
      }
    } finally {
      g2.dispose();
    }
  }

  /** Whether "Save selection as blueprint" applies: something is selected. */
  public static boolean canSave(ActionContext ctx) {
    return ctx.canvas() != null && !ctx.canvas().getSelection().isEmpty();
  }

  /** Asks for a name and saves the selection as a blueprint. */
  public static void saveSelection(ActionContext ctx) {
    final var canvas = ctx.canvas();
    final var selection = new ArrayList<>(canvas.getSelection().getComponents());
    if (selection.isEmpty()) return;
    final var name = JOptionPane.showInputDialog(ctx.frame(), S.get("forkBlueprintNamePrompt"),
        S.get("forkBlueprintSaveTitle"), JOptionPane.QUESTION_MESSAGE);
    if (name == null || name.isBlank()) return;
    if (BlueprintStore.exists(name)) {
      final var answer = JOptionPane.showConfirmDialog(ctx.frame(),
          S.get("forkBlueprintOverwrite", name.strip()), S.get("forkBlueprintSaveTitle"),
          JOptionPane.YES_NO_OPTION);
      if (answer != JOptionPane.YES_OPTION) return;
    }
    try {
      final var blueprint = Blueprint.capture(name.strip(), selection,
          ctx.project().getLogisimFile());
      final var file = BlueprintStore.save(blueprint);
      canvas.setErrorMessage(S.getter("forkBlueprintSaved", name.strip(), file.toString()),
          ForkColors.accent());
    } catch (IllegalArgumentException | IOException e) {
      canvas.setErrorMessage(StringUtil.constantGetter(e.getMessage()));
    }
  }
}
