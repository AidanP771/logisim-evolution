/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.palette;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

/**
 * Remembers whether the last Enter was pressed with Shift. The search dialog runs a result's
 * action after it closes, without passing the key event on, so this is how a circuit result tells
 * Enter (open) from Shift+Enter (place as subcircuit). Observes only; never consumes.
 */
final class PaletteKeys implements KeyEventDispatcher {
  private static PaletteKeys instance;
  private static volatile boolean lastEnterShift;

  private PaletteKeys() {}

  static synchronized void install() {
    if (instance != null) return;
    instance = new PaletteKeys();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(instance);
  }

  static boolean lastEnterHadShift() {
    return lastEnterShift;
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent e) {
    if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ENTER) {
      lastEnterShift = e.isShiftDown();
    } else if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() != KeyEvent.VK_SHIFT) {
      // Any other key means the next activation is not a Shift+Enter (e.g. a mouse double-click).
      lastEnterShift = false;
    }
    return false;
  }
}
