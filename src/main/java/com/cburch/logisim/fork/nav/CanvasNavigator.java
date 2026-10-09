/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.nav;

import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.PokeTool;
import com.cburch.logisim.tools.TextTool;
import com.cburch.logisim.util.MacCompatibility;
import java.awt.Cursor;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.SwingUtilities;

/**
 * Canvas navigation for the UX fork: Space+drag and middle-drag panning, zoom towards the cursor,
 * fit / reset zoom and "back to parent".
 *
 * <p>{@link Canvas} offers every key, mouse and wheel event here first; a {@code true} return
 * means the event was handled and the canvas must not process it further. Each feature is
 * controlled by its own flag in {@link ForkPreferences}; with a flag off the event passes through
 * untouched, giving the stock behaviour.
 *
 * <p>Mouse events arrive in circuit coordinates (the canvas rescales them for zoom), so panning
 * works on screen coordinates instead. Wheel events arrive in canvas pixels.
 */
public final class CanvasNavigator {
  /** Screen pixels the pointer must travel before a middle press becomes a pan, not a click. */
  private static final int PAN_THRESHOLD = 4;

  private static final int MODIFIERS =
      InputEvent.SHIFT_DOWN_MASK
          | InputEvent.CTRL_DOWN_MASK
          | InputEvent.META_DOWN_MASK
          | InputEvent.ALT_DOWN_MASK
          | InputEvent.ALT_GRAPH_DOWN_MASK;

  private static final Cursor PAN_CURSOR = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);

  private final Canvas canvas;
  private final ZoomSteps.WheelAccumulator wheel = new ZoomSteps.WheelAccumulator();

  /** Space is held (and was claimed by us). */
  private boolean spaceDown;
  /** A press was passed to the canvas and its tool is mid-gesture (e.g. drawing a wire). */
  private boolean toolGesture;
  /** We consumed a press, so its drags and release are ours too. */
  private boolean ownGesture;
  /** The pending middle (or macOS Cmd) press, until it turns into a pan or a click. */
  private MouseEvent middlePress;
  /** Events we re-dispatch ourselves must reach the canvas untouched. */
  private boolean replaying;

  private boolean panning;
  private Point panStart;
  private int panStartScrollX;
  private int panStartScrollY;
  private Cursor cursorBeforePan;

  public CanvasNavigator(Canvas canvas) {
    this.canvas = canvas;
    CircuitHistory.forProject(canvas.getProject());
    CircuitSwitcher.install();
    PinchZoom.install(canvas);
    canvas.addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusLost(FocusEvent e) {
            // A key release can go missing when focus moves; never leave Space "stuck".
            spaceDown = false;
            if (!ownGesture) stopPan();
          }
        });
  }

  // ---------------------------------------------------------------------------------------------
  // Keyboard

  public boolean keyPressed(KeyEvent e) {
    final var code = e.getKeyCode();
    final var mods = e.getModifiersEx() & MODIFIERS;
    final var menuMask = AppPreferences.hotkeyMenuMask;

    if (code == KeyEvent.VK_SPACE && mods == 0 && ForkPreferences.SPACE_PAN.isEnabled()
        && !isTyping()) {
      if (!spaceDown) {
        spaceDown = true;
        showPanCursor();
        // Pressed mid-gesture (e.g. while dragging a wire): pan from here without ending it.
        if (toolGesture) startPan(pointerOnScreen());
      }
      e.consume();
      return true;
    }

    final var zoomMods = mods & ~InputEvent.SHIFT_DOWN_MASK;
    if ((zoomMods == menuMask || zoomMods == InputEvent.CTRL_DOWN_MASK)
        && ForkPreferences.ZOOM_TO_CURSOR.isEnabled()) {
      Boolean zoomIn = null;
      switch (code) {
        case KeyEvent.VK_EQUALS, KeyEvent.VK_PLUS, KeyEvent.VK_ADD -> zoomIn = true;
        case KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT -> zoomIn = false;
        default -> {
          // not a zoom key
        }
      }
      if (zoomIn != null) {
        ViewActions.zoomStep(canvas, zoomIn, canvas.getMousePosition());
        e.consume();
        return true;
      }
    }

    if (mods == menuMask && (code == KeyEvent.VK_0 || code == KeyEvent.VK_NUMPAD0)
        && ForkPreferences.RESET_ZOOM.isEnabled()) {
      ViewActions.resetZoom(canvas);
      e.consume();
      return true;
    }

    if (code == KeyEvent.VK_F && ForkPreferences.FIT_KEYS.isEnabled() && !isTyping()) {
      if (mods == 0) {
        ViewActions.fitAll(canvas);
        e.consume();
        return true;
      } else if (mods == InputEvent.SHIFT_DOWN_MASK) {
        ViewActions.fitSelection(canvas);
        e.consume();
        return true;
      }
    }

    if (code == KeyEvent.VK_UP && mods == InputEvent.ALT_DOWN_MASK
        && ForkPreferences.BACK_TO_PARENT.isEnabled() && !isTyping()
        && canvas.getSelection().isEmpty()) {
      // With a selection, Alt+arrows keep setting the label position.
      ViewActions.backToParent(canvas.getProject());
      e.consume();
      return true;
    }
    return false;
  }

  public boolean keyReleased(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_SPACE && spaceDown) {
      spaceDown = false;
      // A pan started with the mouse button finishes when the button is released.
      if (!ownGesture) stopPan();
      e.consume();
      return true;
    }
    return false;
  }

  /** True while the canvas is taking typed text, so plain keys must reach the tool. */
  private boolean isTyping() {
    final var tool = canvas.getProject().getTool();
    if (tool instanceof TextTool textTool && textTool.isEditing()) return true;
    // PokeTool exposes "has a caret that takes keys" as isScrollable().
    return tool instanceof PokeTool pokeTool && pokeTool.isScrollable();
  }

  // ---------------------------------------------------------------------------------------------
  // Mouse

  public boolean mousePressed(MouseEvent e) {
    if (replaying) return false;
    if (ForkPreferences.MIDDLE_PAN.isEnabled() && isMiddlePress(e)) {
      middlePress = e;
      ownGesture = true;
      panStart = e.getLocationOnScreen();
      return true;
    }
    if (spaceDown && e.getButton() == MouseEvent.BUTTON1) {
      ownGesture = true;
      startPan(e.getLocationOnScreen());
      return true;
    }
    toolGesture = true;
    return false;
  }

  public boolean mouseDragged(MouseEvent e) {
    if (ownGesture) {
      final var p = e.getLocationOnScreen();
      if (!panning && middlePress != null && panStart.distance(p) >= PAN_THRESHOLD) {
        startPan(panStart);
      }
      if (panning) panTo(p);
      return true;
    }
    if (toolGesture && panning) {
      // Space pressed mid-gesture: the pointer pans; the tool resumes when Space is released.
      panTo(e.getLocationOnScreen());
      return true;
    }
    return false;
  }

  public boolean mouseReleased(MouseEvent e) {
    if (replaying) return false;
    if (ownGesture) {
      ownGesture = false;
      final var press = middlePress;
      middlePress = null;
      final var wasPan = panning;
      stopPan();
      if (press != null && !wasPan) replayAsMiddleClick(press, e);
      return true;
    }
    if ((e.getModifiersEx() & buttonsMask()) == 0) {
      toolGesture = false;
      // Space still held after a mid-gesture pan: keep the pan cursor, stop following the pointer.
      if (panning) stopPan();
    }
    return false;
  }

  public boolean mouseWheelMoved(MouseWheelEvent e) {
    if (!ForkPreferences.ZOOM_TO_CURSOR.isEnabled()) return false;
    final var mods = e.getModifiersEx() & MODIFIERS;
    if (mods != AppPreferences.hotkeyMenuMask && mods != InputEvent.CTRL_DOWN_MASK) {
      wheel.reset();
      return false;
    }
    final var steps = wheel.add(e.getPreciseWheelRotation());
    for (var i = 0; i < Math.abs(steps); i++) {
      // Negative rotation (wheel away from the user / fingers up) zooms in.
      ViewActions.zoomStep(canvas, steps < 0, e.getPoint());
    }
    e.consume();
    return true;
  }

  /** The middle button, or on macOS (no middle button on trackpads) Cmd + primary button. */
  private static boolean isMiddlePress(MouseEvent e) {
    if (e.getButton() == MouseEvent.BUTTON2) return true;
    return MacCompatibility.isRunningOnMac()
        && e.getButton() == MouseEvent.BUTTON1
        && (e.getModifiersEx() & MODIFIERS) == InputEvent.META_DOWN_MASK;
  }

  private static int buttonsMask() {
    return InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK;
  }

  /**
   * A middle press that never moved is a click: send it to the canvas as a genuine middle-button
   * press and release, so the mapped tool (Poke by default) and double-click-to-centre behave as
   * before, just on release rather than on press.
   */
  private void replayAsMiddleClick(MouseEvent press, MouseEvent release) {
    final var screen = press.getLocationOnScreen();
    final var at = new Point(screen);
    SwingUtilities.convertPointFromScreen(at, canvas);
    final var clicks = press.getClickCount();
    replaying = true;
    try {
      canvas.dispatchEvent(
          new MouseEvent(
              canvas, MouseEvent.MOUSE_PRESSED, press.getWhen(), InputEvent.BUTTON2_DOWN_MASK,
              at.x, at.y, screen.x, screen.y, clicks, false, MouseEvent.BUTTON2));
      canvas.dispatchEvent(
          new MouseEvent(
              canvas, MouseEvent.MOUSE_RELEASED, release.getWhen(), 0, at.x, at.y, screen.x,
              screen.y, clicks, false, MouseEvent.BUTTON2));
    } finally {
      replaying = false;
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Panning

  private void startPan(Point screen) {
    if (screen == null || canvas.getCanvasPane() == null) return;
    panning = true;
    panStart = screen;
    panStartScrollX = canvas.getHorizontalScrollBar();
    panStartScrollY = canvas.getVerticalScrollBar();
    showPanCursor();
  }

  private void panTo(Point screen) {
    canvas.setScrollBar(
        panStartScrollX + panStart.x - screen.x, panStartScrollY + panStart.y - screen.y);
  }

  private void stopPan() {
    panning = false;
    if (!spaceDown) restoreCursor();
  }

  private void showPanCursor() {
    if (cursorBeforePan == null) cursorBeforePan = canvas.getCursor();
    canvas.setCursor(PAN_CURSOR);
  }

  private void restoreCursor() {
    if (cursorBeforePan == null) return;
    final var tool = canvas.getProject().getTool();
    canvas.setCursor(tool != null ? tool.getCursor() : cursorBeforePan);
    cursorBeforePan = null;
  }

  private static Point pointerOnScreen() {
    final var info = MouseInfo.getPointerInfo();
    return info == null ? null : info.getLocation();
  }
}
