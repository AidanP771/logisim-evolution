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
import com.cburch.logisim.fork.keymap.ActionContext;
import com.cburch.logisim.fork.keymap.ForkAction;
import com.cburch.logisim.fork.keymap.ForkActions;
import com.cburch.logisim.fork.keymap.KeymapStore;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.search.FuzzyMatcher;
import com.cburch.logisim.gui.search.SearchCandidate;
import com.cburch.logisim.gui.search.SearchContext;
import com.cburch.logisim.gui.search.SearchProvider;
import com.cburch.logisim.gui.search.SearchQuery;
import com.cburch.logisim.gui.search.SearchResult;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Fork commands (fit, align, clean up wires...) with their current shortcuts as hints. */
final class ActionProvider implements SearchProvider {
  private final List<SearchCandidate> candidates = new ArrayList<>();

  @Override
  public String getDisplayName() {
    return S.get("forkPaletteActions");
  }

  @Override
  public boolean isAvailable(SearchContext context) {
    return ForkPreferences.PALETTE.isEnabled() && context.owner() instanceof Frame;
  }

  @Override
  public void prepare(SearchContext context) {
    candidates.clear();
    if (!(context.owner() instanceof Frame frame)) return;
    final var keymap = KeymapStore.get();
    for (final var action : ForkActions.all()) {
      if (!action.isRemappable() || action.getId().equals("palette.open")) continue;
      final var hint =
          keymap.getBindings(action).stream().map(b -> b.display()).collect(Collectors.joining("  "));
      final var ctx = ActionContext.of(frame);
      candidates.add(
          new SearchCandidate(
              action.getLabel(), action.getCategory().getLabel(), null, hint,
              action.isAvailable(ctx), () -> run(action, frame)));
    }
  }

  private static void run(ForkAction action, Frame frame) {
    final var ctx = ActionContext.of(frame);
    if (action.isAvailable(ctx)) {
      action.run(ctx);
    } else {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  @Override
  public List<SearchResult> search(SearchQuery query) {
    final var parsed = PaletteQuery.parse(query.text());
    if (!parsed.scope().includesActions()) return List.of();
    if (parsed.text().isEmpty() && parsed.scope() != PaletteQuery.Scope.ACTIONS) return List.of();
    final var results = new ArrayList<SearchResult>();
    for (final var candidate : candidates) {
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
