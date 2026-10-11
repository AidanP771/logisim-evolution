/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Preference change events arrive later, on another thread. A burst of set() calls must still end
 * with the last value, both straight away and once the late events have been handled (they used to
 * be able to put an older value back, which made TextColorTest fail on Windows).
 */
public class PrefMonitorRaceTest {
  private static final String LIGHT = "com.formdev.flatlaf.FlatLightLaf";
  private static final String DARK = "com.formdev.flatlaf.FlatDarkLaf";

  @Test
  public void lastStringSetWins() throws InterruptedException {
    final var pref = AppPreferences.LookAndFeel;
    final var original = pref.get();
    try {
      for (var i = 0; i < 300; i++) {
        final var v = i % 2 == 0 ? LIGHT : DARK;
        pref.set(v);
        assertEquals(v, pref.get(), "right after set " + i);
      }
      Thread.sleep(300);
      assertEquals(DARK, pref.get(), "after the late events");
    } finally {
      pref.set(original);
    }
  }

  @Test
  public void intSetIsVisibleAtOnce() throws InterruptedException {
    final var pref = AppPreferences.TRUE_COLOR;
    final var original = pref.get();
    try {
      for (var i = 0; i < 300; i++) {
        final var v = i % 2 == 0 ? AppPreferences.DARK_TRUE_COLOR : AppPreferences.DEFAULT_TRUE_COLOR;
        pref.set(v);
        assertEquals(v, pref.get(), "right after set " + i);
      }
      Thread.sleep(300);
      assertEquals(AppPreferences.DEFAULT_TRUE_COLOR, pref.get(), "after the late events");
    } finally {
      pref.set(original);
    }
  }
}
