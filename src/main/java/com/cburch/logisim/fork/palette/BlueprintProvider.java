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

import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.fork.builder.Blueprint;
import com.cburch.logisim.fork.builder.BlueprintPlacer;
import com.cburch.logisim.fork.builder.BlueprintStore;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.search.FuzzyMatcher;
import com.cburch.logisim.gui.search.SearchCandidate;
import com.cburch.logisim.gui.search.SearchContext;
import com.cburch.logisim.gui.search.SearchProvider;
import com.cburch.logisim.gui.search.SearchQuery;
import com.cburch.logisim.gui.search.SearchResult;
import java.util.ArrayList;
import java.util.List;

/** Saved blueprints, listed after {@code #}: Enter arms one for placing on the canvas. */
final class BlueprintProvider implements SearchProvider {
  private final List<Blueprint> blueprints = new ArrayList<>();
  private Frame frame;

  @Override
  public String getDisplayName() {
    return S.get("forkPaletteBlueprints");
  }

  @Override
  public boolean isAvailable(SearchContext context) {
    return ForkPreferences.PALETTE.isEnabled() && ForkPreferences.BLUEPRINTS.isEnabled()
        && context.owner() instanceof Frame;
  }

  @Override
  public void prepare(SearchContext context) {
    frame = context.owner() instanceof Frame f ? f : null;
    blueprints.clear();
  }

  @Override
  public List<SearchResult> search(SearchQuery query) {
    final var parsed = PaletteQuery.parse(query.text());
    if (parsed.scope() != PaletteQuery.Scope.BLUEPRINTS || frame == null) return List.of();
    // Read lazily: only a "#" query touches the disk.
    if (blueprints.isEmpty()) blueprints.addAll(BlueprintStore.list());
    final var context = getDisplayName();
    final var results = new ArrayList<SearchResult>();
    if (blueprints.isEmpty()) {
      results.add(SearchResult.of(SearchCandidate.of(S.get("forkPaletteNoBlueprints"), context,
          false, () -> { }), 0));
      return results;
    }
    for (final var blueprint : blueprints) {
      final var size = S.get("forkPaletteBlueprintHint", String.valueOf(blueprint.parts().size()),
          String.valueOf(blueprint.wires().size()));
      final var candidate = new SearchCandidate(blueprint.name(), context, null, size, true,
          () -> BlueprintPlacer.arm(frame.getCanvas(), blueprint));
      if (parsed.text().isEmpty()) {
        results.add(SearchResult.of(candidate, 0));
        continue;
      }
      final var match = FuzzyMatcher.match(parsed.text(), candidate.displayText());
      if (match != null) results.add(new SearchResult(candidate, match.score(), match.positions()));
    }
    return results;
  }
}
