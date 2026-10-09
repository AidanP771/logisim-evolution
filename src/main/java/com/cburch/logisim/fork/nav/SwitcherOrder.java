/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.nav;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Order of entries in the circuit switcher. */
final class SwitcherOrder {
  private SwitcherOrder() {}

  /**
   * Recently viewed items first (most recent first), then every other item in project order.
   * Items in {@code recent} that are not in {@code all} (e.g. deleted circuits) are left out.
   */
  static <T> List<T> order(List<T> recent, List<T> all) {
    final var result = new LinkedHashSet<T>();
    for (final var item : recent) {
      if (all.contains(item)) result.add(item);
    }
    result.addAll(all);
    return new ArrayList<>(result);
  }
}
