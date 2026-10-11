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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PathBarTest {
  @TempDir Path tmp;

  private File base() {
    return tmp.resolve("base").toFile();
  }

  private String home() {
    return tmp.resolve("home").toString();
  }

  @Test
  public void blankTextIsNothing() {
    assertNull(PathBar.resolve("   ", base(), home()));
    assertNull(PathBar.resolve("\"\"", base(), home()));
  }

  @Test
  public void absolutePathsAreKept() {
    final var abs = tmp.resolve("x").resolve("y.circ").toString();
    assertEquals(new File(abs), PathBar.resolve(abs, base(), home()));
  }

  @Test
  public void relativePathsAreRelativeToTheCurrentFolder() {
    assertEquals(new File(base(), "sub"), PathBar.resolve("sub", base(), home()));
    assertEquals(tmp.toFile(), PathBar.resolve("..", base(), home()));
    assertEquals(tmp.toFile(), PathBar.resolve("../..", new File(base(), "a"), home()));
  }

  @Test
  public void tildeIsTheHomeFolder() {
    assertEquals(new File(home()), PathBar.resolve("~", base(), home()));
    assertEquals(new File(home(), "circuits"), PathBar.resolve("~/circuits", base(), home()));
  }

  @Test
  public void quotesAndSpacesFromCopyAsPathAreIgnored() {
    final var abs = tmp.resolve("my folder").toString();
    assertEquals(new File(abs), PathBar.resolve("  \"" + abs + "\"  ", base(), home()));
  }
}
