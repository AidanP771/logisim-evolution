/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.palette;

import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.search.OmniSearchDialog;
import com.cburch.logisim.gui.search.SearchProviders;
import com.cburch.logisim.gui.search.providers.AddToolSearchProvider;
import com.cburch.logisim.gui.search.providers.MenuSearchProvider;
import com.cburch.logisim.proj.Project;
import javax.swing.SwingUtilities;

/**
 * The command palette: the built-in "Find Action" dialog with the fork's providers.
 *
 * <p>{@link #install()} swaps the built-in component and menu providers for wrappers that
 * understand {@code >}, {@code @} and trailing numbers, and adds circuits and fork commands. With
 * the palette flag off the wrappers defer to the built-in providers and the extra providers stay
 * hidden, so Find Action behaves exactly as in the stock release.
 */
public final class Palette {
  private static boolean installed;

  private Palette() {}

  public static synchronized void install() {
    if (installed) return;
    installed = true;
    for (final var provider : SearchProviders.getAll()) {
      if (provider instanceof AddToolSearchProvider) {
        SearchProviders.unregister(provider);
        SearchProviders.register(new ComponentProvider(provider));
      } else if (provider instanceof MenuSearchProvider) {
        SearchProviders.unregister(provider);
        SearchProviders.register(new ScopedProvider(provider, PaletteQuery.Scope::includesActions));
      }
    }
    SearchProviders.register(new CircuitProvider());
    SearchProviders.register(new ActionProvider());
    PaletteKeys.install();
  }

  /** Starts remembering recently armed components for {@code project}. */
  public static void track(Project project) {
    Recents.track(project);
  }

  /** Opens the palette over {@code frame}. */
  public static void open(Frame frame) {
    // Opened after the triggering key event finishes; the dialog is modal and blocks.
    SwingUtilities.invokeLater(() -> OmniSearchDialog.showForWindow(frame));
  }
}
