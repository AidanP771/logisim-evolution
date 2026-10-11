/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import java.util.prefs.PreferenceChangeEvent;

/**
 * Represents a preference monitor for integer values. This class listens
 * to preference changes and appropriately updates its internal value.
 */
class PrefMonitorInt extends AbstractPrefMonitor<Integer> {
  /** Default integer value for this preference monitor. */
  private final int dflt;

  /** Current integer value of this preference monitor. */
  private volatile int value;

  /**
   * Constructs a new preference monitor for integer values.
   *
   * @param name The name of the preference.
   * @param dflt The default integer value.
   */
  public PrefMonitorInt(String name, int dflt) {
    super(name);
    this.dflt = dflt;
    final var prefs = AppPreferences.getPrefs();
    this.value = prefs.getInt(name, dflt);
    prefs.addPreferenceChangeListener(this);
  }

  /**
   * Retrieves the current value of the preference.
   *
   * @return The current integer value.
   */
  public Integer get() {
    return value;
  }

  /**
   * Handles preference changes and updates the internal value if needed.
   * Does nothing if newValue is the same as the current value.
   *
   * @param event The event indicating a preference change.
   */
  public void preferenceChange(PreferenceChangeEvent event) {
    final var prefs = event.getNode();
    final var prop = event.getKey();
    final var name = getIdentifier();
    if (prop.equals(name)) {
      final int oldValue;
      final int newValue;
      // Same lock as set(): a late event can't revert a newer value (see PrefMonitorString).
      synchronized (this) {
        oldValue = value;
        newValue = prefs.getInt(name, dflt);
        if (newValue == oldValue) return;
        value = newValue;
      }
      AppPreferences.firePropertyChange(name, oldValue, newValue);
    }
  }

  /**
   * Sets the preference value.
   * Does nothing if newValue is the same as the current value.
   *
   * @param newValue The new integer value to set.
   */
  public void set(Integer newValue) {
    // The value changes at once, not when the preferences thread gets round to it, so code that
    // sets and then reads (applyThemeColors) sees the new value.
    final int oldValue;
    synchronized (this) {
      oldValue = value;
      if (oldValue == newValue) return;
      AppPreferences.getPrefs().putInt(getIdentifier(), newValue);
      value = newValue;
    }
    AppPreferences.firePropertyChange(getIdentifier(), oldValue, (int) newValue);
  }
}
