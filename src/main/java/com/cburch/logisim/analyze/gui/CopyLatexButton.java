/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.fork.Strings.S;

import com.cburch.logisim.analyze.file.AnalyzerTexWriter;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * Fork UX: icon-only button beside "Export LaTeX" that copies the same document to the clipboard
 * ({@code analyzer.copyTex}). The icon turns into a tick for a moment to confirm the copy.
 */
public class CopyLatexButton extends JButton {
  private static final long serialVersionUID = 1L;
  private static final int CONFIRM_MILLIS = 1500;

  private final AnalyzerModel model;
  private final Timer restore;

  CopyLatexButton(AnalyzerModel model) {
    this.model = model;
    setIcon(new GlyphIcon(false));
    setToolTipText(S.get("forkCopyTexTip"));
    getAccessibleContext().setAccessibleName(S.get("forkCopyTexTip"));
    restore = new Timer(CONFIRM_MILLIS, e -> setIcon(new GlyphIcon(false)));
    restore.setRepeats(false);
    addActionListener(e -> copy());
  }

  private void copy() {
    final var tex = AnalyzerTexWriter.toTex(model);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(tex), null);
    setIcon(new GlyphIcon(true));
    restore.restart();
  }

  /** Two overlapping sheets (copy), or a tick once copied; drawn in the button's text colour. */
  private static final class GlyphIcon implements Icon {
    private final boolean done;

    GlyphIcon(boolean done) {
      this.done = done;
    }

    @Override
    public int getIconWidth() {
      return AppPreferences.getScaled(16);
    }

    @Override
    public int getIconHeight() {
      return AppPreferences.getScaled(16);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      final var g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        final var s = getIconWidth() / 16.0;
        g2.scale(s, s);
        final var disabled = UIManager.getColor("Button.disabledText");
        g2.setColor(c.isEnabled() || disabled == null ? c.getForeground() : disabled);
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (done) {
          g2.drawPolyline(new int[] {3, 7, 13}, new int[] {8, 12, 4}, 3);
        } else {
          g2.drawRoundRect(5, 1, 9, 11, 2, 2);
          g2.drawPolyline(new int[] {11, 11, 2, 2, 5}, new int[] {12, 15, 15, 4, 4}, 5);
        }
      } finally {
        g2.dispose();
      }
    }
  }
}
