/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.builder;

import com.cburch.logisim.fork.keymap.KeymapStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Blueprint files: {@code <fork config folder>/blueprints/<name>.json}, one per blueprint. */
public final class BlueprintStore {
  static final Logger logger = LoggerFactory.getLogger(BlueprintStore.class);

  private static Path folderOverride;

  private BlueprintStore() {}

  public static Path folder() {
    return folderOverride != null
        ? folderOverride : KeymapStore.configDirectory().resolve("blueprints");
  }

  /** For tests: store blueprints somewhere else ({@code null} restores the default). */
  static void setFolderForTesting(Path folder) {
    folderOverride = folder;
  }

  /** A file name for {@code name}: letters, digits, space, dash and underscore are kept. */
  static String fileName(String name) {
    final var safe = name.strip().replaceAll("[^A-Za-z0-9 _-]", "_");
    return (safe.isEmpty() ? "blueprint" : safe) + ".json";
  }

  public static Path save(Blueprint blueprint) throws IOException {
    final var dir = folder();
    Files.createDirectories(dir);
    final var file = dir.resolve(fileName(blueprint.name()));
    Files.writeString(file, blueprint.toJson(), StandardCharsets.UTF_8);
    return file;
  }

  public static boolean exists(String name) {
    return Files.isRegularFile(folder().resolve(fileName(name)));
  }

  /** Every readable blueprint, by name; unreadable files are logged and skipped. */
  public static List<Blueprint> list() {
    final var out = new ArrayList<Blueprint>();
    final var dir = folder();
    if (!Files.isDirectory(dir)) return out;
    try (var files = Files.list(dir)) {
      for (final var file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
        try {
          out.add(Blueprint.fromJson(Files.readString(file, StandardCharsets.UTF_8)));
        } catch (IOException | IllegalArgumentException e) {
          logger.warn("Skipping blueprint {}: {}", file, e.getMessage());
        }
      }
    } catch (IOException e) {
      logger.warn("Cannot list blueprints in {}", dir, e);
    }
    out.sort(Comparator.comparing(b -> b.name().toLowerCase()));
    return out;
  }
}
