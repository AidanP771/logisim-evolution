/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.keymap;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.MacCompatibility;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the active {@link Keymap} from {@code keymap.json} in the fork's user config folder
 * (never from a .circ file):
 *
 * <ul>
 *   <li>macOS: {@code ~/Library/Application Support/Logisim-evolution-fork/}
 *   <li>Windows: {@code %APPDATA%\Logisim-evolution-fork\}
 *   <li>Linux and others: {@code $XDG_CONFIG_HOME/logisim-evolution-fork/} (default
 *       {@code ~/.config})
 * </ul>
 */
public final class KeymapStore {
  private static final Logger logger = LoggerFactory.getLogger(KeymapStore.class);
  private static Keymap current;

  private KeymapStore() {}

  /** The fork's user config folder (may not exist yet). */
  public static Path configDirectory() {
    final var home = System.getProperty("user.home");
    if (MacCompatibility.isRunningOnMac()) {
      return Paths.get(home, "Library", "Application Support", "Logisim-evolution-fork");
    }
    final var os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    if (os.contains("win")) {
      final var appData = System.getenv("APPDATA");
      return Paths.get(appData != null ? appData : home, "Logisim-evolution-fork");
    }
    final var xdg = System.getenv("XDG_CONFIG_HOME");
    return Paths.get(xdg != null && !xdg.isBlank() ? xdg : home + "/.config", "logisim-evolution-fork");
  }

  public static Path keymapFile() {
    return configDirectory().resolve("keymap.json");
  }

  /** The active keymap, loaded on first use. */
  public static synchronized Keymap get() {
    if (current == null) reload();
    return current;
  }

  /** Re-reads keymap.json. Problems are logged and kept on the keymap; they never throw. */
  public static synchronized Keymap reload() {
    String json = null;
    final var file = keymapFile();
    if (Files.isRegularFile(file)) {
      try {
        json = Files.readString(file, StandardCharsets.UTF_8);
      } catch (IOException e) {
        logger.warn("Could not read {}: {}", file, e.toString());
      }
    }
    current = Keymap.load(ForkActions.all(), AppPreferences.hotkeyMenuMask, json);
    for (final var problem : current.getProblems()) logger.warn("Keymap: {}", problem);
    return current;
  }
}
