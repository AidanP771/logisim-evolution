/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.nav;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.generic.ZoomControl;
import com.cburch.logisim.gui.generic.ZoomModel;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Project;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * View navigation used by the fork's shortcuts. Every zoom change goes through the frame's
 * {@link ZoomModel}, so the toolbar zoom control stays in sync.
 */
public final class ViewActions {
  private ViewActions() {}

  private static ZoomModel zoomModel(Canvas canvas) {
    final var frame = canvas.getProject().getFrame();
    return frame == null ? null : frame.getZoomModel();
  }

  /** Zooms one level in or out, keeping {@code anchor} (canvas pixels) fixed, or the centre if null. */
  public static void zoomStep(Canvas canvas, boolean zoomIn, Point anchor) {
    final var model = zoomModel(canvas);
    if (model == null) return;
    final var next = ZoomSteps.next(model.getZoomFactor(), model.getZoomOptions(), zoomIn);
    setZoom(canvas, model, next, anchor);
  }

  /**
   * Multiplies the zoom by {@code factor} (continuous, e.g. a trackpad pinch), clamped to the
   * model's range, keeping {@code anchor} (canvas pixels) fixed, or the centre if null.
   */
  public static void zoomBy(Canvas canvas, double factor, Point anchor) {
    final var model = zoomModel(canvas);
    if (model == null || factor <= 0) return;
    final var options = model.getZoomOptions();
    if (options == null || options.isEmpty()) return;
    final var min = options.get(0) / 100.0;
    final var max = options.get(options.size() - 1) / 100.0;
    final var next = Math.max(min, Math.min(max, model.getZoomFactor() * factor));
    setZoom(canvas, model, next, anchor);
  }

  /** Sets zoom to 100%. */
  public static void resetZoom(Canvas canvas) {
    final var model = zoomModel(canvas);
    if (model != null) model.setZoomFactorCenter(1.0);
  }

  private static void setZoom(Canvas canvas, ZoomModel model, double value, Point anchor) {
    if (value == model.getZoomFactor()) return;
    if (anchor == null) {
      model.setZoomFactorCenter(value);
    } else {
      // BasicZoomModel converts this event to viewport coordinates and keeps that point fixed.
      final var event =
          new MouseEvent(
              canvas, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, anchor.x, anchor.y, 0,
              false);
      model.setZoomFactor(value, event);
    }
  }

  /** Fits the whole circuit in the window. Does nothing for an empty circuit. */
  public static void fitAll(Canvas canvas) {
    final Circuit circuit = canvas.getCircuit();
    if (circuit == null) return;
    final var g = canvas.getGraphics();
    fitBounds(canvas, g != null ? circuit.getBounds(g) : circuit.getBounds());
  }

  /** Fits the selection in the window. Does nothing when nothing is selected. */
  public static void fitSelection(Canvas canvas) {
    final var selection = canvas.getSelection();
    if (selection == null || selection.isEmpty()) return;
    final var g = canvas.getGraphics();
    fitBounds(canvas, g != null ? selection.getBounds(g) : selection.getBounds());
  }

  /** Zooms so {@code bounds} fills the viewport (with the toolbar's padding) and centres it. */
  static void fitBounds(Canvas canvas, Bounds bounds) {
    if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) return;
    final var model = zoomModel(canvas);
    final var pane = canvas.getCanvasPane();
    if (model == null || pane == null) return;
    final var viewport = pane.getViewport();
    final var size = viewport.getExtentSize();
    final var zoom = ZoomControl.computeFitZoomFactor(bounds, size, model.getZoomOptions());
    if (Double.isNaN(zoom)) return;
    if (zoom != model.getZoomFactor()) {
      // CanvasPane resizes the canvas synchronously on a zoom change; lay the viewport out now so
      // the scroll position below is not clamped to the old size (as BasicZoomModel does).
      model.setZoomFactor(zoom);
      viewport.doLayout();
    }
    final var scroll = centredScroll(bounds, zoom, size.width, size.height);
    canvas.setScrollBar(scroll.x, scroll.y);
  }

  /** Scroll position (pixels) that puts the centre of {@code bounds} in the middle of the view. */
  static Point centredScroll(Bounds bounds, double zoom, int viewWidth, int viewHeight) {
    final var centreX = (bounds.getX() + bounds.getWidth() / 2.0) * zoom;
    final var centreY = (bounds.getY() + bounds.getHeight() / 2.0) * zoom;
    return new Point(
        (int) Math.round(centreX - viewWidth / 2.0), (int) Math.round(centreY - viewHeight / 2.0));
  }

  /**
   * Goes back up after descending into a subcircuit: to the parent simulation state if there is
   * one, otherwise to the previously viewed circuit.
   *
   * @return whether anything changed
   */
  public static boolean backToParent(Project project) {
    final var state = project.getCircuitState();
    if (state != null && state.getParentState() != null) {
      project.setCircuitState(state.getParentState());
      return true;
    }
    final var recent = CircuitHistory.forProject(project).getRecent();
    final var current = project.getCurrentCircuit();
    for (final var circuit : recent) {
      if (circuit != current) {
        project.setCurrentCircuit(circuit);
        return true;
      }
    }
    return false;
  }
}
