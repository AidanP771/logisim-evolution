/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.palette;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.tools.AddTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Remembers which components were most recently armed for placing (from the palette, the
 * toolbox, the toolbar or a single-key shortcut), per project and only for this session.
 */
final class Recents {
  private static final int CAPACITY = 12;
  private static final Map<Project, List<String>> BY_PROJECT = new WeakHashMap<>();

  private Recents() {}

  /** Starts recording for {@code project}; safe to call repeatedly. */
  static synchronized void track(Project project) {
    if (BY_PROJECT.containsKey(project)) return;
    BY_PROJECT.put(project, new ArrayList<>());
    project.addProjectListener(
        event -> {
          if (event.getAction() == ProjectEvent.ACTION_SET_TOOL
              && event.getData() instanceof AddTool tool) {
            record(project, tool.getName());
          }
        });
  }

  static synchronized void record(Project project, String toolName) {
    final var list = BY_PROJECT.computeIfAbsent(project, p -> new ArrayList<>());
    list.remove(toolName);
    list.add(0, toolName);
    while (list.size() > CAPACITY) list.remove(list.size() - 1);
  }

  /** Tool names, most recent first. */
  static synchronized List<String> names(Project project) {
    final var list = BY_PROJECT.get(project);
    return list == null ? List.of() : List.copyOf(list);
  }
}
