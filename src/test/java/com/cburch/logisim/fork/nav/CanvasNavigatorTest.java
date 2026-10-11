/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.nav;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

public class CanvasNavigatorTest {
  private static MouseEvent press(int button, int modifiers) {
    return new MouseEvent(new JPanel(), MouseEvent.MOUSE_PRESSED, 0, modifiers, 10, 10, 1, false,
        button);
  }

  @Test
  public void middleButtonIsAMiddlePress() {
    assertTrue(CanvasNavigator.isMiddlePress(press(MouseEvent.BUTTON2, 0)));
  }

  @Test
  public void menuModifierPlusLeftButtonIsAMiddlePressOnEveryPlatform() {
    // Cmd on macOS, Ctrl on Windows and Linux (Alt when headless).
    assertTrue(CanvasNavigator.isMiddlePress(
        press(MouseEvent.BUTTON1, AppPreferences.hotkeyMenuMask)));
  }

  @Test
  public void plainOrExtraModifierLeftClicksAreNot() {
    assertFalse(CanvasNavigator.isMiddlePress(press(MouseEvent.BUTTON1, 0)));
    assertFalse(CanvasNavigator.isMiddlePress(
        press(MouseEvent.BUTTON1, AppPreferences.hotkeyMenuMask | InputEvent.SHIFT_DOWN_MASK)));
    assertFalse(CanvasNavigator.isMiddlePress(
        press(MouseEvent.BUTTON3, AppPreferences.hotkeyMenuMask)));
  }
}
