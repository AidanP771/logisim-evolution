/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.palette;

import static com.cburch.logisim.fork.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;

/**
 * Maps the palette's trailing number onto "the obvious attribute" of a component:
 *
 * <ol>
 *   <li>{@code inputs} (gates): that many inputs, e.g. {@code and 3}.
 *   <li>{@code select} (multiplexers, decoders...): enough select bits for that many data lines,
 *       e.g. {@code mux 4} gives 2 select bits.
 *   <li>{@code width} (registers, pins, adders...): that many bits, e.g. {@code reg 8}.
 * </ol>
 *
 * <p>Only the first of these the component has is used. A number it cannot take is ignored,
 * never an error.
 */
public final class ParameterRule {
  private ParameterRule() {}

  /**
   * Applies {@code n} to {@code attrs} and returns a short description of what changed (for the
   * result's hint), or null if no attribute took it.
   */
  public static String apply(AttributeSet attrs, int n) {
    // The first of these the component has is "the obvious one"; if it rejects the number, the
    // number is ignored rather than spilling over to another attribute ("and 1" is not 1 bit wide).
    if (has(attrs, "inputs")) return trySet(attrs, "inputs", n) ? S.get("forkParamInputs", n) : null;
    if (has(attrs, "select")) {
      final var bits = selectBitsFor(n);
      return trySet(attrs, "select", bits) ? S.get("forkParamSelect", n, bits) : null;
    }
    if (has(attrs, "width")) return trySet(attrs, "width", n) ? S.get("forkParamWidth", n) : null;
    return null;
  }

  private static boolean has(AttributeSet attrs, String name) {
    for (final var attr : attrs.getAttributes()) {
      if (attr.getName().equals(name)) return true;
    }
    return false;
  }

  /** Select bits needed to address {@code lines} data lines (at least 1). */
  static int selectBitsFor(int lines) {
    var bits = 1;
    while ((1 << bits) < lines && bits < 31) bits++;
    return bits;
  }

  @SuppressWarnings("unchecked")
  private static boolean trySet(AttributeSet attrs, String name, int value) {
    for (final var attr : attrs.getAttributes()) {
      if (!attr.getName().equals(name)) continue;
      try {
        // Range-checked attributes (gate inputs, bit widths) throw for values they cannot take.
        final var parsed = ((Attribute<Object>) attr).parse(String.valueOf(value));
        if (parsed == null) return false;
        attrs.setValue((Attribute<Object>) attr, parsed);
        return true;
      } catch (RuntimeException e) {
        return false;
      }
    }
    return false;
  }
}
