/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BlueprintTest {
  @TempDir Path tmp;

  @AfterEach
  public void restoreFolder() {
    BlueprintStore.setFolderForTesting(null);
  }

  private static Blueprint sample() {
    final var attrs = new LinkedHashMap<String, String>();
    attrs.put("facing", "east");
    attrs.put("label", "sum \"S\"\\out\n2");
    return new Blueprint("Half adder",
        List.of(new Blueprint.Part("Gates", "XOR Gate", 60, 20, attrs),
            new Blueprint.Part(Blueprint.CIRCUIT_LIBRARY, "adder", 0, 0, new LinkedHashMap<>())),
        List.of(new Blueprint.WireSpec(0, 0, 40, 0), new Blueprint.WireSpec(40, 0, 40, 30)));
  }

  @Test
  public void jsonRoundTripKeepsEverything() {
    final var original = sample();
    assertEquals(original, Blueprint.fromJson(original.toJson()));
  }

  @Test
  public void notABlueprintIsRefused() {
    assertThrows(IllegalArgumentException.class, () -> Blueprint.fromJson("[1, 2]"));
    assertThrows(IllegalArgumentException.class, () -> Blueprint.fromJson("{ broken"));
  }

  @Test
  public void storeSavesAndListsByName() throws Exception {
    BlueprintStore.setFolderForTesting(tmp);
    BlueprintStore.save(sample());
    BlueprintStore.save(new Blueprint("A/B: odd?", List.of(), List.of()));
    final var names = BlueprintStore.list().stream().map(Blueprint::name).toList();
    assertEquals(List.of("A/B: odd?", "Half adder"), names);
    assertEquals("A_B_ odd_.json", BlueprintStore.fileName("A/B: odd?"));
  }
}
