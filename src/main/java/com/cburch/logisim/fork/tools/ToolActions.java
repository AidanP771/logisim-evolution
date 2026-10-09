/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.tools;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.fork.keymap.ActionContext;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.EditTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Single-key tool selection ("arming") for the fork keymap. */
public final class ToolActions {
  /** Per-project input/output pin tools, so attribute tweaks (e.g. width) persist in a session. */
  private static final Map<Project, Map<Boolean, AddTool>> PIN_TOOLS = new WeakHashMap<>();

  private ToolActions() {}

  /** Finds a tool by its internal name (e.g. "AND Gate", "Wiring Tool") in the project's libraries. */
  public static Tool findTool(Project project, String name) {
    if (project == null || project.getLogisimFile() == null) return null;
    final var visited = new IdentityHashMap<Library, Boolean>();
    for (final var lib : project.getLogisimFile().getLibraries()) {
      final var found = findTool(lib, name, visited);
      if (found != null) return found;
    }
    return null;
  }

  private static Tool findTool(Library lib, String name, Map<Library, Boolean> visited) {
    if (visited.put(lib, Boolean.TRUE) != null) return null;
    final var tool = lib.getTool(name);
    if (tool != null) return tool;
    for (final var child : lib.getLibraries()) {
      final var found = findTool(child, name, visited);
      if (found != null) return found;
    }
    return null;
  }

  /** Arms the named library tool, as if it had been clicked in the component tree. */
  public static void arm(ActionContext ctx, String toolName) {
    final var tool = findTool(ctx.project(), toolName);
    if (tool != null) ctx.project().setTool(tool);
  }

  /** Arms an input ({@code output == false}) or output pin. */
  public static void armPin(ActionContext ctx, boolean output) {
    final var project = ctx.project();
    final var tools = PIN_TOOLS.computeIfAbsent(project, p -> new IdentityHashMap<>());
    var tool = tools.get(output);
    if (tool == null) {
      final var base = findTool(project, "Pin");
      if (!(base instanceof AddTool addTool)) return;
      tool = (AddTool) addTool.cloneTool();
      final var attrs = tool.getAttributeSet();
      attrs.setValue(Pin.ATTR_TYPE, output ? Pin.OUTPUT : Pin.INPUT);
      if (output && attrs.containsAttribute(StdAttr.FACING)) {
        attrs.setValue(StdAttr.FACING, Direction.WEST);
      }
      tools.put(output, tool);
    }
    project.setTool(tool);
  }

  /** Esc: back to the Edit tool, or (already there) drop the selection. */
  public static void selectToolOrDeselect(ActionContext ctx) {
    final var project = ctx.project();
    if (!(project.getTool() instanceof EditTool)) {
      final var edit = findTool(project, EditTool._ID);
      if (edit != null) project.setTool(edit);
      return;
    }
    final var act = SelectionActions.dropAll(ctx.canvas().getSelection());
    if (act != null) project.doAction(act);
  }

  /** Esc applies when another tool is active or something is selected. */
  public static boolean canSelectToolOrDeselect(ActionContext ctx) {
    return !(ctx.project().getTool() instanceof EditTool) || ctx.hasSelection();
  }
}
