/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.keymap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fork.keymap.ForkAction.Category;
import com.cburch.logisim.fork.keymap.ForkAction.Scope;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

public class KeymapTest {
  private static final int MOD = InputEvent.META_DOWN_MASK;
  private static final ForkAction FIT = action("view.fit", "F");
  private static final ForkAction AND = action("tool.and", "A");
  private static final ForkAction XOR = action("tool.xor", "X");
  private static final ForkAction PAN =
      new ForkAction("view.pan", Category.VIEW, Scope.INFO, null, List.of("Space + drag"), c -> true, null);
  private static final List<ForkAction> ACTIONS = List.of(FIT, AND, XOR, PAN);

  private static ForkAction action(String id, String key) {
    return new ForkAction(id, Category.VIEW, Scope.CANVAS, null, List.of(key), c -> true, c -> { });
  }

  private static KeyBinding key(String text) {
    return KeyBinding.parse(text, MOD);
  }

  @Test
  public void defaultsAndGestureEntries() {
    final var keymap = Keymap.defaults(ACTIONS, MOD);
    assertEquals(List.of(key("F")), keymap.getBindings(FIT));
    assertEquals(List.of(), keymap.getBindings(PAN)); // shown as text, never dispatched
    assertTrue(keymap.getProblems().isEmpty(), keymap.getProblems().toString());
    final var event = new KeyEvent(new JPanel(), KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_A, 'a');
    assertEquals(List.of(AND), keymap.find(event));
  }

  @Test
  public void overridesReplaceUnbindAndAddKeys() {
    final var keymap = Keymap.load(ACTIONS, MOD, """
        {"bindings": {"tool.and": "Shift+G", "tool.xor": null, "view.fit": ["F", "Mod+Shift+F"]}}""");
    assertEquals(List.of(key("Shift+G")), keymap.getBindings(AND));
    assertEquals(List.of(), keymap.getBindings(XOR));
    assertEquals(List.of(key("F"), key("Mod+Shift+F")), keymap.getBindings(FIT));
    assertTrue(keymap.getProblems().isEmpty(), keymap.getProblems().toString());
  }

  @Test
  public void swappingTwoActionsWorks() {
    final var keymap = Keymap.load(ACTIONS, MOD, "{\"bindings\": {\"tool.and\": \"X\", \"tool.xor\": \"A\"}}");
    // tool.and takes X while tool.xor still holds it, so that one is reported and skipped...
    assertEquals(List.of(), keymap.getBindings(AND));
    assertEquals(List.of(key("A")), keymap.getBindings(XOR));
    assertEquals(1, keymap.getProblems().size());
  }

  @Test
  public void badEntriesAreSkippedNotFatal() {
    final var keymap = Keymap.load(ACTIONS, MOD, """
        {"bindings": {"nope.unknown": "Q", "tool.and": "Mod+Banana", "view.pan": "P",
                      "tool.xor": 5, "view.fit": "A"}}""");
    assertEquals(List.of(), keymap.getBindings(AND)); // its only entry was invalid
    assertEquals(List.of(key("X")), keymap.getBindings(XOR)); // bad type: default kept
    // tool.and's override cleared its default first, so A was free for view.fit.
    assertEquals(List.of(key("A")), keymap.getBindings(FIT));
    assertEquals(4, keymap.getProblems().size(), keymap.getProblems().toString());
  }

  @Test
  public void brokenJsonKeepsDefaults() {
    final var keymap = Keymap.load(ACTIONS, MOD, "{ not json");
    assertEquals(List.of(key("A")), keymap.getBindings(AND));
    assertEquals(1, keymap.getProblems().size());
    final var wrongShape = Keymap.load(ACTIONS, MOD, "[1, 2]");
    assertEquals(List.of(key("F")), wrongShape.getBindings(FIT));
    assertEquals(1, wrongShape.getProblems().size());
  }
}
