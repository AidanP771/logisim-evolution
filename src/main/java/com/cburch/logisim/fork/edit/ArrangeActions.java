/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.edit;

import static com.cburch.logisim.fork.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fork.wiring.WireGraph;
import com.cburch.logisim.fork.keymap.ActionContext;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Ram;
import com.cburch.logisim.std.memory.Rom;
import com.cburch.logisim.tools.move.MoveGesture;
import com.cburch.logisim.tools.move.MoveRequestListener;
import com.cburch.logisim.util.StringGetter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.ToIntFunction;

/** Align, distribute and duplicate-with-offset for the selection. Each is one undo step. */
public final class ArrangeActions {
  /** Which edge or centre to line components up on. */
  public enum Edge {
    LEFT, RIGHT, TOP, BOTTOM, CENTER_X, CENTER_Y
  }

  /** Moves are applied straight away, so there is nothing to report back. */
  private static final MoveRequestListener NO_LISTENER = (gesture, dx, dy) -> {};

  private ArrangeActions() {}

  /** Components (not wires) in the selection; arranging moves these and wires follow. */
  static List<Component> selectedComponents(Canvas canvas) {
    final var list = new ArrayList<Component>();
    for (final var comp : canvas.getSelection().getComponents()) {
      if (!(comp instanceof Wire)) list.add(comp);
    }
    return list;
  }

  public static boolean canAlign(ActionContext ctx) {
    return ctx.isEditTool() && selectedComponents(ctx.canvas()).size() >= 2;
  }

  public static boolean canDistribute(ActionContext ctx) {
    return ctx.isEditTool() && selectedComponents(ctx.canvas()).size() >= 3;
  }

  // --- pure layout maths (unit tested) -----------------------------------------------------

  /** Rounds to the nearest grid step, so aligned components stay on the grid. */
  static int snap(double value) {
    return (int) Math.round(value / EditActions.GRID) * EditActions.GRID;
  }

  static int coordinate(Bounds b, Edge edge) {
    return switch (edge) {
      case LEFT -> b.getX();
      case RIGHT -> b.getX() + b.getWidth();
      case TOP -> b.getY();
      case BOTTOM -> b.getY() + b.getHeight();
      case CENTER_X -> b.getX() + b.getWidth() / 2;
      case CENTER_Y -> b.getY() + b.getHeight() / 2;
    };
  }

  static boolean horizontal(Edge edge) {
    return edge == Edge.LEFT || edge == Edge.RIGHT || edge == Edge.CENTER_X;
  }

  /** Offsets (dx or dy, grid-snapped) that line {@code bounds} up on {@code edge}. */
  static int[] alignOffsets(List<Bounds> bounds, Edge edge) {
    var target = coordinate(bounds.get(0), edge);
    for (final var b : bounds) {
      final var c = coordinate(b, edge);
      target = switch (edge) {
        case LEFT, TOP -> Math.min(target, c);
        case RIGHT, BOTTOM -> Math.max(target, c);
        default -> target; // centres: the first component's centre (set below)
      };
    }
    if (edge == Edge.CENTER_X || edge == Edge.CENTER_Y) {
      // Line up on the middle of the whole group.
      var min = Integer.MAX_VALUE;
      var max = Integer.MIN_VALUE;
      for (final var b : bounds) {
        min = Math.min(min, coordinate(b, edge));
        max = Math.max(max, coordinate(b, edge));
      }
      target = (min + max) / 2;
    }
    final var offsets = new int[bounds.size()];
    for (var i = 0; i < bounds.size(); i++) offsets[i] = snap(target - coordinate(bounds.get(i), edge));
    return offsets;
  }

  /** Offsets that space the centres of {@code bounds} evenly between the two outermost. */
  static int[] distributeOffsets(List<Bounds> bounds, boolean horizontal) {
    final var edge = horizontal ? Edge.CENTER_X : Edge.CENTER_Y;
    final var order = new ArrayList<Integer>();
    for (var i = 0; i < bounds.size(); i++) order.add(i);
    final ToIntFunction<Integer> centre = i -> coordinate(bounds.get(i), edge);
    order.sort(Comparator.comparingInt(centre));
    final var first = centre.applyAsInt(order.get(0));
    final var last = centre.applyAsInt(order.get(order.size() - 1));
    final var step = (double) (last - first) / (order.size() - 1);
    final var offsets = new int[bounds.size()];
    for (var rank = 1; rank < order.size() - 1; rank++) {
      final var i = order.get(rank);
      offsets[i] = snap(first + rank * step - centre.applyAsInt(i));
    }
    return offsets;
  }

  // --- actions ------------------------------------------------------------------------------

  public static void align(ActionContext ctx, Edge edge) {
    final var comps = selectedComponents(ctx.canvas());
    final var bounds = comps.stream().map(Component::getBounds).toList();
    final var offsets = alignOffsets(bounds, edge);
    move(ctx, comps, offsets, horizontal(edge), S.getter("forkAlignAction"));
  }

  public static void distribute(ActionContext ctx, boolean horizontal) {
    final var comps = selectedComponents(ctx.canvas());
    final var bounds = comps.stream().map(Component::getBounds).toList();
    final var offsets = distributeOffsets(bounds, horizontal);
    move(ctx, comps, offsets, horizontal, S.getter("forkDistributeAction"));
  }

  /**
   * Moves each component by its own offset, as one undo step; attached wires follow. Moving parts
   * around can drop a pin onto an unrelated wire; if the result changes which pins are connected,
   * it is undone at once and the user is told, so arranging never rewires a circuit.
   */
  private static void move(
      ActionContext ctx, List<Component> comps, int[] offsets, boolean horizontal, StringGetter name) {
    final var canvas = ctx.canvas();
    final var circuit = canvas.getCircuit();
    final var before = connectivity(circuit, Map.of());
    Action combined = null;
    final var steps = new ArrayList<MoveStep>();
    for (var i = 0; i < comps.size(); i++) {
      if (offsets[i] == 0) continue;
      final var step = new MoveStep(canvas, comps.get(i), horizontal ? offsets[i] : 0,
          horizontal ? 0 : offsets[i], name);
      steps.add(step);
      combined = combined == null ? step : combined.append(step);
    }
    if (combined == null) return;
    ctx.project().doAction(combined);
    final var alias = new IdentityHashMap<Component, Component>();
    for (final var step : steps) {
      if (step.moved != null) alias.put(step.moved, step.comp);
    }
    if (!connectivity(circuit, alias).equals(before)) {
      ctx.project().undoAction();
      canvas.setErrorMessage(S.getter("forkArrangeRefused"));
    }
  }

  /**
   * Which component pins share a wire net, with moved components counted as their originals
   * ({@code alias}), so a pure move compares equal.
   */
  static Set<Set<String>> connectivity(Circuit circuit, Map<Component, Component> alias) {
    final var ids = new IdentityHashMap<Component, Integer>();
    final var portsAt = new HashMap<Location, List<String>>();
    for (final var comp : circuit.getNonWires()) {
      final var original = alias.getOrDefault(comp, comp);
      final var id = ids.computeIfAbsent(original, c -> System.identityHashCode(c));
      final var ends = comp.getEnds();
      for (var i = 0; i < ends.size(); i++) {
        portsAt.computeIfAbsent(ends.get(i).getLocation(), k -> new ArrayList<>()).add(id + "#" + i);
      }
    }
    final var groups = new HashSet<Set<String>>();
    for (final var net : WireGraph.of(circuit).pinNets().values()) {
      final var group = new TreeSet<String>();
      for (final var loc : net) group.addAll(portsAt.getOrDefault(loc, List.of()));
      groups.add(group);
    }
    // Pins plugged straight into each other (no wire) also count as connected.
    for (final var ports : portsAt.values()) {
      if (ports.size() > 1) groups.add(new TreeSet<>(ports));
    }
    return groups;
  }

  /**
   * Moves one component, rerouting its wires the way dragging does. Built when it first runs, so
   * in a sequence each step sees the wires as the previous step left them.
   */
  private static final class MoveStep extends CircuitEditAction {
    private final Canvas canvas;
    final Component comp;
    private final int dx;
    private final int dy;
    Component moved;

    MoveStep(Canvas canvas, Component comp, int dx, int dy, StringGetter name) {
      super(name);
      this.canvas = canvas;
      this.comp = comp;
      this.dx = dx;
      this.dy = dy;
    }

    @Override
    protected Circuit circuit(Project proj) {
      return canvas.getCircuit();
    }

    @Override
    protected void build(Circuit circuit, CircuitMutation xn) {
      final var loc = comp.getLocation();
      moved = comp.getFactory().createComponent(
          Location.create(loc.getX() + dx, loc.getY() + dy, false), comp.getAttributeSet());
      xn.replace(comp, moved);
      if (AppPreferences.MOVE_KEEP_CONNECT.getBoolean()) {
        final var gesture = new MoveGesture(NO_LISTENER, circuit, List.of(comp));
        final var repl = gesture.forceRequest(dx, dy).getReplacementMap();
        if (repl != null) xn.replace(repl);
      }
    }

    @Override
    protected void afterForward(Project proj) {
      // Keep the moved component selected, as a drag would.
      if (moved != null) canvas.getSelection().add(moved);
    }
  }

  // --- duplicate ----------------------------------------------------------------------------

  /**
   * Edit → Duplicate (Mod+D) with the fork flag on: copies the selection one grid step down and to
   * the right and selects the copy, instead of pasting under the mouse pointer.
   *
   * @return true if handled; false lets Logisim's own duplicate run
   */
  public static boolean duplicateWithOffset(Canvas canvas, Project proj) {
    final var sel = canvas.getSelection();
    // Pasted-but-not-dropped ("floating") components keep the stock behaviour.
    if (sel.isEmpty() || !sel.getFloatingComponents().isEmpty()) return false;
    proj.doAction(new Duplicate(canvas, new ArrayList<>(sel.getComponents())));
    return true;
  }

  private static final class Duplicate extends CircuitEditAction {
    private final Canvas canvas;
    private final List<Component> originals;
    private final List<Component> copies = new ArrayList<>();

    Duplicate(Canvas canvas, List<Component> originals) {
      super(S.getter("forkDuplicateAction"));
      this.canvas = canvas;
      this.originals = originals;
    }

    @Override
    protected Circuit circuit(Project proj) {
      return canvas.getCircuit();
    }

    @Override
    protected void build(Circuit circuit, CircuitMutation xn) {
      for (final var comp : originals) {
        // Same as SelectionBase.copyComponents: memories share their contents, others are cloned.
        final var factory = comp.getFactory();
        final var attrs =
            factory instanceof Rom || factory instanceof Ram
                ? comp.getAttributeSet()
                : (AttributeSet) comp.getAttributeSet().clone();
        var x = comp.getLocation().getX() + EditActions.GRID;
        var y = comp.getLocation().getY() + EditActions.GRID;
        final var snapFeature = factory.getFeature(ComponentFactory.SHOULD_SNAP, attrs);
        if (snapFeature == null || (Boolean) snapFeature) {
          x = Canvas.snapXToGrid(x);
          y = Canvas.snapYToGrid(y);
        }
        final var copy = factory.createComponent(Location.create(x, y, false), attrs);
        copies.add(copy);
        xn.add(copy);
      }
    }

    @Override
    protected void afterForward(Project proj) {
      final var sel = canvas.getSelection();
      final var drop = com.cburch.logisim.gui.main.SelectionActions.dropAll(sel);
      if (drop != null) drop.doIt(proj);
      sel.addAll(copies);
    }
  }
}
