/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class CheatSheetTest {
  private static boolean fitSelection(String query) {
    return CheatSheet.matches(query, "Fit the selection", List.of("Shift+F"), "View");
  }

  @Test
  public void blankQueryMatchesEverything() {
    assertTrue(fitSelection(""));
    assertTrue(fitSelection("   "));
  }

  @Test
  public void matchesLabelKeysAndCategoryIgnoringCase() {
    assertTrue(fitSelection("FIT"));
    assertTrue(fitSelection("shift+f"));
    assertTrue(fitSelection("view"));
  }

  @Test
  public void everyWordMustMatchSomewhere() {
    assertTrue(fitSelection("view selection shift"));
    assertFalse(fitSelection("view wire"));
    assertFalse(fitSelection("zoom"));
  }
}
