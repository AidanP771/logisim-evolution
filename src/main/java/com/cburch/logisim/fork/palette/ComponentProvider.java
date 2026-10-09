/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.palette;

import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.gui.search.FuzzyMatcher;
import com.cburch.logisim.gui.search.SearchCandidate;
import com.cburch.logisim.gui.search.SearchContext;
import com.cburch.logisim.gui.search.SearchProvider;
import com.cburch.logisim.gui.search.SearchQuery;
import com.cburch.logisim.gui.search.SearchResult;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;

/**
 * Components in the palette: like the built-in "Add" search, plus a trailing number that sets the
 * obvious attribute ({@link ParameterRule}) and recently used components first on an empty query.
 * With the palette flag off it hands everything to the built-in provider.
 */
final class ComponentProvider implements SearchProvider {
  /** Same weighting the built-in providers use for a match inside the title. */
  private static final int TITLE_MATCH_BONUS = 25;
  /** Recent components outrank everything else on an empty query. */
  private static final int RECENT_BONUS = 1000;

  private final SearchProvider builtIn;
  private final List<Entry> entries = new ArrayList<>();
  private Project project;

  private record Entry(AddTool tool, String title, String path) {
    String displayText() {
      return path.isEmpty() ? title : path + SearchCandidate.CONTEXT_SEPARATOR + title;
    }
  }

  ComponentProvider(SearchProvider builtIn) {
    this.builtIn = builtIn;
  }

  SearchProvider getBuiltIn() {
    return builtIn;
  }

  @Override
  public String getDisplayName() {
    return builtIn.getDisplayName();
  }

  @Override
  public boolean isAvailable(SearchContext context) {
    return context.project() != null;
  }

  @Override
  public void prepare(SearchContext context) {
    builtIn.prepare(context);
    project = context.project();
    entries.clear();
    if (project == null || project.getLogisimFile() == null) return;
    Recents.track(project);
    final Set<Library> visiting = Collections.newSetFromMap(new IdentityHashMap<>());
    collect(project.getLogisimFile(), getDisplayName(), true, visiting);
  }

  private void collect(Library library, String parentPath, boolean root, Set<Library> visiting) {
    if ((!root && library.isHidden()) || !visiting.add(library)) return;
    try {
      final var name = library.getDisplayName() == null ? "" : library.getDisplayName().trim();
      final var path = name.isEmpty() ? parentPath : parentPath + SearchCandidate.CONTEXT_SEPARATOR + name;
      for (final var tool : library.getTools()) {
        if (tool instanceof AddTool addTool && isPlaceable(addTool)) {
          entries.add(new Entry(addTool, addTool.getDisplayName(), path));
        }
      }
      for (final var child : library.getLibraries()) collect(child, path, false, visiting);
    } finally {
      visiting.remove(library);
    }
  }

  private boolean isPlaceable(AddTool tool) {
    return !(tool.getFactory(false) instanceof SubcircuitFactory subcircuit
        && subcircuit.getSubcircuit() == project.getCurrentCircuit());
  }

  @Override
  public List<SearchResult> search(SearchQuery query) {
    if (!ForkPreferences.PALETTE.isEnabled()) return builtIn.search(query);
    final var parsed = PaletteQuery.parse(query.text());
    if (!parsed.scope().includesComponents()) return List.of();
    final var recent = Recents.names(project);
    final var results = new ArrayList<SearchResult>();
    for (final var entry : entries) {
      final var display = entry.displayText();
      int score;
      int[] highlights;
      if (parsed.text().isEmpty()) {
        final var rank = recent.indexOf(entry.tool().getName());
        score = rank < 0 ? 0 : RECENT_BONUS - rank;
        highlights = SearchResult.NO_HIGHLIGHTS;
      } else {
        final var match = FuzzyMatcher.match(parsed.text(), display);
        if (match == null) continue;
        score = match.score();
        if (match.positions()[0] >= display.length() - entry.title().length()) {
          score += TITLE_MATCH_BONUS;
        }
        highlights = match.positions();
      }
      results.add(new SearchResult(candidate(entry, parsed.parameter()), score, highlights));
    }
    return results;
  }

  private SearchCandidate candidate(Entry entry, Integer parameter) {
    var tool = entry.tool();
    var hint = "";
    if (parameter != null) {
      final var configured = (AddTool) tool.cloneTool();
      final var applied = ParameterRule.apply(configured.getAttributeSet(), parameter);
      if (applied != null) {
        tool = configured;
        hint = applied;
      }
    }
    final var armed = tool;
    final var project = this.project;
    return new SearchCandidate(
        entry.title(), entry.path(), new ToolIcon(entry.tool()), hint, true,
        () -> project.setTool(armed));
  }

  /** Paints an AddTool's icon at toolbox size. */
  private static final class ToolIcon implements Icon {
    private final AddTool tool;

    ToolIcon(AddTool tool) {
      this.tool = tool;
    }

    @Override
    public int getIconHeight() {
      return AppPreferences.getScaled(AppPreferences.BOX_SIZE);
    }

    @Override
    public int getIconWidth() {
      return AppPreferences.getScaled(AppPreferences.BOX_SIZE);
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
      final var base = graphics.create();
      base.setColor(new Color(AppPreferences.COMPONENT_ICON_COLOR.get()));
      final var iconGraphics = base.create();
      try {
        final var border = AppPreferences.getScaled(AppPreferences.ICON_BORDER);
        tool.paintIcon(new ComponentDrawContext(component, null, null, base, iconGraphics),
            x + border, y + border);
      } finally {
        iconGraphics.dispose();
        base.dispose();
      }
    }
  }
}
