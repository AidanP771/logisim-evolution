/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.edit;

import static com.cburch.logisim.fork.Strings.S;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.fork.keymap.ActionContext;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.SetAttributeAction;
import com.cburch.logisim.tools.move.MoveGesture;

/** Keyboard editing of the selection: rotate and nudge. All changes are undoable actions. */
public final class EditActions {
  /** One grid step, in circuit units. */
  public static final int GRID = 10;

  private EditActions() {}

  /** R / Shift+R: rotates the armed component, or every selected component, by 90 degrees. */
  public static void rotate(ActionContext ctx, boolean clockwise) {
    if (ctx.project().getTool() instanceof AddTool addTool) {
      final var attrs = addTool.getAttributeSet();
      if (attrs.containsAttribute(StdAttr.FACING)) {
        final var facing = attrs.getValue(StdAttr.FACING);
        attrs.setValue(StdAttr.FACING, clockwise ? facing.getRight() : facing.getLeft());
        ctx.canvas().repaint();
      }
      return;
    }
    final var circuit = ctx.canvas().getCircuit();
    final var act = new SetAttributeAction(circuit, S.getter("forkRotateAction"));
    for (final var comp : ctx.canvas().getSelection().getComponents()) {
      if (comp instanceof Wire) continue;
      final var attr = facingAttribute(comp);
      final Direction facing = comp.getAttributeSet().getValue(StdAttr.FACING);
      if (attr != null && facing != null) {
        act.set(comp, attr, clockwise ? facing.getRight() : facing.getLeft());
      }
    }
    if (!act.isEmpty()) ctx.project().doAction(act);
  }

  /** Rotation applies to an armed component or a non-empty selection in the Edit tool. */
  public static boolean canRotate(ActionContext ctx) {
    return ctx.project().getTool() instanceof AddTool || (ctx.isEditTool() && ctx.hasSelection());
  }

  /** Same lookup EditTool uses: the attribute a factory treats as "facing". */
  @SuppressWarnings("unchecked")
  static Attribute<Direction> facingAttribute(Component comp) {
    final var attrs = comp.getAttributeSet();
    return (Attribute<Direction>)
        comp.getFactory().getFeature(ComponentFactory.FACING_ATTRIBUTE_KEY, attrs);
  }

  /**
   * Arrow keys: moves the selection by {@code steps} grid units. Attached wires follow the same
   * way they do when dragging (the "keep connections" preference).
   */
  public static void nudge(ActionContext ctx, int stepsX, int stepsY) {
    final var canvas = ctx.canvas();
    final var project = ctx.project();
    final var sel = canvas.getSelection();
    final var dx = stepsX * GRID;
    final var dy = stepsY * GRID;
    if (!project.getLogisimFile().contains(canvas.getCircuit())) {
      canvas.setErrorMessage(S.getter("forkCannotModify"));
      return;
    }
    if (sel.hasConflictWhenMoved(dx, dy)) {
      canvas.setErrorMessage(S.getter("forkMoveConflict"));
      return;
    }
    project.doAction(SelectionActions.translate(sel, dx, dy, connections(canvas, dx, dy)));
  }

  /** Wire rerouting for moving the selection by (dx, dy), or null if connections are not kept. */
  static com.cburch.logisim.circuit.ReplacementMap connections(Canvas canvas, int dx, int dy) {
    if (!AppPreferences.MOVE_KEEP_CONNECT.getBoolean()) return null;
    final var gesture =
        new MoveGesture(
            (g, x, y) -> {
              // Nothing to report: the move is applied straight away.
            },
            canvas.getCircuit(),
            canvas.getSelection().getAnchoredComponents());
    return gesture.forceRequest(dx, dy).getReplacementMap();
  }

  /** Nudging applies to a non-empty selection in the Edit tool. */
  public static boolean canNudge(ActionContext ctx) {
    return ctx.isEditTool() && ctx.hasSelection();
  }
}
