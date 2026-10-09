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
import com.cburch.logisim.fork.gui.CheatSheet;
import com.cburch.logisim.fork.keymap.ForkAction.Category;
import com.cburch.logisim.fork.keymap.ForkAction.Scope;
import java.util.List;

/**
 * Registers the actions of the palette, help, arrange and wiring features. Kept apart from
 * {@link ForkActions} so that registry does not depend on every feature package at class-load time.
 */
final class FeatureActions {
  private static boolean registered;

  private FeatureActions() {}

  static synchronized void ensureRegistered() {
    if (registered) return;
    registered = true;

    // --- Help (window-wide) --------------------------------------------------
    ForkActions.register("help.cheatSheet", Category.HELP, Scope.WINDOW,
        ForkPreferences.CHEAT_SHEET, List.of("?"), ctx -> true, ctx -> CheatSheet.show(ctx.frame()));
  }
}
