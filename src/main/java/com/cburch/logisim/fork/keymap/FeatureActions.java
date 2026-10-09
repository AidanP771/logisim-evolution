/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.keymap;

import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.fork.edit.ArrangeActions;
import com.cburch.logisim.fork.edit.ArrangeActions.Edge;
import com.cburch.logisim.fork.gui.CheatSheet;
import com.cburch.logisim.fork.keymap.ForkAction.Category;
import com.cburch.logisim.fork.keymap.ForkAction.Scope;
import com.cburch.logisim.fork.palette.Palette;
import com.cburch.logisim.fork.wiring.WireActions;
import java.util.List;

/**
 * Registers the actions of the palette, help, arrange and wiring features. Kept apart from
 * {@link ForkActions} so that registry does not depend on every feature package at class-load time.
 */
final class FeatureActions {
  private static boolean registered;

  private FeatureActions() {}

  private static void align(String id, String key, Edge edge) {
    ForkActions.register(id, Category.ARRANGE, Scope.CANVAS, ForkPreferences.ALIGN,
        key == null ? List.of() : List.of(key), ArrangeActions::canAlign,
        ctx -> ArrangeActions.align(ctx, edge));
  }

  static synchronized void ensureRegistered() {
    if (registered) return;
    registered = true;

    // --- Help and palette (window-wide) --------------------------------------------------
    ForkActions.register("palette.open", Category.NAVIGATE, Scope.WINDOW, ForkPreferences.PALETTE,
        List.of("Mod+Shift+P"), ctx -> true, ctx -> Palette.open(ctx.frame()));
    ForkActions.register("help.cheatSheet", Category.HELP, Scope.WINDOW,
        ForkPreferences.CHEAT_SHEET, List.of("?"), ctx -> true, ctx -> CheatSheet.show(ctx.frame()));

    // --- Arrange (selection of 2+ / 3+ components) -------------------------------------------
    align("arrange.alignLeft", "Mod+Alt+Left", Edge.LEFT);
    align("arrange.alignRight", "Mod+Alt+Right", Edge.RIGHT);
    align("arrange.alignTop", "Mod+Alt+Up", Edge.TOP);
    align("arrange.alignBottom", "Mod+Alt+Down", Edge.BOTTOM);
    align("arrange.alignCenterX", null, Edge.CENTER_X);
    align("arrange.alignCenterY", null, Edge.CENTER_Y);
    ForkActions.register("arrange.distributeH", Category.ARRANGE, Scope.CANVAS,
        ForkPreferences.ALIGN, List.of(), ArrangeActions::canDistribute,
        ctx -> ArrangeActions.distribute(ctx, true));
    ForkActions.register("arrange.distributeV", Category.ARRANGE, Scope.CANVAS,
        ForkPreferences.ALIGN, List.of(), ArrangeActions::canDistribute,
        ctx -> ArrangeActions.distribute(ctx, false));

    // --- Selection and wiring ---------------------------------------------------------------
    ForkActions.register("edit.delete", Category.EDIT, Scope.CANVAS, ForkPreferences.DELETE_DANGLING,
        List.of("Delete", "Backspace"), WireActions::canDeleteSelection,
        ctx -> WireActions.deleteSelection(ctx.canvas(), ctx.project()));
    ForkActions.register("edit.selectConnected", Category.EDIT, Scope.INFO,
        ForkPreferences.SELECT_CONNECTED, List.of("Double-click a wire", "Shift + double-click a part"),
        ctx -> true, null);
    ForkActions.register("wire.route", Category.WIRING, Scope.INFO, ForkPreferences.CLICK_ROUTE,
        List.of("Click a pin, click bends, click a pin/wire", "Space or / flips, Esc cancels"),
        ctx -> true, null);
    ForkActions.register("wire.segmentDelete", Category.WIRING, Scope.INFO,
        ForkPreferences.SEGMENT_DELETE, List.of("Alt + click a wire"), ctx -> true, null);
    ForkActions.register("wire.cleanup", Category.WIRING, Scope.CANVAS, ForkPreferences.WIRE_CLEANUP,
        List.of(), ctx -> true, WireActions::cleanUp);
    ForkActions.register("view.toggleUnconnected", Category.WIRING, Scope.CANVAS, null, List.of(),
        ctx -> true, ctx -> {
          final var flag = ForkPreferences.SHOW_UNCONNECTED;
          flag.setEnabled(!flag.isEnabled());
          ctx.canvas().repaint();
        });
  }
}
