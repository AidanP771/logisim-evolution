/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.wiring;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Location;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wire connectivity of a circuit, worked out from endpoints only. That is exact for Logisim:
 * {@code WireRepair} splits a wire wherever a pin or another wire's end touches its middle, so
 * connections only ever happen at wire ends.
 *
 * <p>Built from plain segments and pin locations so the logic can be unit tested without a GUI.
 *
 * @param <W> the wire type (Logisim's {@link Wire}, or a test stand-in)
 */
public final class WireGraph<W> {
  /** A wire as two endpoints; {@code wire} is the object it stands for. */
  public record Segment<W>(W wire, Location a, Location b) {
    Location other(Location end) {
      return end.equals(a) ? b : a;
    }
  }

  private final List<Segment<W>> segments;
  private final Map<Location, List<Segment<W>>> byEnd = new HashMap<>();
  private final Map<Location, Integer> pinCount = new HashMap<>();

  public WireGraph(Collection<Segment<W>> segments, Collection<Location> pins) {
    this.segments = List.copyOf(segments);
    for (final var s : this.segments) {
      byEnd.computeIfAbsent(s.a(), k -> new ArrayList<>()).add(s);
      byEnd.computeIfAbsent(s.b(), k -> new ArrayList<>()).add(s);
    }
    for (final var pin : pins) pinCount.merge(pin, 1, Integer::sum);
  }

  /** The graph of {@code circuit}: its wires, and every end of every other component. */
  public static WireGraph<Wire> of(Circuit circuit) {
    final var segs = new ArrayList<Segment<Wire>>();
    for (final var w : circuit.getWires()) segs.add(new Segment<>(w, w.getEnd0(), w.getEnd1()));
    final var pins = new ArrayList<Location>();
    for (final var comp : circuit.getNonWires()) {
      for (final var end : comp.getEnds()) pins.add(end.getLocation());
    }
    return new WireGraph<>(segs, pins);
  }

  public List<Segment<W>> getSegments() {
    return segments;
  }

  public int wireEnds(Location loc) {
    final var list = byEnd.get(loc);
    return list == null ? 0 : list.size();
  }

  public int pins(Location loc) {
    return pinCount.getOrDefault(loc, 0);
  }

  /** A wire end touching nothing else. */
  public boolean isDanglingEnd(Location loc) {
    return wireEnds(loc) == 1 && pins(loc) == 0;
  }

  /** A pin with no wire and no other pin on it. */
  public boolean isUnconnectedPin(Location loc) {
    return pins(loc) == 1 && wireEnds(loc) == 0;
  }

  /** Where a run of wire can continue: exactly two wire ends and no pin (a bend or straight join). */
  private boolean isPassThrough(Location loc) {
    return wireEnds(loc) == 2 && pins(loc) == 0;
  }

  private Segment<W> segmentFor(W wire) {
    for (final var s : segments) {
      if (s.wire() == wire) return s;
    }
    return null;
  }

  /**
   * The run of wire through {@code wire} between the nearest junctions, pins or dangling ends:
   * "one segment" as the user sees it, including its bends.
   */
  public List<W> chainThrough(W wire) {
    final var start = segmentFor(wire);
    if (start == null) return List.of();
    final var chain = new LinkedHashSet<Segment<W>>();
    chain.add(start);
    for (final var end : List.of(start.a(), start.b())) {
      var current = start;
      var at = end;
      while (isPassThrough(at)) {
        Segment<W> next = null;
        for (final var s : byEnd.get(at)) {
          if (s != current) next = s;
        }
        if (next == null || !chain.add(next)) break; // a closed loop of wire
        current = next;
        at = next.other(at);
      }
    }
    return chain.stream().map(Segment::wire).toList();
  }

  /**
   * Wires that lead nowhere: repeatedly strips segments with a dangling end, so a stub with bends
   * goes completely. A wire only between two pins, or part of a connection, is never included.
   */
  public List<W> danglingStubs() {
    final var removed = new LinkedHashSet<Segment<W>>();
    final var ends = new HashMap<Location, Integer>();
    for (final var entry : byEnd.entrySet()) ends.put(entry.getKey(), entry.getValue().size());
    final var queue = new ArrayDeque<Location>();
    for (final var loc : ends.keySet()) {
      if (ends.get(loc) == 1 && pins(loc) == 0) queue.add(loc);
    }
    while (!queue.isEmpty()) {
      final var loc = queue.poll();
      if (ends.getOrDefault(loc, 0) != 1 || pins(loc) != 0) continue;
      for (final var s : byEnd.get(loc)) {
        if (removed.contains(s)) continue;
        removed.add(s);
        for (final var end : List.of(s.a(), s.b())) {
          final var left = ends.merge(end, -1, Integer::sum);
          if (left == 1 && pins(end) == 0) queue.add(end);
        }
        break;
      }
    }
    return removed.stream().map(Segment::wire).toList();
  }

  /**
   * Which pins are wired together: each pin location maps to the set of pin locations on the same
   * net. Used to prove an edit does not change connectivity.
   */
  public Map<Location, Set<Location>> pinNets() {
    final var parent = new HashMap<Location, Location>();
    for (final var s : segments) union(parent, s.a(), s.b());
    final var groups = new HashMap<Location, Set<Location>>();
    for (final var pin : pinCount.keySet()) {
      groups.computeIfAbsent(find(parent, pin), k -> new HashSet<>()).add(pin);
    }
    final var nets = new HashMap<Location, Set<Location>>();
    for (final var pin : pinCount.keySet()) nets.put(pin, groups.get(find(parent, pin)));
    return nets;
  }

  private static Location find(Map<Location, Location> parent, Location loc) {
    var root = loc;
    while (parent.containsKey(root) && !parent.get(root).equals(root)) root = parent.get(root);
    // Path compression.
    var cur = loc;
    while (!cur.equals(root)) {
      final var next = parent.get(cur);
      parent.put(cur, root);
      cur = next;
    }
    return root;
  }

  private static void union(Map<Location, Location> parent, Location a, Location b) {
    parent.putIfAbsent(a, a);
    parent.putIfAbsent(b, b);
    final var ra = find(parent, a);
    final var rb = find(parent, b);
    if (!ra.equals(rb)) parent.put(ra, rb);
  }

  /** The graph with {@code removed} wires taken out. */
  public WireGraph<W> without(Collection<W> removed) {
    final var gone = new HashSet<Object>(removed);
    final var kept = segments.stream().filter(s -> !gone.contains(s.wire())).toList();
    final var pins = new ArrayList<Location>();
    for (final var entry : pinCount.entrySet()) {
      for (var i = 0; i < entry.getValue(); i++) pins.add(entry.getKey());
    }
    return new WireGraph<>(kept, pins);
  }
}
