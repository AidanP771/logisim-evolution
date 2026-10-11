/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;

/**
 * Colours of the fork's canvas overlays, each with a lighter variant for dark themes so it keeps
 * at least about 4.5:1 contrast against the dark canvas. Read them at paint time: the theme can
 * change while the app runs.
 */
public final class ForkColors {
  private static final Color UNCONNECTED_LIGHT = new Color(0xD3, 0x2F, 0x2F);
  private static final Color UNCONNECTED_DARK = new Color(0xFF, 0x6E, 0x6E);
  private static final Color ACCENT_LIGHT = new Color(0x1E, 0x6B, 0xB8);
  private static final Color ACCENT_DARK = new Color(0x64, 0xB0, 0xF4);
  private static final Color GUIDE_LIGHT = new Color(0xC2, 0x18, 0x5B);
  private static final Color GUIDE_DARK = new Color(0xF4, 0x8F, 0xB1);

  private ForkColors() {}

  public static boolean isDark() {
    return AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
  }

  /** Unconnected-pin rings and dangling-end crosses. */
  public static Color unconnected() {
    return isDark() ? UNCONNECTED_DARK : UNCONNECTED_LIGHT;
  }

  /** Net highlight, route preview and informational status text. */
  public static Color accent() {
    return isDark() ? ACCENT_DARK : ACCENT_LIGHT;
  }

  /** Alignment guides while dragging. */
  public static Color guide() {
    return isDark() ? GUIDE_DARK : GUIDE_LIGHT;
  }
}
