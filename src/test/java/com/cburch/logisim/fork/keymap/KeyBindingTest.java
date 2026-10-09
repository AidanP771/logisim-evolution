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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

public class KeyBindingTest {
  private static final int META = InputEvent.META_DOWN_MASK;
  private static final JPanel SOURCE = new JPanel();

  private static KeyEvent pressed(int code, int mods) {
    return new KeyEvent(SOURCE, KeyEvent.KEY_PRESSED, 0, mods, code, KeyEvent.CHAR_UNDEFINED);
  }

  private static KeyEvent typed(char c, int mods) {
    return new KeyEvent(SOURCE, KeyEvent.KEY_TYPED, 0, mods, KeyEvent.VK_UNDEFINED, c);
  }

  @Test
  public void modStandsForThePlatformMenuModifier() {
    final var b = KeyBinding.parse("Mod+Shift+P", META);
    assertEquals(new KeyBinding(KeyEvent.VK_P, KeyEvent.CHAR_UNDEFINED, META | InputEvent.SHIFT_DOWN_MASK), b);
    assertTrue(b.matches(pressed(KeyEvent.VK_P, META | InputEvent.SHIFT_DOWN_MASK)));
    assertFalse(b.matches(pressed(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
    assertEquals(KeyBinding.parse("Mod+Shift+P", InputEvent.CTRL_DOWN_MASK).modifiers(),
        InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
  }

  @Test
  public void modifiersMustMatchExactly() {
    final var f = KeyBinding.parse("F", META);
    assertTrue(f.matches(pressed(KeyEvent.VK_F, 0)));
    assertFalse(f.matches(pressed(KeyEvent.VK_F, InputEvent.SHIFT_DOWN_MASK)));
    // Mouse buttons held at the same time do not matter.
    assertTrue(f.matches(pressed(KeyEvent.VK_F, InputEvent.BUTTON1_DOWN_MASK)));
    assertTrue(KeyBinding.parse("Alt+Up", META).matches(pressed(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK)));
  }

  @Test
  public void questionMarkMatchesTheTypedCharacterOnAnyLayout() {
    final var help = KeyBinding.parse("?", META);
    assertTrue(help.isTyped());
    assertTrue(help.matches(typed('?', InputEvent.SHIFT_DOWN_MASK)));
    assertTrue(help.matches(typed('?', 0)));
    assertFalse(help.matches(typed('/', 0)));
    assertFalse(help.matches(pressed(KeyEvent.VK_SLASH, InputEvent.SHIFT_DOWN_MASK)));
  }

  @Test
  public void namedAndPunctuationKeys() {
    assertEquals(KeyEvent.VK_PLUS, KeyBinding.parse("Mod++", META).keyCode());
    assertEquals(KeyEvent.VK_EQUALS, KeyBinding.parse("Mod+=", META).keyCode());
    assertEquals(KeyEvent.VK_ESCAPE, KeyBinding.parse("esc", META).keyCode());
    assertEquals(KeyEvent.VK_F9, KeyBinding.parse("Ctrl+F9", META).keyCode());
    assertEquals(KeyEvent.VK_0, KeyBinding.parse("Mod+0", META).keyCode());
    assertEquals(KeyEvent.VK_ADD, KeyBinding.parse("Mod+NumpadAdd", META).keyCode());
  }

  @Test
  public void invalidBindingsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> KeyBinding.parse("", META));
    assertThrows(IllegalArgumentException.class, () -> KeyBinding.parse("Hyper+F", META));
    assertThrows(IllegalArgumentException.class, () -> KeyBinding.parse("Mod+", META));
    assertThrows(IllegalArgumentException.class, () -> KeyBinding.parse("Mod+Banana", META));
  }
}
