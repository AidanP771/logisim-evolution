/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.gui;

import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.util.MacCompatibility;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Light / dark appearance, switched live (no restart) and reachable from the keymap
 * ({@code view.darkMode}), the palette and the Fork UX tab.
 *
 * <p>It drives Logisim's own look-and-feel preference (Preferences, Window tab): Light and Dark
 * pick the partner of the current theme (IntelliJ and Darcula, Light and Dark, macOS Light and
 * macOS Dark), then apply it the way the Window tab does. Follow system reads the OS setting at
 * startup and whenever a main window is activated. Manual leaves the theme to the Window tab.
 */
public final class Appearance {
  /** How the theme is chosen. */
  public enum Mode {
    MANUAL,
    LIGHT,
    DARK,
    SYSTEM
  }

  static final Logger logger = LoggerFactory.getLogger(Appearance.class);

  private static final String MODE_KEY = "ui.appearance";
  /** Do not ask the OS more often than this while windows are being activated. */
  private static final long SYSTEM_CHECK_INTERVAL_MS = 2000;

  /** Light theme and its dark partner. */
  private static final Map<String, String> DARK_PARTNER =
      Map.of(
          FlatIntelliJLaf.class.getName(), FlatDarculaLaf.class.getName(),
          FlatLightLaf.class.getName(), FlatDarkLaf.class.getName(),
          FlatMacLightLaf.class.getName(), FlatMacDarkLaf.class.getName());

  private static boolean installed;
  private static volatile long lastSystemCheck;

  private Appearance() {}

  public static Mode getMode() {
    try {
      return Mode.valueOf(ForkPreferences.getValue(MODE_KEY, Mode.MANUAL.name()));
    } catch (IllegalArgumentException e) {
      return Mode.MANUAL;
    }
  }

  /** Stores {@code mode} and applies it now. */
  public static void setMode(Mode mode) {
    ForkPreferences.setValue(MODE_KEY, mode.name());
    switch (mode) {
      case LIGHT -> apply(false);
      case DARK -> apply(true);
      case SYSTEM -> {
        final var dark = systemIsDark();
        if (dark != null) apply(dark);
      }
      default -> {
        // Manual: the Window tab decides.
      }
    }
  }

  public static boolean isDark() {
    return AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
  }

  /** Flips between light and dark, leaving "follow system" if it was on. */
  public static void toggle() {
    setMode(isDark() ? Mode.LIGHT : Mode.DARK);
  }

  /**
   * The theme to use for {@code dark}, given the current theme {@code laf}: {@code laf} itself if
   * it already matches, else its partner, else the platform's default FlatLaf theme.
   */
  static String themeFor(String laf, boolean dark, boolean mac) {
    if (laf != null && AppPreferences.isDarkTheme(laf) == dark) return laf;
    for (final var pair : DARK_PARTNER.entrySet()) {
      if (pair.getKey().equals(laf) && dark) return pair.getValue();
      if (pair.getValue().equals(laf) && !dark) return pair.getKey();
    }
    if (mac) return dark ? FlatMacDarkLaf.class.getName() : FlatMacLightLaf.class.getName();
    return dark ? FlatDarculaLaf.class.getName() : FlatIntelliJLaf.class.getName();
  }

  /**
   * Called by {@code Main} before the look and feel is set: with Light, Dark or Follow system,
   * makes the stored theme match, so the app starts in the right colours with no flash.
   */
  public static void beforeStartup() {
    final Boolean dark =
        switch (getMode()) {
          case LIGHT -> false;
          case DARK -> true;
          case SYSTEM -> systemIsDark();
          default -> null;
        };
    if (dark == null) return;
    final var current = AppPreferences.LookAndFeel.get();
    final var target = themeFor(current, dark, MacCompatibility.isRunningOnMac());
    if (!target.equals(current)) AppPreferences.LookAndFeel.set(target);
  }

  /** Re-checks the OS setting whenever a main window is activated, in Follow system mode. */
  public static synchronized void install() {
    if (installed) return;
    installed = true;
    Toolkit.getDefaultToolkit().addAWTEventListener(
        event -> {
          if (event.getID() == WindowEvent.WINDOW_ACTIVATED
              && ((WindowEvent) event).getWindow() instanceof Frame) {
            followSystemSoon();
          }
        },
        AWTEvent.WINDOW_EVENT_MASK);
  }

  private static void followSystemSoon() {
    if (getMode() != Mode.SYSTEM) return;
    final var now = System.currentTimeMillis();
    if (now - lastSystemCheck < SYSTEM_CHECK_INTERVAL_MS) return;
    lastSystemCheck = now;
    final var thread = new Thread(() -> {
      final var dark = systemIsDark();
      if (dark != null) {
        SwingUtilities.invokeLater(() -> {
          if (getMode() == Mode.SYSTEM && dark != isDark()) apply(dark);
        });
      }
    }, "fork-appearance");
    thread.setDaemon(true);
    thread.start();
  }

  /** Switches to the light or dark theme now, in every window. */
  static void apply(boolean dark) {
    final var current = AppPreferences.LookAndFeel.get();
    final var target = themeFor(current, dark, MacCompatibility.isRunningOnMac());
    if (target.equals(current) && UIManager.getLookAndFeel() != null
        && target.equals(UIManager.getLookAndFeel().getClass().getName())) {
      return;
    }
    try {
      AppPreferences.LookAndFeel.set(target);
      AppPreferences.applyThemeColors();
      UIManager.setLookAndFeel(target);
    } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
      logger.warn("Could not switch to {}", target, e);
      return;
    }
    if (UIManager.getLookAndFeel() instanceof FlatLaf) {
      FlatLaf.updateUI();
    } else {
      for (final var window : Window.getWindows()) SwingUtilities.updateComponentTreeUI(window);
    }
    if (MacCompatibility.isRunningOnMac()) {
      // Native title bars follow the theme, not the OS setting.
      final var appearance = dark ? "NSAppearanceNameDarkAqua" : "NSAppearanceNameAqua";
      for (final var window : Window.getWindows()) {
        if (window instanceof RootPaneContainer c) {
          c.getRootPane().putClientProperty("apple.awt.windowAppearance", appearance);
        }
      }
    }
    // As the Window tab does: default circuit appearances pick up the new component colours.
    for (final var proj : Projects.getOpenProjects()) {
      for (final var circuit : proj.getLogisimFile().getCircuits()) {
        if (circuit.getAppearance().isDefaultAppearance()) {
          circuit.getAppearance().recomputeDefaultAppearance();
        }
      }
      if (proj.getFrame() != null) proj.getFrame().repaint();
    }
  }

  /** Whether the OS is in dark mode, or {@code null} if that can't be told. */
  static Boolean systemIsDark() {
    final var os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    try {
      if (os.contains("mac")) {
        final var r = run(List.of("defaults", "read", "-g", "AppleInterfaceStyle"));
        return r == null ? null : parseMac(r.output());
      } else if (os.contains("win")) {
        final var r = run(List.of("reg", "query",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "/v", "AppsUseLightTheme"));
        return r == null || r.exit() != 0 ? null : parseWindows(r.output());
      } else {
        final var r = run(List.of("gsettings", "get", "org.gnome.desktop.interface", "color-scheme"));
        if (r != null && r.exit() == 0 && parseLinux(r.output()) != null) {
          return parseLinux(r.output());
        }
        final var gtk = run(List.of("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme"));
        return gtk == null || gtk.exit() != 0 ? null : gtk.output().toLowerCase(Locale.ROOT)
            .contains("dark");
      }
    } catch (IOException e) {
      return null;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  /** {@code defaults read -g AppleInterfaceStyle} prints "Dark" in dark mode and fails in light. */
  static Boolean parseMac(String output) {
    return output.trim().equalsIgnoreCase("Dark");
  }

  /** The registry value is 0x0 when apps use the dark theme. */
  static Boolean parseWindows(String output) {
    for (final var line : output.split("\\R")) {
      if (line.contains("AppsUseLightTheme")) {
        return line.trim().endsWith("0x0");
      }
    }
    return null;
  }

  /** GNOME color-scheme: 'prefer-dark' or 'prefer-light'/'default'; null if unset. */
  static Boolean parseLinux(String output) {
    final var value = output.trim().toLowerCase(Locale.ROOT);
    if (value.contains("dark")) return true;
    if (value.contains("light")) return false;
    return null;
  }

  private record Result(int exit, String output) {}

  private static Result run(List<String> command) throws IOException, InterruptedException {
    final var process = new ProcessBuilder(command).redirectErrorStream(true).start();
    if (!process.waitFor(2, TimeUnit.SECONDS)) {
      process.destroy();
      return null;
    }
    final var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return new Result(process.exitValue(), output);
  }
}
