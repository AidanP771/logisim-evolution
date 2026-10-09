/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ForkPreferencesTest {
  private Preferences testNode;

  @BeforeEach
  public void useTestNode() {
    testNode = Preferences.userRoot().node("/com/cburch/logisim-fork-ux-test-" + System.nanoTime());
    ForkPreferences.setNodeForTesting(testNode);
  }

  @AfterEach
  public void removeTestNode() throws BackingStoreException {
    ForkPreferences.setNodeForTesting(null);
    testNode.removeNode();
  }

  @Test
  public void everyFlagDefaultsToOn() {
    for (final var flag : ForkPreferences.getFlags()) {
      assertTrue(flag.isEnabled(), flag.getKey());
    }
  }

  @Test
  public void flagValuesPersistInTheirOwnNode() {
    ForkPreferences.SPACE_PAN.setEnabled(false);
    assertFalse(ForkPreferences.SPACE_PAN.isEnabled());
    assertFalse(testNode.getBoolean(ForkPreferences.SPACE_PAN.getKey(), true));
    assertTrue(ForkPreferences.MIDDLE_PAN.isEnabled());
    ForkPreferences.SPACE_PAN.setEnabled(true);
    assertTrue(ForkPreferences.SPACE_PAN.isEnabled());
  }

  @Test
  public void listenersFireOnlyOnChange() {
    final var calls = new AtomicInteger();
    ForkPreferences.addListener(ForkPreferences.FIT_KEYS, e -> calls.incrementAndGet());
    ForkPreferences.FIT_KEYS.setEnabled(true); // already on: no event
    ForkPreferences.FIT_KEYS.setEnabled(false);
    ForkPreferences.FIT_KEYS.setEnabled(false); // unchanged: no event
    assertEquals(1, calls.get());
  }

  @Test
  public void keysAreUniqueAndOutsideLogisimNode() {
    final var keys = new HashSet<String>();
    for (final var flag : ForkPreferences.getFlags()) {
      assertTrue(keys.add(flag.getKey()), "duplicate key " + flag.getKey());
    }
    // Must not be Logisim's own node (or below it), so the stock app never sees these values.
    assertNotEquals("/com/cburch/logisim", ForkPreferences.NODE_PATH);
    assertFalse(ForkPreferences.NODE_PATH.startsWith("/com/cburch/logisim/"));
  }

  @Test
  public void everyFlagHasLabelAndDescription() {
    for (final var flag : ForkPreferences.getFlags()) {
      assertFalse(flag.getLabel().isBlank(), flag.getKey());
      assertFalse(flag.getDescription().isBlank(), flag.getKey());
      assertNotEquals(flag.getLabel(), flag.getDescription(), flag.getKey());
      // LocaleManager returns the key itself when a string is missing.
      assertFalse(flag.getLabel().startsWith("forkFlag"), "missing string for " + flag.getKey());
      assertFalse(flag.getDescription().startsWith("forkFlag"), "missing string for " + flag.getKey());
      assertFalse(flag.getCategory().getLabel().startsWith("forkCategory"), flag.getKey());
    }
  }
}
