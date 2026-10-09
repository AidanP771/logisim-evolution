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
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.EditTool;

/** What a {@link ForkAction} acts on: the main window, its project and its circuit canvas. */
public record ActionContext(Frame frame, Project project, Canvas canvas) {
  public static ActionContext of(Frame frame) {
    return new ActionContext(frame, frame.getProject(), frame.getCanvas());
  }

  public boolean hasSelection() {
    return canvas != null && !canvas.getSelection().isEmpty();
  }

  public boolean isEditTool() {
    return project.getTool() instanceof EditTool;
  }
}
