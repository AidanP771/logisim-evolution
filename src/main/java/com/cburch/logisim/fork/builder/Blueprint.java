/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.builder;

import static com.cburch.logisim.fork.Strings.S;

import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.fork.keymap.MiniJson;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A saved group of parts and wires ({@code build.blueprints}), kept as JSON in the fork's config
 * folder, never in circuit files. Parts are stored by library and factory name, position relative
 * to the group's top-left grid point, and attributes in Logisim's standard string form, so they
 * are rebuilt the way pasting rebuilds them. Subcircuits are stored by circuit name and only
 * resolve in a project that has a circuit of that name.
 */
public record Blueprint(String name, List<Part> parts, List<WireSpec> wires) {
  /** Library name used for the project's own circuits. */
  static final String CIRCUIT_LIBRARY = "#circuit";

  /** One part: where its factory lives, its offset from the origin and its attributes. */
  public record Part(String library, String factory, int dx, int dy, Map<String, String> attrs) {}

  /** One wire, relative to the origin. */
  public record WireSpec(int x0, int y0, int x1, int y1) {}

  /** A part ready to place: factory, attributes and offset. */
  public record Resolved(ComponentFactory factory, AttributeSet attrs, int dx, int dy) {}

  /** What a blueprint becomes in a given project. */
  public record Placement(List<Resolved> parts, List<WireSpec> wires) {}

  /** Captures {@code components} (parts and wires) from a circuit of {@code file}. */
  public static Blueprint capture(String name, Collection<Component> components, LogisimFile file) {
    var minX = Integer.MAX_VALUE;
    var minY = Integer.MAX_VALUE;
    for (final var c : components) {
      if (c instanceof Wire w) {
        minX = Math.min(minX, Math.min(w.getEnd0().getX(), w.getEnd1().getX()));
        minY = Math.min(minY, Math.min(w.getEnd0().getY(), w.getEnd1().getY()));
      } else {
        minX = Math.min(minX, c.getLocation().getX());
        minY = Math.min(minY, c.getLocation().getY());
      }
    }
    final var ox = Math.floorDiv(minX, 10) * 10;
    final var oy = Math.floorDiv(minY, 10) * 10;
    final var parts = new ArrayList<Part>();
    final var wires = new ArrayList<WireSpec>();
    for (final var c : components) {
      if (c instanceof Wire w) {
        wires.add(new WireSpec(w.getEnd0().getX() - ox, w.getEnd0().getY() - oy,
            w.getEnd1().getX() - ox, w.getEnd1().getY() - oy));
        continue;
      }
      final var factory = c.getFactory();
      final String library;
      final String factoryName;
      if (factory instanceof SubcircuitFactory sub) {
        library = CIRCUIT_LIBRARY;
        factoryName = sub.getSubcircuit().getName();
      } else {
        final var lib = libraryOf(file.getLibraries(), factory);
        if (lib == null) throw new IllegalArgumentException(S.get("forkBlueprintUnknownPart",
            factory.getDisplayName()));
        library = lib.getName();
        factoryName = factory.getName();
      }
      final var attrs = new LinkedHashMap<String, String>();
      final var set = c.getAttributeSet();
      for (final var attr : set.getAttributes()) {
        if (!set.isToSave(attr)) continue;
        final var value = set.getValue(attr);
        if (value != null) attrs.put(attr.getName(), standardString(attr, value));
      }
      parts.add(new Part(library, factoryName, c.getLocation().getX() - ox,
          c.getLocation().getY() - oy, attrs));
    }
    return new Blueprint(name, parts, wires);
  }

  @SuppressWarnings("unchecked")
  private static <V> String standardString(Attribute<V> attr, Object value) {
    return attr.toStandardString((V) value);
  }

  /** The library (searching sub-libraries too) holding a tool for {@code factory}. */
  private static Library libraryOf(List<Library> libraries, ComponentFactory factory) {
    for (final var lib : libraries) {
      final var tool = lib.getTool(factory.getName());
      if (tool instanceof AddTool add && add.getFactory() == factory) return lib;
      final var inner = libraryOf(lib.getLibraries(), factory);
      if (inner != null) return inner;
    }
    return null;
  }

  private static Library findLibrary(List<Library> libraries, String name) {
    for (final var lib : libraries) {
      if (lib.getName().equals(name)) return lib;
      final var inner = findLibrary(lib.getLibraries(), name);
      if (inner != null) return inner;
    }
    return null;
  }

  /** Rebuilds the parts in {@code file}; throws with a message naming what is missing. */
  public Placement resolve(LogisimFile file) {
    final var resolved = new ArrayList<Resolved>();
    for (final var part : parts) {
      final ComponentFactory factory;
      if (CIRCUIT_LIBRARY.equals(part.library())) {
        final var circuit = file.getCircuit(part.factory());
        if (circuit == null) {
          throw new IllegalArgumentException(S.get("forkBlueprintNoCircuit", part.factory()));
        }
        factory = circuit.getSubcircuitFactory();
      } else {
        final var lib = findLibrary(file.getLibraries(), part.library());
        final var tool = lib == null ? null : lib.getTool(part.factory());
        if (!(tool instanceof AddTool add) || add.getFactory() == null) {
          throw new IllegalArgumentException(
              S.get("forkBlueprintNoPart", part.factory(), part.library()));
        }
        factory = add.getFactory();
      }
      final var attrs = factory.createAttributeSet();
      for (final var e : part.attrs().entrySet()) {
        final var attr = attrs.getAttribute(e.getKey());
        if (attr == null) continue;
        try {
          setParsed(attrs, attr, e.getValue());
        } catch (RuntimeException ignored) {
          // An attribute this version can't read keeps its default, as when loading a file.
        }
      }
      resolved.add(new Resolved(factory, attrs, part.dx(), part.dy()));
    }
    return new Placement(resolved, wires);
  }

  private static <V> void setParsed(AttributeSet attrs, Attribute<V> attr, String text) {
    attrs.setValue(attr, attr.parse(text));
  }

  /** The JSON form written to {@code blueprints/<name>.json}. */
  public String toJson() {
    final var root = new LinkedHashMap<String, Object>();
    root.put("name", name);
    root.put("version", 1);
    final var partList = new ArrayList<Object>();
    for (final var p : parts) {
      final var m = new LinkedHashMap<String, Object>();
      m.put("library", p.library());
      m.put("factory", p.factory());
      m.put("x", p.dx());
      m.put("y", p.dy());
      m.put("attributes", new LinkedHashMap<String, Object>(p.attrs()));
      partList.add(m);
    }
    root.put("parts", partList);
    final var wireList = new ArrayList<Object>();
    for (final var w : wires) wireList.add(List.of(w.x0(), w.y0(), w.x1(), w.y1()));
    root.put("wires", wireList);
    return MiniJson.write(root);
  }

  /** Reads {@link #toJson()} output; throws IllegalArgumentException if it is not a blueprint. */
  public static Blueprint fromJson(String json) {
    if (!(MiniJson.parse(json) instanceof Map<?, ?> root)) {
      throw new IllegalArgumentException("not a blueprint");
    }
    final var name = String.valueOf(root.get("name"));
    final var parts = new ArrayList<Part>();
    if (root.get("parts") instanceof List<?> list) {
      for (final var item : list) {
        if (!(item instanceof Map<?, ?> m)) continue;
        final var attrs = new LinkedHashMap<String, String>();
        if (m.get("attributes") instanceof Map<?, ?> a) {
          for (final var e : a.entrySet()) attrs.put(String.valueOf(e.getKey()),
              String.valueOf(e.getValue()));
        }
        parts.add(new Part(String.valueOf(m.get("library")), String.valueOf(m.get("factory")),
            num(m.get("x")), num(m.get("y")), attrs));
      }
    }
    final var wires = new ArrayList<WireSpec>();
    if (root.get("wires") instanceof List<?> list) {
      for (final var item : list) {
        if (item instanceof List<?> w && w.size() == 4) {
          wires.add(new WireSpec(num(w.get(0)), num(w.get(1)), num(w.get(2)), num(w.get(3))));
        }
      }
    }
    return new Blueprint(name, parts, wires);
  }

  private static int num(Object o) {
    if (o instanceof Number n) return n.intValue();
    throw new IllegalArgumentException("expected a number, got " + o);
  }

  /** Where a wire goes when the blueprint's origin is at {@code at}. */
  static Location[] place(WireSpec w, Location at) {
    return new Location[] {
      Location.create(at.getX() + w.x0(), at.getY() + w.y0(), false),
      Location.create(at.getX() + w.x1(), at.getY() + w.y1(), false)
    };
  }
}
