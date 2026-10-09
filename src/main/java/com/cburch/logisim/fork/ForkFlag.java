/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork;

import static com.cburch.logisim.fork.Strings.S;

/**
 * On/off switch for one fork UX feature. Values live in {@link ForkPreferences}, never in .circ
 * files, and every flag defaults to on.
 */
public final class ForkFlag {
  /** Groups flags on the "Fork UX" preferences tab. */
  public enum Category {
    NAVIGATION("forkCategoryNavigation"),
    PALETTE("forkCategoryPalette"),
    KEYBOARD("forkCategoryKeyboard"),
    SELECTION("forkCategorySelection"),
    WIRING("forkCategoryWiring"),
    FEEDBACK("forkCategoryFeedback");

    private final String labelKey;

    Category(String labelKey) {
      this.labelKey = labelKey;
    }

    public String getLabel() {
      return S.get(labelKey);
    }
  }

  private final String key;
  private final Category category;
  private final String labelKey;
  private final String descriptionKey;

  ForkFlag(String key, Category category, String labelKey, String descriptionKey) {
    this.key = key;
    this.category = category;
    this.labelKey = labelKey;
    this.descriptionKey = descriptionKey;
  }

  /** Preference key, also used as the flag's stable identifier. */
  public String getKey() {
    return key;
  }

  public Category getCategory() {
    return category;
  }

  public String getLabel() {
    return S.get(labelKey);
  }

  public String getDescription() {
    return S.get(descriptionKey);
  }

  public boolean isEnabled() {
    return ForkPreferences.isEnabled(this);
  }

  public void setEnabled(boolean enabled) {
    ForkPreferences.setEnabled(this, enabled);
  }

  @Override
  public String toString() {
    return key;
  }
}
