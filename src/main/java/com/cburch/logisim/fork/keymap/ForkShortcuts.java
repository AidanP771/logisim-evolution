/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.keymap;

import com.cburch.logisim.gui.main.Frame;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import javax.swing.text.JTextComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs fork shortcuts from the active {@link Keymap}.
 *
 * <p>Watches key events through a {@link KeyEventDispatcher}, which sees them before components and
 * menus do. A binding only fires when its action {@linkplain ForkAction#isAvailable is available};
 * otherwise the key reaches Logisim untouched, so with every flag off the stock behaviour remains.
 */
public final class ForkShortcuts implements KeyEventDispatcher {
  private static final Logger logger = LoggerFactory.getLogger(ForkShortcuts.class);
  private static ForkShortcuts instance;

  /** Canvases in a modal fork mode (such as click-to-route wiring) that need every key. */
  private static final Map<Object, Predicate<KeyEvent>> MODAL = new WeakHashMap<>();

  /** After a pressed key ran an action, its typed character must not reach the canvas too. */
  private char swallowTyped = KeyEvent.CHAR_UNDEFINED;

  private ForkShortcuts() {}

  public static synchronized void install() {
    if (instance != null) return;
    instance = new ForkShortcuts();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(instance);
  }

  /**
   * Lets {@code owner} (a canvas) claim keys while a modal mode is active: shortcuts are skipped
   * for events the predicate accepts. Pass null to release.
   */
  public static synchronized void setModal(Object owner, Predicate<KeyEvent> claims) {
    if (claims == null) {
      MODAL.remove(owner);
    } else {
      MODAL.put(owner, claims);
    }
  }

  private static synchronized boolean claimedByModal(Object canvas, KeyEvent e) {
    final var claims = MODAL.get(canvas);
    return claims != null && claims.test(e);
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent e) {
    if (e.getID() == KeyEvent.KEY_TYPED && swallowTyped != KeyEvent.CHAR_UNDEFINED) {
      final var swallow = Character.toLowerCase(e.getKeyChar()) == swallowTyped;
      swallowTyped = KeyEvent.CHAR_UNDEFINED;
      if (swallow) return true;
    }
    if (e.getID() != KeyEvent.KEY_PRESSED && e.getID() != KeyEvent.KEY_TYPED) return false;
    final var kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    if (!(kfm.getFocusedWindow() instanceof Frame frame)) return false;
    return handle(frame, kfm.getFocusOwner(), e);
  }

  /**
   * Runs the shortcut bound to {@code e}, if any applies, for a main window whose keyboard focus
   * is on {@code focus}. Returns true (and consumes the event) when an action ran.
   */
  public boolean handle(Frame frame, Object focus, KeyEvent e) {
    if (frame.getProject() == null) return false;
    final var canvas = frame.getCanvas();
    if (canvas != null && claimedByModal(canvas, e)) return false;

    for (final var action : KeymapStore.get().find(e)) {
      if (!applies(action, e, focus, canvas)) continue;
      final var ctx = ActionContext.of(frame);
      if (!action.isAvailable(ctx)) continue;
      try {
        action.run(ctx);
      } catch (RuntimeException ex) {
        logger.error("Fork action {} failed", action.getId(), ex);
      }
      if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
        swallowTyped = Character.toLowerCase(e.getKeyChar());
      }
      e.consume();
      return true;
    }
    return false;
  }

  /** The installed dispatcher, or null before the first canvas exists. */
  public static synchronized ForkShortcuts getInstance() {
    return instance;
  }

  private static boolean applies(ForkAction action, KeyEvent e, Object focus, Object canvas) {
    final var plain =
        (e.getModifiersEx() & KeyBinding.MODIFIER_MASK & ~InputEvent.SHIFT_DOWN_MASK) == 0;
    return switch (action.getScope()) {
      case CANVAS ->
          canvas != null
              && focus == canvas
              && !TypingGuard.isTyping((com.cburch.logisim.gui.main.Canvas) canvas);
      case WINDOW -> {
        // Plain keys belong to text fields (attribute table, filters) and to canvas text entry.
        if (plain && focus instanceof JTextComponent) yield false;
        yield !(plain
            && focus == canvas
            && TypingGuard.isTyping((com.cburch.logisim.gui.main.Canvas) canvas));
      }
      case INFO -> false;
    };
  }
}
