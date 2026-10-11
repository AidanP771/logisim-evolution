/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import org.junit.jupiter.api.Test;

public class AppearanceTest {
  private static final String INTELLIJ = FlatIntelliJLaf.class.getName();
  private static final String DARCULA = FlatDarculaLaf.class.getName();

  @Test
  public void themesSwitchToTheirPartner() {
    assertEquals(DARCULA, Appearance.themeFor(INTELLIJ, true, false));
    assertEquals(INTELLIJ, Appearance.themeFor(DARCULA, false, false));
    assertEquals(FlatDarkLaf.class.getName(),
        Appearance.themeFor(FlatLightLaf.class.getName(), true, false));
    assertEquals(FlatMacLightLaf.class.getName(),
        Appearance.themeFor(FlatMacDarkLaf.class.getName(), false, true));
  }

  @Test
  public void aMatchingThemeIsKept() {
    assertEquals(DARCULA, Appearance.themeFor(DARCULA, true, true));
    assertEquals(INTELLIJ, Appearance.themeFor(INTELLIJ, false, true));
  }

  @Test
  public void otherThemesFallBackToThePlatformDefault() {
    final var metal = "javax.swing.plaf.metal.MetalLookAndFeel";
    assertEquals(DARCULA, Appearance.themeFor(metal, true, false));
    assertEquals(FlatMacDarkLaf.class.getName(), Appearance.themeFor(metal, true, true));
    assertEquals(metal, Appearance.themeFor(metal, false, false));
  }

  @Test
  public void parsesTheOsSettings() {
    assertTrue(Appearance.parseMac("Dark\n"));
    assertFalse(Appearance.parseMac("The domain/default pair of (kCFPreferencesAnyApplication, "
        + "AppleInterfaceStyle) does not exist"));
    assertTrue(Appearance.parseWindows("\r\nHKEY_CURRENT_USER\\...\\Personalize\r\n"
        + "    AppsUseLightTheme    REG_DWORD    0x0\r\n"));
    assertFalse(Appearance.parseWindows("    AppsUseLightTheme    REG_DWORD    0x1"));
    assertNull(Appearance.parseWindows("ERROR: nothing"));
    assertTrue(Appearance.parseLinux("'prefer-dark'\n"));
    assertFalse(Appearance.parseLinux("'prefer-light'"));
    assertNull(Appearance.parseLinux("'default'"));
  }
}
