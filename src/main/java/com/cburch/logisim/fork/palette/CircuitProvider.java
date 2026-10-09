/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.palette;

import static com.cburch.logisim.fork.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.gui.search.FuzzyMatcher;
import com.cburch.logisim.gui.search.SearchCandidate;
import com.cburch.logisim.gui.search.SearchContext;
import com.cburch.logisim.gui.search.SearchProvider;
import com.cburch.logisim.gui.search.SearchQuery;
import com.cburch.logisim.gui.search.SearchResult;
import com.cburch.logisim.proj.Project;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

/** The project's circuits: Enter opens one, Shift+Enter arms it for placing as a subcircuit. */
final class CircuitProvider implements SearchProvider {
  private final List<Circuit> circuits = new ArrayList<>();
  private Project project;

  @Override
  public String getDisplayName() {
    return S.get("forkPaletteCircuits");
  }

  @Override
  public boolean isAvailable(SearchContext context) {
    return ForkPreferences.PALETTE.isEnabled() && context.project() != null;
  }

  @Override
  public void prepare(SearchContext context) {
    project = context.project();
    circuits.clear();
    if (project != null && project.getLogisimFile() != null) {
      circuits.addAll(project.getLogisimFile().getCircuits());
    }
  }

  @Override
  public List<SearchResult> search(SearchQuery query) {
    final var parsed = PaletteQuery.parse(query.text());
    if (!parsed.scope().includesCircuits()) return List.of();
    // Without the @ prefix, an empty query lists components and actions, not circuits.
    if (parsed.text().isEmpty() && parsed.scope() != PaletteQuery.Scope.CIRCUITS) return List.of();
    final var context = getDisplayName();
    final var results = new ArrayList<SearchResult>();
    for (final var circuit : circuits) {
      final var candidate =
          new SearchCandidate(
              circuit.getName(), context, null, S.get("forkPaletteCircuitHint"), true,
              () -> activate(circuit));
      if (parsed.text().isEmpty()) {
        results.add(SearchResult.of(candidate, 0));
        continue;
      }
      final var match = FuzzyMatcher.match(parsed.text(), candidate.displayText());
      if (match != null) results.add(new SearchResult(candidate, match.score(), match.positions()));
    }
    return results;
  }

  private void activate(Circuit circuit) {
    if (!PaletteKeys.lastEnterHadShift()) {
      project.setCurrentCircuit(circuit);
      return;
    }
    // Shift+Enter: place as a subcircuit, unless that would put a circuit inside itself.
    final var tool = project.getLogisimFile().getAddTool(circuit);
    if (tool == null || circuit == project.getCurrentCircuit()
        || !project.getDependencies().canAdd(project.getCurrentCircuit(), circuit)) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    project.setTool(tool);
  }
}
