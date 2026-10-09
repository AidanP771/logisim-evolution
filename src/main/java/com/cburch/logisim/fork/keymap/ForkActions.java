/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.keymap;

import com.cburch.logisim.fork.ForkFlag;
import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.fork.edit.EditActions;
import com.cburch.logisim.fork.keymap.ForkAction.Category;
import com.cburch.logisim.fork.keymap.ForkAction.Scope;
import com.cburch.logisim.fork.nav.ViewActions;
import com.cburch.logisim.fork.tools.ToolActions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Every fork command, with its default bindings. The order here is the order of the cheat sheet.
 *
 * <p>Default keys follow the conflict resolutions in {@code docs/ux-audit.md} section 6.
 */
public final class ForkActions {
  private static final List<ForkAction> ALL = new ArrayList<>();
  private static final Predicate<ActionContext> ALWAYS = ctx -> true;

  private ForkActions() {}

  /** Adds an action; feature code calls this from its static setup (see {@link #all()}). */
  static ForkAction register(
      String id,
      Category category,
      Scope scope,
      ForkFlag flag,
      List<String> keys,
      Predicate<ActionContext> available,
      Consumer<ActionContext> handler) {
    final var action = new ForkAction(id, category, scope, flag, keys, available, handler);
    ALL.add(action);
    return action;
  }

  private static void canvas(
      String id, Category cat, ForkFlag flag, List<String> keys, Consumer<ActionContext> handler) {
    register(id, cat, Scope.CANVAS, flag, keys, ALWAYS, handler);
  }

  private static void canvas(
      String id,
      Category cat,
      ForkFlag flag,
      List<String> keys,
      Predicate<ActionContext> available,
      Consumer<ActionContext> handler) {
    register(id, cat, Scope.CANVAS, flag, keys, available, handler);
  }

  private static void info(String id, Category cat, ForkFlag flag, String... gestures) {
    register(id, cat, Scope.INFO, flag, List.of(gestures), ALWAYS, null);
  }

  static {
    // --- View -------------------------------------------------------------------------------
    canvas("view.zoomIn", Category.VIEW, ForkPreferences.ZOOM_TO_CURSOR,
        List.of("Mod+=", "Mod+Shift+=", "Mod++", "Mod+NumpadAdd", "Ctrl+=", "Ctrl+Shift+=",
            "Ctrl+NumpadAdd"),
        ctx -> ViewActions.zoomStep(ctx.canvas(), true, ctx.canvas().getMousePosition()));
    canvas("view.zoomOut", Category.VIEW, ForkPreferences.ZOOM_TO_CURSOR,
        List.of("Mod+-", "Mod+NumpadSubtract", "Ctrl+-", "Ctrl+NumpadSubtract"),
        ctx -> ViewActions.zoomStep(ctx.canvas(), false, ctx.canvas().getMousePosition()));
    canvas("view.fit", Category.VIEW, ForkPreferences.FIT_KEYS, List.of("F"),
        ctx -> ViewActions.fitAll(ctx.canvas()));
    canvas("view.fitSelection", Category.VIEW, ForkPreferences.FIT_KEYS, List.of("Shift+F"),
        ctx -> ViewActions.fitSelection(ctx.canvas()));
    canvas("view.resetZoom", Category.VIEW, ForkPreferences.RESET_ZOOM,
        List.of("Mod+0", "Mod+Numpad0"), ctx -> ViewActions.resetZoom(ctx.canvas()));
    info("view.pan", Category.VIEW, ForkPreferences.SPACE_PAN, "Space + drag");
    info("view.panMiddle", Category.VIEW, ForkPreferences.MIDDLE_PAN,
        "Middle-drag", "Cmd + drag (macOS)");
    info("view.zoomWheel", Category.VIEW, ForkPreferences.ZOOM_TO_CURSOR, "Mod + scroll", "Pinch");

    // --- Navigate ---------------------------------------------------------------------------
    canvas("nav.parent", Category.NAVIGATE, ForkPreferences.BACK_TO_PARENT, List.of("Alt+Up"),
        ctx -> !ctx.hasSelection(), ctx -> ViewActions.backToParent(ctx.project()));
    info("nav.switchCircuit", Category.NAVIGATE, ForkPreferences.CIRCUIT_SWITCHER,
        "Ctrl + Tab", "Ctrl + Shift + Tab");

    // --- Tools ------------------------------------------------------------------------------
    canvas("tool.select", Category.TOOLS, ForkPreferences.SINGLE_KEY_TOOLS, List.of("Esc"),
        ToolActions::canSelectToolOrDeselect, ToolActions::selectToolOrDeselect);
    canvas("tool.wire", Category.TOOLS, ForkPreferences.SINGLE_KEY_TOOLS, List.of("W"),
        ctx -> ToolActions.arm(ctx, "Wiring Tool"));
    canvas("tool.and", Category.TOOLS, ForkPreferences.SINGLE_KEY_TOOLS, List.of("A"),
        ctx -> ToolActions.arm(ctx, "AND Gate"));
    canvas("tool.or", Category.TOOLS, ForkPreferences.SINGLE_KEY_TOOLS, List.of("O"),
        ctx -> ToolActions.arm(ctx, "OR Gate"));
    canvas("tool.not", Category.TOOLS, ForkPreferences.SINGLE_KEY_TOOLS, List.of("N"),
        ctx -> ToolActions.arm(ctx, "NOT Gate"));
    canvas("tool.xor", Category.TOOLS, ForkPreferences.SINGLE_KEY_TOOLS, List.of("X"),
        ctx -> ToolActions.arm(ctx, "XOR Gate"));
    canvas("tool.nand", Category.TOOLS, ForkPreferences.SINGLE_KEY_TOOLS, List.of("Shift+A"),
        ctx -> ToolActions.arm(ctx, "NAND Gate"));
    canvas("tool.nor", Category.TOOLS, ForkPreferences.SINGLE_KEY_TOOLS, List.of("Shift+O"),
        ctx -> ToolActions.arm(ctx, "NOR Gate"));
    canvas("tool.inputPin", Category.TOOLS, ForkPreferences.SINGLE_KEY_TOOLS, List.of("I"),
        ctx -> ToolActions.armPin(ctx, false));
    canvas("tool.outputPin", Category.TOOLS, ForkPreferences.SINGLE_KEY_TOOLS, List.of("P"),
        ctx -> ToolActions.armPin(ctx, true));
    canvas("tool.text", Category.TOOLS, ForkPreferences.SINGLE_KEY_TOOLS, List.of("T"),
        ctx -> ToolActions.arm(ctx, "Text Tool"));
    info("tool.inputCount", Category.TOOLS, null, "2 … 9 (gate selected or armed)");
    info("tool.bitWidth", Category.TOOLS, null, "Alt + 1 … 9 (bit width)");

    // --- Edit -------------------------------------------------------------------------------
    canvas("edit.rotateCw", Category.EDIT, ForkPreferences.ROTATE_KEYS, List.of("R"),
        EditActions::canRotate, ctx -> EditActions.rotate(ctx, true));
    canvas("edit.rotateCcw", Category.EDIT, ForkPreferences.ROTATE_KEYS, List.of("Shift+R"),
        EditActions::canRotate, ctx -> EditActions.rotate(ctx, false));
    canvas("edit.nudgeLeft", Category.EDIT, ForkPreferences.NUDGE, List.of("Left"),
        EditActions::canNudge, ctx -> EditActions.nudge(ctx, -1, 0));
    canvas("edit.nudgeRight", Category.EDIT, ForkPreferences.NUDGE, List.of("Right"),
        EditActions::canNudge, ctx -> EditActions.nudge(ctx, 1, 0));
    canvas("edit.nudgeUp", Category.EDIT, ForkPreferences.NUDGE, List.of("Up"),
        EditActions::canNudge, ctx -> EditActions.nudge(ctx, 0, -1));
    canvas("edit.nudgeDown", Category.EDIT, ForkPreferences.NUDGE, List.of("Down"),
        EditActions::canNudge, ctx -> EditActions.nudge(ctx, 0, 1));
    canvas("edit.nudgeLeftBig", Category.EDIT, ForkPreferences.NUDGE, List.of("Shift+Left"),
        EditActions::canNudge, ctx -> EditActions.nudge(ctx, -5, 0));
    canvas("edit.nudgeRightBig", Category.EDIT, ForkPreferences.NUDGE, List.of("Shift+Right"),
        EditActions::canNudge, ctx -> EditActions.nudge(ctx, 5, 0));
    canvas("edit.nudgeUpBig", Category.EDIT, ForkPreferences.NUDGE, List.of("Shift+Up"),
        EditActions::canNudge, ctx -> EditActions.nudge(ctx, 0, -5));
    canvas("edit.nudgeDownBig", Category.EDIT, ForkPreferences.NUDGE, List.of("Shift+Down"),
        EditActions::canNudge, ctx -> EditActions.nudge(ctx, 0, 5));
    info("edit.duplicate", Category.EDIT, ForkPreferences.DUPLICATE_OFFSET, "Mod + D");
  }

  /** Every registered action, in cheat-sheet order. */
  public static List<ForkAction> all() {
    FeatureActions.ensureRegistered();
    return Collections.unmodifiableList(ALL);
  }

  /** The action with {@code id}, or null. */
  public static ForkAction get(String id) {
    for (final var action : all()) {
      if (action.getId().equals(id)) return action;
    }
    return null;
  }
}
