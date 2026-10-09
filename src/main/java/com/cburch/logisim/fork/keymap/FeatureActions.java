/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.keymap;

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

    // Feature packages register their actions here.
  }
}
