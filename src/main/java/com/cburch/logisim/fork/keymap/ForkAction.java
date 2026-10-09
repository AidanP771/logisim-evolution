/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.keymap;

import static com.cburch.logisim.fork.Strings.S;

import com.cburch.logisim.fork.ForkFlag;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A named fork command (such as {@code view.fit}) with its default key bindings.
 *
 * <p>Actions are listed in {@link ForkActions}; bindings can be changed in {@code keymap.json}
 * (see {@link Keymap}). The command palette, the shortcut dispatcher and the cheat sheet all read
 * this one registry.
 */
public final class ForkAction {
  /** Groups actions in the cheat sheet. */
  public enum Category {
    VIEW("forkCatView"),
    NAVIGATE("forkCatNavigate"),
    TOOLS("forkCatTools"),
    EDIT("forkCatEdit"),
    ARRANGE("forkCatArrange"),
    WIRING("forkCatWiring"),
    HELP("forkCatHelp");

    private final String labelKey;

    Category(String labelKey) {
      this.labelKey = labelKey;
    }

    public String getLabel() {
      return S.get(labelKey);
    }
  }

  /** Where a shortcut applies. */
  public enum Scope {
    /** Only while the circuit canvas has focus and nothing is taking typed text. */
    CANVAS,
    /** Anywhere in the main window, except plain keys while a text field has focus. */
    WINDOW,
    /** Not dispatched as a key (mouse gestures or modal keys); listed in the cheat sheet only. */
    INFO
  }

  private final String id;
  private final Category category;
  private final Scope scope;
  private final ForkFlag flag;
  private final List<String> defaultBindings;
  private final Predicate<ActionContext> available;
  private final Consumer<ActionContext> handler;

  ForkAction(
      String id,
      Category category,
      Scope scope,
      ForkFlag flag,
      List<String> defaultBindings,
      Predicate<ActionContext> available,
      Consumer<ActionContext> handler) {
    this.id = id;
    this.category = category;
    this.scope = scope;
    this.flag = flag;
    this.defaultBindings = List.copyOf(defaultBindings);
    this.available = available;
    this.handler = handler;
  }

  /** Stable identifier, used as the key in {@code keymap.json}. */
  public String getId() {
    return id;
  }

  public Category getCategory() {
    return category;
  }

  public Scope getScope() {
    return scope;
  }

  /** The feature flag that switches this action off, or null if it is always on. */
  public ForkFlag getFlag() {
    return flag;
  }

  /** Default bindings as text, such as {@code "Mod+Shift+P"}. */
  public List<String> getDefaultBindings() {
    return defaultBindings;
  }

  /** Whether {@link #getScope()} lets the keymap rebind this action. */
  public boolean isRemappable() {
    return scope != Scope.INFO;
  }

  public String getLabel() {
    return S.get("forkAction." + id);
  }

  public boolean isEnabled() {
    return flag == null || flag.isEnabled();
  }

  /**
   * Whether the action applies right now. When it does not, its key falls through to Logisim's
   * own handling (for example arrows without a selection).
   */
  public boolean isAvailable(ActionContext context) {
    return isEnabled() && handler != null && available.test(context);
  }

  public void run(ActionContext context) {
    handler.accept(context);
  }

  @Override
  public String toString() {
    return id;
  }
}
