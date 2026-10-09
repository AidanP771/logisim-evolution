/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.nav;

import java.util.List;

/** Pure helpers for stepping through the zoom levels offered by a {@code ZoomModel}. */
final class ZoomSteps {
  private ZoomSteps() {}

  /**
   * Returns the next zoom factor from {@code options} (percentages, ascending), or the current
   * factor if already at the end. Mirrors {@code ZoomControl.zoomIn/zoomOut} so keyboard, wheel and
   * toolbar all land on the same levels.
   */
  static double next(double current, List<Double> options, boolean zoomIn) {
    if (options == null || options.isEmpty()) return current;
    if (zoomIn) {
      final var threshold = current * 100.0 * 1.001;
      for (final var choice : options) {
        if (choice > threshold) return choice / 100.0;
      }
    } else {
      final var threshold = current * 100.0 * 0.999;
      for (var i = options.size() - 1; i >= 0; i--) {
        if (options.get(i) < threshold) return options.get(i) / 100.0;
      }
    }
    return current;
  }

  /**
   * Turns precise wheel rotation into whole zoom steps. Trackpads send many small fractional
   * rotations; zooming one level per event would be far too fast.
   */
  static final class WheelAccumulator {
    private double pending;

    /** Adds a rotation and returns the number of whole steps (negative = zoom in) now due. */
    int add(double preciseRotation) {
      // A direction change discards what was left over in the old direction.
      if (pending != 0 && Math.signum(pending) != Math.signum(preciseRotation)) pending = 0;
      pending += preciseRotation;
      final var steps = (int) pending; // truncates toward zero
      pending -= steps;
      return steps;
    }

    void reset() {
      pending = 0;
    }
  }
}
