/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.nav;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.fork.edit.SelectConnected;
import com.cburch.logisim.fork.edit.SnapGuides;
import com.cburch.logisim.fork.keymap.ForkShortcuts;
import com.cburch.logisim.fork.wiring.NetHover;
import com.cburch.logisim.fork.wiring.PinFinder;
import com.cburch.logisim.fork.wiring.WireActions;
import com.cburch.logisim.fork.wiring.WireRouter;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.tools.EditTool;
import com.cburch.logisim.tools.WiringTool;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Editing gestures on the circuit canvas (Phases 4 and 5): click-to-route wiring, a larger pick-up
 * area around pins, Alt+click segment delete, double-click net selection, alignment guides while
 * dragging, and the hover highlight. {@link CanvasNavigator} hands it mouse events in circuit
 * coordinates after its own panning checks.
 *
 * <p>A press that might start a route (or delete a segment) is held back. If the pointer then
 * moves, the press is replayed to Logisim so ordinary drag-to-wire and Alt+drag work unchanged;
 * if it is released in place, the fork gesture happens instead.
 */
final class EditingInput {
  private static final int DRAG_THRESHOLD = 4;
  private static final Cursor PIN_CURSOR = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

  private enum Pending { NONE, ROUTE_START, SEGMENT_DELETE }

  private final Canvas canvas;
  private final Consumer<MouseEvent> replay;
  private final WireRouter router;
  private final NetHover hover;
  private final SnapGuides guides;

  private Pending pending = Pending.NONE;
  private MouseEvent pendingPress;
  private Location pendingAt;
  private Wire pendingWire;
  private boolean swallowRelease;
  private Location guidePress;
  private boolean pinCursorShown;

  EditingInput(Canvas canvas, Consumer<MouseEvent> replay) {
    this.canvas = canvas;
    this.replay = replay;
    this.router = new WireRouter(canvas);
    this.hover = new NetHover(canvas);
    this.guides = new SnapGuides(canvas);
    // While routing, Esc/Space/Backspace/Enter/"/" belong to the router, not to keymap shortcuts.
    ForkShortcuts.setModal(canvas, e -> router.isActive() && WireRouter.isRouteKey(e));
  }

  private double zoom() {
    final var frame = canvas.getProject().getFrame();
    return frame == null ? 1.0 : frame.getZoomModel().getZoomFactor();
  }

  private static Location snapped(MouseEvent e) {
    return Location.create(Canvas.snapXToGrid(e.getX()), Canvas.snapYToGrid(e.getY()), false);
  }

  private boolean isEditTool() {
    return canvas.getProject().getTool() instanceof EditTool;
  }

  private boolean isWiringTool() {
    return canvas.getProject().getTool() instanceof WiringTool;
  }

  // --- keys -------------------------------------------------------------------------------

  boolean keyPressed(KeyEvent e) {
    if (!router.isActive()) return false;
    if (router.isStale() || !(isEditTool() || isWiringTool())) {
      router.cancel();
      return false;
    }
    switch (e.getKeyCode()) {
      case KeyEvent.VK_ESCAPE -> router.cancel();
      case KeyEvent.VK_BACK_SPACE -> router.undoPoint();
      case KeyEvent.VK_SPACE, KeyEvent.VK_SLASH -> router.flip();
      case KeyEvent.VK_ENTER -> router.finishAtCursor();
      default -> {
        return false;
      }
    }
    e.consume();
    return true;
  }

  // --- mouse --------------------------------------------------------------------------------

  boolean mousePressed(MouseEvent e) {
    if (router.isActive()) {
      if (router.isStale() || !(isEditTool() || isWiringTool())) {
        router.cancel();
      } else {
        if (e.getButton() == MouseEvent.BUTTON1) {
          router.click(snapped(e), e.getClickCount() >= 2);
        } else {
          router.cancel(); // right-click (context menu) abandons the route
        }
        swallowRelease = true;
        return true;
      }
    }
    if (e.getButton() != MouseEvent.BUTTON1) return false;
    final var edit = isEditTool();
    final var wiring = isWiringTool();
    if (!edit && !wiring) return false;
    final var circuit = canvas.getCircuit();
    if (circuit == null) return false;
    final var mods = e.getModifiersEx() & CanvasNavigator.MODIFIERS;
    final var at = Location.create(e.getX(), e.getY(), false);

    // Alt+click on a wire: delete that run of wire (Alt+drag keeps its stock meaning).
    if (edit && mods == InputEvent.ALT_DOWN_MASK && ForkPreferences.SEGMENT_DELETE.isEnabled()) {
      final var wire = PinFinder.wireNear(circuit, at, wireTolerance());
      if (wire != null) {
        hold(e, Pending.SEGMENT_DELETE, at);
        pendingWire = wire;
        return true;
      }
    }

    // Double-click: select a whole net (wire) or a part with everything wired to it (Shift).
    if (edit && e.getClickCount() >= 2 && ForkPreferences.SELECT_CONNECTED.isEnabled()) {
      if (mods == 0) {
        final var wire = PinFinder.wireNear(circuit, at, wireTolerance());
        if (wire != null) {
          SelectConnected.selectNet(canvas, wire);
          swallowRelease = true;
          return true;
        }
      } else if (mods == InputEvent.SHIFT_DOWN_MASK) {
        final var part = partAt(at);
        if (part != null) {
          SelectConnected.selectWithNeighbours(canvas, part);
          swallowRelease = true;
          return true;
        }
      }
    }

    if (mods != 0) return false;

    // Pressing near a pin or wire end: snap to it, so the press starts a wire, not a move.
    Location pin = null;
    if (edit && ForkPreferences.PIN_WIRING.isEnabled()) {
      pin = PinFinder.nearestConnectable(circuit, at, PinFinder.pinRadius(zoom()));
      if (pin != null) e.translatePoint(pin.getX() - e.getX(), pin.getY() - e.getY());
    }

    // A click (not a drag) on a pin starts click-to-route; with the Wiring tool, anywhere does.
    if (ForkPreferences.CLICK_ROUTE.isEnabled() && e.getClickCount() == 1) {
      final var start = wiring ? snapped(e)
          : pin != null ? pin
          : PinFinder.nearestConnectable(circuit, at, 6);
      if (start != null && (wiring || isRouteStart(start))) {
        hold(e, Pending.ROUTE_START, start);
        return true;
      }
    }

    // Dragging a part: show alignment guides.
    if (edit && pin == null && ForkPreferences.SNAP_GUIDES.isEnabled()) {
      final var part = partAt(at);
      if (part != null) {
        final var sel = canvas.getSelection();
        final var moving = sel.contains(part) ? new ArrayList<>(sel.getComponents()) : new ArrayList<Component>();
        if (moving.isEmpty()) moving.add(part);
        guides.begin(moving);
        guidePress = at;
      }
    }
    return false;
  }

  /** Routes start at pins and at wire ends that lead nowhere yet, not in the middle of a net. */
  private boolean isRouteStart(Location loc) {
    final var circuit = canvas.getCircuit();
    if (!circuit.getNonWires(loc).isEmpty()) return true;
    return circuit.getWires(loc).size() == 1;
  }

  private Component partAt(Location at) {
    for (final var comp : canvas.getCircuit().getAllContaining(at, canvas.getGraphics())) {
      if (!(comp instanceof Wire)) return comp;
    }
    return null;
  }

  private int wireTolerance() {
    return (int) Math.max(3, Math.round(5 / zoom()));
  }

  private void hold(MouseEvent e, Pending kind, Location at) {
    pending = kind;
    pendingPress = e;
    pendingAt = at;
  }

  boolean mouseDragged(MouseEvent e) {
    if (router.isActive()) {
      router.moveCursor(snapped(e));
      return true;
    }
    if (pending != Pending.NONE) {
      if (pendingPress.getLocationOnScreen().distance(e.getLocationOnScreen()) < DRAG_THRESHOLD) {
        return true; // still a click: ignore the jitter
      }
      // It is a drag after all: hand Logisim the original press, then this drag.
      final var press = pendingPress;
      clearPending();
      replay.accept(press);
      return false;
    }
    if (guides.isActive() && guidePress != null) {
      guides.drag(e.getX() - guidePress.getX(), e.getY() - guidePress.getY());
    }
    return false;
  }

  boolean mouseReleased(MouseEvent e) {
    if (swallowRelease) {
      swallowRelease = false;
      return true;
    }
    switch (pending) {
      case ROUTE_START -> {
        final var at = pendingAt;
        clearPending();
        hover.clear();
        router.start(at);
        return true;
      }
      case SEGMENT_DELETE -> {
        final var wire = pendingWire;
        clearPending();
        if (canvas.getCircuit().getWires().contains(wire)) {
          WireActions.deleteSegment(canvas, canvas.getProject(), wire);
        }
        return true;
      }
      default -> {
        // nothing pending
      }
    }
    if (guides.isActive()) {
      guides.end();
      guidePress = null;
    }
    return false;
  }

  private void clearPending() {
    pending = Pending.NONE;
    pendingPress = null;
    pendingAt = null;
    pendingWire = null;
  }

  /** Pointer moves with no button down (circuit coordinates). Observes only. */
  void mouseMoved(MouseEvent e) {
    if (router.isActive()) {
      if (router.isStale()) {
        router.cancel();
      } else {
        router.moveCursor(snapped(e));
        return;
      }
    }
    final var edit = isEditTool();
    final var at = Location.create(e.getX(), e.getY(), false);
    hover.update(at, zoom(), edit || isWiringTool());
    // Crosshair over pins, so it is clear a press there starts a wire.
    final var circuit = canvas.getCircuit();
    final var overPin = edit && circuit != null && ForkPreferences.PIN_WIRING.isEnabled()
        && PinFinder.nearestConnectable(circuit, at, PinFinder.pinRadius(zoom())) != null;
    if (overPin) {
      canvas.setCursor(PIN_CURSOR);
      pinCursorShown = true;
    } else if (pinCursorShown) {
      pinCursorShown = false;
      final var tool = canvas.getProject().getTool();
      if (tool != null) canvas.setCursor(tool.getCursor());
    }
  }

  void focusLost() {
    hover.clear();
  }

  /** Overlay drawing, in circuit coordinates (called by the canvas painter). */
  void paint(Graphics2D g) {
    final var clip = g.getClipBounds();
    NetHover.paintUnconnected(g, canvas.getCircuit(), clip);
    hover.paint(g);
    guides.paint(g);
    router.paint(g);
  }
}
