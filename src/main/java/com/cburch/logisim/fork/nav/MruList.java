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
import java.util.List;
import java.util.function.Predicate;

/** Most-recently-used list: the most recent item first, no duplicates, bounded size. */
final class MruList<T> {
  private final int capacity;
  private final List<T> items = new ArrayList<>();

  MruList(int capacity) {
    this.capacity = capacity;
  }

  /** Moves {@code item} to the front, adding it if new. */
  synchronized void touch(T item) {
    if (item == null) return;
    items.remove(item);
    items.add(0, item);
    while (items.size() > capacity) items.remove(items.size() - 1);
  }

  /** Snapshot of the items that still satisfy {@code alive}, most recent first. */
  synchronized List<T> items(Predicate<T> alive) {
    items.removeIf(alive.negate());
    return List.copyOf(items);
  }
}
