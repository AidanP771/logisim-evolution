/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.palette;

/**
 * What the user typed into the palette, split into scope, search text and an optional number.
 *
 * <ul>
 *   <li>{@code >zoom} searches actions only (menus and fork commands).
 *   <li>{@code @adder} searches the project's circuits only.
 *   <li>{@code and 3} searches for "and" and passes 3 as a parameter (3 inputs).
 * </ul>
 *
 * @param scope which kinds of result to show
 * @param text the search text, without the prefix and the trailing number
 * @param parameter the trailing number, or null
 */
public record PaletteQuery(Scope scope, String text, Integer parameter) {
  /** Which providers a query addresses. */
  public enum Scope {
    ALL,
    ACTIONS,
    CIRCUITS;

    public boolean includesActions() {
      return this == ALL || this == ACTIONS;
    }

    public boolean includesCircuits() {
      return this == ALL || this == CIRCUITS;
    }

    public boolean includesComponents() {
      return this == ALL;
    }
  }

  /** Largest accepted parameter; larger numbers are treated as search text. */
  static final int MAX_PARAMETER = 4096;

  public static PaletteQuery parse(String raw) {
    var text = raw == null ? "" : raw.strip();
    var scope = Scope.ALL;
    if (text.startsWith(">")) {
      scope = Scope.ACTIONS;
      text = text.substring(1).strip();
    } else if (text.startsWith("@")) {
      scope = Scope.CIRCUITS;
      text = text.substring(1).strip();
    }
    Integer parameter = null;
    if (scope == Scope.ALL) {
      final var space = text.lastIndexOf(' ');
      if (space > 0) {
        final var last = text.substring(space + 1);
        if (last.matches("\\d{1,4}")) {
          final var value = Integer.parseInt(last);
          if (value >= 1 && value <= MAX_PARAMETER) {
            parameter = value;
            text = text.substring(0, space).strip();
          }
        }
      }
    }
    return new PaletteQuery(scope, text, parameter);
  }
}
