/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.keymap;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Locale;
import java.util.Map;

/**
 * One key binding, written as text such as {@code "Mod+Shift+P"}, {@code "Alt+Up"} or {@code "?"}.
 *
 * <p>{@code Mod} is the platform menu modifier (Cmd on macOS, Ctrl elsewhere); {@code Ctrl},
 * {@code Cmd}/{@code Meta}, {@code Alt}/{@code Option} and {@code Shift} are the physical keys.
 * A binding on a punctuation character that needs Shift on some layouts (such as {@code ?}) matches
 * the typed character instead of a key code, so it works on every keyboard layout.
 *
 * @param keyCode key code for pressed-key bindings, or {@link KeyEvent#VK_UNDEFINED}
 * @param keyChar character for typed-character bindings, or {@link KeyEvent#CHAR_UNDEFINED}
 * @param modifiers extended modifier mask ({@code InputEvent.*_DOWN_MASK})
 */
public record KeyBinding(int keyCode, char keyChar, int modifiers) {
  /** Modifiers that take part in matching; mouse buttons and lock keys are ignored. */
  public static final int MODIFIER_MASK =
      InputEvent.SHIFT_DOWN_MASK
          | InputEvent.CTRL_DOWN_MASK
          | InputEvent.META_DOWN_MASK
          | InputEvent.ALT_DOWN_MASK
          | InputEvent.ALT_GRAPH_DOWN_MASK;

  /** Characters bound by what is typed, not by key code. */
  private static final String TYPED_CHARS = "?!@#$%^&*()_{}|:\"<>~";

  private static final Map<String, Integer> NAMED_KEYS =
      Map.ofEntries(
          Map.entry("UP", KeyEvent.VK_UP),
          Map.entry("DOWN", KeyEvent.VK_DOWN),
          Map.entry("LEFT", KeyEvent.VK_LEFT),
          Map.entry("RIGHT", KeyEvent.VK_RIGHT),
          Map.entry("ESC", KeyEvent.VK_ESCAPE),
          Map.entry("ESCAPE", KeyEvent.VK_ESCAPE),
          Map.entry("SPACE", KeyEvent.VK_SPACE),
          Map.entry("TAB", KeyEvent.VK_TAB),
          Map.entry("ENTER", KeyEvent.VK_ENTER),
          Map.entry("BACKSPACE", KeyEvent.VK_BACK_SPACE),
          Map.entry("DELETE", KeyEvent.VK_DELETE),
          Map.entry("INSERT", KeyEvent.VK_INSERT),
          Map.entry("HOME", KeyEvent.VK_HOME),
          Map.entry("END", KeyEvent.VK_END),
          Map.entry("PAGEUP", KeyEvent.VK_PAGE_UP),
          Map.entry("PAGEDOWN", KeyEvent.VK_PAGE_DOWN),
          Map.entry("PLUS", KeyEvent.VK_PLUS),
          Map.entry("NUMPADADD", KeyEvent.VK_ADD),
          Map.entry("NUMPADSUBTRACT", KeyEvent.VK_SUBTRACT),
          Map.entry("NUMPAD0", KeyEvent.VK_NUMPAD0),
          Map.entry("=", KeyEvent.VK_EQUALS),
          Map.entry("-", KeyEvent.VK_MINUS),
          Map.entry("/", KeyEvent.VK_SLASH),
          Map.entry(",", KeyEvent.VK_COMMA),
          Map.entry(".", KeyEvent.VK_PERIOD),
          Map.entry(";", KeyEvent.VK_SEMICOLON),
          Map.entry("'", KeyEvent.VK_QUOTE),
          Map.entry("[", KeyEvent.VK_OPEN_BRACKET),
          Map.entry("]", KeyEvent.VK_CLOSE_BRACKET),
          Map.entry("\\", KeyEvent.VK_BACK_SLASH),
          Map.entry("`", KeyEvent.VK_BACK_QUOTE));

  public boolean isTyped() {
    return keyChar != KeyEvent.CHAR_UNDEFINED;
  }

  /**
   * Parses {@code text} such as {@code "Mod+Shift+P"}.
   *
   * @param menuMask the platform's menu modifier mask, which {@code Mod} stands for
   * @throws IllegalArgumentException if the text is not a valid binding
   */
  public static KeyBinding parse(String text, int menuMask) {
    if (text == null || text.isBlank()) throw new IllegalArgumentException("empty key binding");
    final var trimmed = text.trim();
    // "+" on its own, or a trailing "++" (as in "Mod++"), names the plus key itself.
    final String keyPart;
    final String modPart;
    if (trimmed.equals("+")) {
      keyPart = "+";
      modPart = "";
    } else if (trimmed.endsWith("++")) {
      keyPart = "+";
      modPart = trimmed.substring(0, trimmed.length() - 2);
    } else {
      final var cut = trimmed.lastIndexOf('+');
      keyPart = cut < 0 ? trimmed : trimmed.substring(cut + 1);
      modPart = cut < 0 ? "" : trimmed.substring(0, cut);
    }
    var mods = 0;
    if (!modPart.isEmpty()) {
      for (final var raw : modPart.split("\\+")) {
        mods |=
            switch (raw.trim().toUpperCase(Locale.ROOT)) {
              case "MOD" -> menuMask;
              case "CTRL", "CONTROL" -> InputEvent.CTRL_DOWN_MASK;
              case "CMD", "COMMAND", "META" -> InputEvent.META_DOWN_MASK;
              case "ALT", "OPTION", "OPT" -> InputEvent.ALT_DOWN_MASK;
              case "SHIFT" -> InputEvent.SHIFT_DOWN_MASK;
              default -> throw new IllegalArgumentException("unknown modifier '" + raw + "' in " + text);
            };
      }
    }
    if (keyPart.isEmpty()) throw new IllegalArgumentException("missing key in " + text);
    if (keyPart.equals("+")) return new KeyBinding(KeyEvent.VK_PLUS, KeyEvent.CHAR_UNDEFINED, mods);
    if (keyPart.length() == 1 && TYPED_CHARS.indexOf(keyPart.charAt(0)) >= 0) {
      // Typed characters already include whatever Shift the layout needs.
      return new KeyBinding(
          KeyEvent.VK_UNDEFINED, keyPart.charAt(0), mods & ~InputEvent.SHIFT_DOWN_MASK);
    }
    final var upper = keyPart.toUpperCase(Locale.ROOT);
    final var named = NAMED_KEYS.get(upper);
    if (named != null) return new KeyBinding(named, KeyEvent.CHAR_UNDEFINED, mods);
    if (upper.length() == 1) {
      final var c = upper.charAt(0);
      if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
        return new KeyBinding(c, KeyEvent.CHAR_UNDEFINED, mods); // VK_A..VK_Z, VK_0..VK_9
      }
    }
    if (upper.matches("F([1-9]|1[0-2])")) {
      return new KeyBinding(
          KeyEvent.VK_F1 + Integer.parseInt(upper.substring(1)) - 1, KeyEvent.CHAR_UNDEFINED, mods);
    }
    throw new IllegalArgumentException("unknown key '" + keyPart + "' in " + text);
  }

  /** Whether {@code event} triggers this binding. */
  public boolean matches(KeyEvent event) {
    final var mods = event.getModifiersEx() & MODIFIER_MASK;
    if (isTyped()) {
      if (event.getID() != KeyEvent.KEY_TYPED || event.getKeyChar() != keyChar) return false;
      // Shift is part of the character; other modifiers must match exactly.
      return (mods & ~InputEvent.SHIFT_DOWN_MASK) == modifiers;
    }
    return event.getID() == KeyEvent.KEY_PRESSED && event.getKeyCode() == keyCode && mods == modifiers;
  }

  /** Human-readable form for the cheat sheet, using the platform's modifier names. */
  public String display() {
    final var key = isTyped() ? String.valueOf(keyChar) : KeyEvent.getKeyText(keyCode);
    if (modifiers == 0) return key;
    final var mods = InputEvent.getModifiersExText(modifiers);
    // macOS spells modifiers as symbols ("⌘⇧"); other platforms as words ("Ctrl+Shift").
    final var symbolic = !mods.isEmpty() && !Character.isLetter(mods.charAt(mods.length() - 1));
    return symbolic ? mods + key : mods + "+" + key;
  }
}
