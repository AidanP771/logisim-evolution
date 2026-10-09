/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.keymap;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The active key bindings: each action's defaults, with the user's {@code keymap.json} applied on
 * top.
 *
 * <p>The file looks like this; {@code null} or {@code []} unbinds an action:
 *
 * <pre>{@code
 * { "bindings": { "tool.and": "Shift+G", "view.fit": ["F", "Mod+Shift+F"], "tool.xor": null } }
 * }</pre>
 *
 * <p>Malformed entries, unknown action ids and bindings already used by another action are
 * skipped and reported through {@link #getProblems()}; they never stop the rest from loading.
 */
public final class Keymap {
  private final Map<ForkAction, List<KeyBinding>> bindings = new LinkedHashMap<>();
  private final List<String> problems = new ArrayList<>();

  private Keymap() {}

  /** Defaults only. */
  public static Keymap defaults(List<ForkAction> actions, int menuMask) {
    final var keymap = new Keymap();
    for (final var action : actions) {
      final var list = new ArrayList<KeyBinding>();
      if (!action.isRemappable()) {
        // INFO entries describe gestures ("Space + drag"); their text is shown as is.
        keymap.bindings.put(action, list);
        continue;
      }
      for (final var text : action.getDefaultBindings()) {
        try {
          final var binding = KeyBinding.parse(text, menuMask);
          // On Windows/Linux "Mod" is Ctrl, so "Mod+=" and "Ctrl+=" are the same binding.
          if (!list.contains(binding)) list.add(binding);
        } catch (IllegalArgumentException e) {
          keymap.problems.add("default binding of " + action.getId() + ": " + e.getMessage());
        }
      }
      keymap.bindings.put(action, list);
    }
    return keymap;
  }

  /** Defaults with the overrides in {@code json} (the content of keymap.json) applied. */
  public static Keymap load(List<ForkAction> actions, int menuMask, String json) {
    final var keymap = defaults(actions, menuMask);
    if (json == null || json.isBlank()) return keymap;
    final Object root;
    try {
      root = MiniJson.parse(json);
    } catch (IllegalArgumentException e) {
      keymap.problems.add("keymap.json ignored: " + e.getMessage());
      return keymap;
    }
    if (!(root instanceof Map<?, ?> rootMap) || !(rootMap.get("bindings") instanceof Map<?, ?> map)) {
      keymap.problems.add("keymap.json ignored: expected {\"bindings\": {...}}");
      return keymap;
    }
    final var byId = new LinkedHashMap<String, ForkAction>();
    for (final var action : actions) byId.put(action.getId(), action);
    for (final var entry : map.entrySet()) {
      final var id = String.valueOf(entry.getKey());
      final var action = byId.get(id);
      if (action == null) {
        keymap.problems.add("unknown action '" + id + "' skipped");
        continue;
      }
      if (!action.isRemappable()) {
        keymap.problems.add("'" + id + "' cannot be rebound; skipped");
        continue;
      }
      final List<String> texts = new ArrayList<>();
      final var value = entry.getValue();
      if (value instanceof String s) {
        texts.add(s);
      } else if (value instanceof List<?> list) {
        var ok = true;
        for (final var item : list) {
          if (item instanceof String s) {
            texts.add(s);
          } else {
            ok = false;
          }
        }
        if (!ok) {
          keymap.problems.add("'" + id + "': bindings must be strings; entry skipped");
          continue;
        }
      } else if (value != null) {
        keymap.problems.add("'" + id + "': expected a string, a list or null; entry skipped");
        continue;
      }
      keymap.override(action, texts, menuMask);
    }
    return keymap;
  }

  private void override(ForkAction action, List<String> texts, int menuMask) {
    final var list = new ArrayList<KeyBinding>();
    // Free this action's own defaults first, so swapping two actions' keys works in either order.
    bindings.put(action, list);
    for (final var text : texts) {
      final KeyBinding binding;
      try {
        binding = KeyBinding.parse(text, menuMask);
      } catch (IllegalArgumentException e) {
        problems.add("'" + action.getId() + "': " + e.getMessage() + "; skipped");
        continue;
      }
      final var owner = findOwner(binding);
      if (owner != null && owner != action) {
        problems.add(
            "'" + action.getId() + "': " + text + " is already used by '" + owner.getId()
                + "'; skipped");
        continue;
      }
      if (!list.contains(binding)) list.add(binding);
    }
  }

  private ForkAction findOwner(KeyBinding binding) {
    for (final var entry : bindings.entrySet()) {
      if (entry.getValue().contains(binding)) return entry.getKey();
    }
    return null;
  }

  public List<KeyBinding> getBindings(ForkAction action) {
    return Collections.unmodifiableList(bindings.getOrDefault(action, List.of()));
  }

  /** Actions bound to {@code event}, in registry order (normally at most one). */
  public List<ForkAction> find(KeyEvent event) {
    final var found = new ArrayList<ForkAction>();
    for (final var entry : bindings.entrySet()) {
      if (!entry.getKey().isRemappable()) continue;
      for (final var binding : entry.getValue()) {
        if (binding.matches(event)) {
          found.add(entry.getKey());
          break;
        }
      }
    }
    return found;
  }

  /** Human-readable problems found while loading, for the log and the cheat sheet. */
  public List<String> getProblems() {
    return Collections.unmodifiableList(problems);
  }
}
