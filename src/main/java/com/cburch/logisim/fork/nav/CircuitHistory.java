/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.nav;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Remembers which circuits of a project were viewed, most recent first. */
public final class CircuitHistory implements ProjectListener {
  private static final int CAPACITY = 32;
  private static final Map<Project, CircuitHistory> HISTORIES = new WeakHashMap<>();

  private final Project project;
  private final MruList<Circuit> recent = new MruList<>(CAPACITY);

  private CircuitHistory(Project project) {
    this.project = project;
    recent.touch(project.getCurrentCircuit());
    project.addProjectListener(this);
  }

  /** The history for {@code project}, created (and starting to record) on first use. */
  public static synchronized CircuitHistory forProject(Project project) {
    return HISTORIES.computeIfAbsent(project, CircuitHistory::new);
  }

  @Override
  public void projectChanged(ProjectEvent event) {
    if (event.getAction() == ProjectEvent.ACTION_SET_CURRENT
        && event.getData() instanceof Circuit circuit) {
      recent.touch(circuit);
    }
  }

  /** Circuits still in the project, most recently viewed first. */
  public List<Circuit> getRecent() {
    return recent.items(c -> project.getLogisimFile() != null && project.getLogisimFile().contains(c));
  }
}
