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
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * The command palette: the built-in "Find Action" dialog with the fork's providers.
 *
 * <p>{@link #install()} swaps the built-in component and menu providers for wrappers that
 * understand {@code >}, {@code @} and trailing numbers, and adds circuits and fork commands. With
 * the palette flag off the wrappers defer to the built-in providers and the extra providers stay
 * hidden, so Find Action behaves exactly as in the stock release.
 */
public final class Palette {
  private static final int PREWARM_DELAY_MS = 1500;
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
    // Build each main window's palette shortly after it opens, while the user is still looking.
    Toolkit.getDefaultToolkit().addAWTEventListener(
        event -> {
          if (event.getID() != WindowEvent.WINDOW_OPENED) return;
          final var window = ((WindowEvent) event).getWindow();
          final var timer = new Timer(PREWARM_DELAY_MS, e -> OmniSearchDialog.prewarm(window));
          timer.setRepeats(false);
          timer.start();
        },
        AWTEvent.WINDOW_EVENT_MASK);
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
