/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Parser;
import com.cburch.logisim.analyze.model.Var;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AnalyzerTexWriterTest {
  /** The header holds the time of writing; blank it out so two writes compare equal. */
  private static String withoutTimestamp(String tex) {
    return tex.replaceAll("(?m)^\\\\fancyhead\\[C\\].*$", "");
  }

  @Test
  public void toTexIsWhatDoSaveWrites(@TempDir Path dir) throws Exception {
    final var model = new AnalyzerModel();
    model.getInputs().add(Var.parse("a"));
    model.getInputs().add(Var.parse("b"));
    model.getOutputs().add(Var.parse("x"));
    model.getOutputExpressions().setExpression("x", Parser.parse("a b", model));

    final var file = dir.resolve("out.tex");
    AnalyzerTexWriter.doSave(file.toFile(), model);
    final var saved = Files.readString(file, StandardCharsets.UTF_8);
    final var copied = AnalyzerTexWriter.toTex(model);

    assertTrue(copied.contains("\\begin{document}"));
    assertTrue(copied.trim().endsWith("\\end{document}"));
    assertEquals(withoutTimestamp(saved), withoutTimestamp(copied));
  }
}
