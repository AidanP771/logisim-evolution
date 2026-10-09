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

  public static final ForkFlag PALETTE = flag("palette.extended", Category.PALETTE, "Palette");

  public static final ForkFlag SINGLE_KEY_TOOLS =
      flag("keys.singleKeyTools", Category.KEYBOARD, "SingleKeyTools");
  public static final ForkFlag ROTATE_KEYS = flag("keys.rotate", Category.KEYBOARD, "RotateKeys");
  public static final ForkFlag CHEAT_SHEET = flag("keys.cheatSheet", Category.KEYBOARD, "CheatSheet");

  public static final ForkFlag NUDGE = flag("select.nudge", Category.SELECTION, "Nudge");
  public static final ForkFlag DUPLICATE_OFFSET =
      flag("select.duplicateOffset", Category.SELECTION, "DuplicateOffset");
  public static final ForkFlag ALIGN = flag("select.align", Category.SELECTION, "Align");
  public static final ForkFlag SNAP_GUIDES = flag("select.snapGuides", Category.SELECTION, "SnapGuides");
  public static final ForkFlag SELECT_CONNECTED =
      flag("select.connected", Category.SELECTION, "SelectConnected");

  public static final ForkFlag NET_HIGHLIGHT = flag("wire.netHighlight", Category.WIRING, "NetHighlight");
  public static final ForkFlag SHOW_UNCONNECTED =
      flag("view.showUnconnected", Category.WIRING, "ShowUnconnected");
  public static final ForkFlag PIN_WIRING = flag("wire.fromPin", Category.WIRING, "PinWiring");
  public static final ForkFlag CLICK_ROUTE = flag("wire.clickRoute", Category.WIRING, "ClickRoute");
  public static final ForkFlag WIRE_CLEANUP = flag("wire.cleanup", Category.WIRING, "WireCleanup");
  public static final ForkFlag SEGMENT_DELETE =
      flag("wire.segmentDelete", Category.WIRING, "SegmentDelete");
  public static final ForkFlag DELETE_DANGLING =
      flag("wire.deleteDangling", Category.WIRING, "DeleteDangling");

  public static final ForkFlag STATUS_HINTS = flag("ui.statusHints", Category.FEEDBACK, "StatusHints");

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
