/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork;

import com.cburch.logisim.fork.ForkFlag.Category;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Feature flags for the UX fork.
 *
 * <p>Stored in their own {@link Preferences} node, separate from Logisim's own preferences
 * ({@code AppPreferences}) and never written to .circ files, so the stock release ignores them.
 */
public final class ForkPreferences {
  /** Absolute node path; a sibling of Logisim's own node, not a child of it. */
  static final String NODE_PATH = "/com/cburch/logisim-fork-ux";

  private static final List<ForkFlag> FLAGS = new ArrayList<>();
  private static final PropertyChangeSupport SUPPORT = new PropertyChangeSupport(ForkPreferences.class);
  private static Preferences node;

  public static final ForkFlag SPACE_PAN = flag("nav.spacePan", Category.NAVIGATION, "SpacePan");
  public static final ForkFlag MIDDLE_PAN = flag("nav.middlePan", Category.NAVIGATION, "MiddlePan");
  public static final ForkFlag ZOOM_TO_CURSOR =
      flag("nav.zoomToCursor", Category.NAVIGATION, "ZoomToCursor");
  public static final ForkFlag PINCH_ZOOM = flag("nav.pinchZoom", Category.NAVIGATION, "PinchZoom");
  public static final ForkFlag FIT_KEYS = flag("nav.fitKeys", Category.NAVIGATION, "FitKeys");
  public static final ForkFlag RESET_ZOOM = flag("nav.resetZoom", Category.NAVIGATION, "ResetZoom");
  public static final ForkFlag CIRCUIT_SWITCHER =
      flag("nav.circuitSwitcher", Category.NAVIGATION, "CircuitSwitcher");
  public static final ForkFlag BACK_TO_PARENT =
      flag("nav.backToParent", Category.NAVIGATION, "BackToParent");

  private ForkPreferences() {}

  private static ForkFlag flag(String key, Category category, String stringSuffix) {
    final var flag =
        new ForkFlag(key, category, "forkFlag" + stringSuffix, "forkFlag" + stringSuffix + "Desc");
    FLAGS.add(flag);
    return flag;
  }

  /** All flags, in registration order. */
  public static List<ForkFlag> getFlags() {
    return Collections.unmodifiableList(FLAGS);
  }

  static synchronized Preferences getNode() {
    if (node == null) node = Preferences.userRoot().node(NODE_PATH);
    return node;
  }

  /** Redirects storage, for unit tests only. Pass {@code null} to restore the real node. */
  static synchronized void setNodeForTesting(Preferences testNode) {
    node = testNode;
  }

  static boolean isEnabled(ForkFlag flag) {
    return getNode().getBoolean(flag.getKey(), true);
  }

  static void setEnabled(ForkFlag flag, boolean enabled) {
    final var old = isEnabled(flag);
    getNode().putBoolean(flag.getKey(), enabled);
    if (old != enabled) SUPPORT.firePropertyChange(flag.getKey(), old, enabled);
  }

  /** Listens for changes to one flag; the event's property name is the flag key. */
  public static void addListener(ForkFlag flag, PropertyChangeListener listener) {
    SUPPORT.addPropertyChangeListener(flag.getKey(), listener);
  }

  public static void removeListener(ForkFlag flag, PropertyChangeListener listener) {
    SUPPORT.removePropertyChangeListener(flag.getKey(), listener);
  }
}
