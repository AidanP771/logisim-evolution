/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.keymap;

import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.tools.PokeTool;
import com.cburch.logisim.tools.TextTool;

/** Decides when plain keys belong to text entry rather than to fork shortcuts. */
public final class TypingGuard {
  private TypingGuard() {}

  /** True while the canvas is taking typed text, so plain keys must reach the tool. */
  public static boolean isTyping(Canvas canvas) {
    if (canvas == null) return false;
    final var tool = canvas.getProject().getTool();
    if (tool instanceof TextTool textTool && textTool.isEditing()) return true;
    // PokeTool exposes "has a caret that takes keys" (a pin value, RAM, keyboard...) as isScrollable().
    return tool instanceof PokeTool pokeTool && pokeTool.isScrollable();
  }
}
