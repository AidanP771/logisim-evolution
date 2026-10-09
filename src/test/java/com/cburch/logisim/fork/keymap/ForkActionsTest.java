/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.keymap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.util.HashMap;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

public class ForkActionsTest {
  @Test
  public void idsAreUniqueAndLabelled() {
    final var ids = new HashSet<String>();
    for (final var action : ForkActions.all()) {
      assertTrue(ids.add(action.getId()), "duplicate id " + action.getId());
      assertFalse(action.getLabel().startsWith("forkAction."), "missing label for " + action.getId());
      assertFalse(action.getCategory().getLabel().startsWith("forkCat"), action.getId());
      assertNotNull(ForkActions.get(action.getId()));
    }
  }

  @Test
  public void defaultBindingsParseAndNeverClash() {
    // Check both platform conventions: Mod = Cmd (macOS) and Mod = Ctrl (Windows/Linux).
    for (final var menuMask : new int[] {InputEvent.META_DOWN_MASK, InputEvent.CTRL_DOWN_MASK}) {
      final var keymap = Keymap.defaults(ForkActions.all(), menuMask);
      assertTrue(keymap.getProblems().isEmpty(), keymap.getProblems().toString());
      final var owners = new HashMap<KeyBinding, String>();
      for (final var action : ForkActions.all()) {
        for (final var binding : keymap.getBindings(action)) {
          final var previous = owners.put(binding, action.getId());
          assertTrue(previous == null, binding.display() + " bound to " + previous + " and " + action.getId());
        }
      }
    }
  }
}
