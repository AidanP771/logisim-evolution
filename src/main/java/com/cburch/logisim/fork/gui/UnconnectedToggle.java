/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.gui;

import static com.cburch.logisim.fork.Strings.S;

import com.cburch.logisim.fork.ForkColors;
import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Toolbar toggle for the red unconnected-pin markers ({@link ForkPreferences#SHOW_UNCONNECTED}),
 * shown beside the grid toggle under the canvas. It follows the flag, so the {@code U} key, the
 * palette and the preferences tab all keep it in sync.
 */
public final class UnconnectedToggle extends JComponent {
  private static final long serialVersionUID = 1L;

  private final Component canvas;
  private final transient PropertyChangeListener listener =
      e -> SwingUtilities.invokeLater(this::repaint);

  /** {@code canvas} is repainted when the toggle is clicked, so the markers appear at once. */
  public UnconnectedToggle(Component canvas) {
    this.canvas = canvas;
    final var size = AppPreferences.getScaled(AppPreferences.IconSize);
    setPreferredSize(new Dimension(size + AppPreferences.getScaled(4), size));
    setToolTipText(S.get("forkShowUnconnectedTip"));
    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            final var flag = ForkPreferences.SHOW_UNCONNECTED;
            flag.setEnabled(!flag.isEnabled());
            if (canvas != null) canvas.repaint();
          }
        });
  }

  @Override
  public void addNotify() {
    super.addNotify();
    ForkPreferences.addListener(ForkPreferences.SHOW_UNCONNECTED, listener);
  }

  @Override
  public void removeNotify() {
    ForkPreferences.removeListener(ForkPreferences.SHOW_UNCONNECTED, listener);
    super.removeNotify();
  }

  @Override
  protected void paintComponent(Graphics g) {
    final var g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final var on = ForkPreferences.SHOW_UNCONNECTED.isEnabled();
      final var d = Math.min(getWidth(), getHeight()) - AppPreferences.getScaled(6);
      final var x = (getWidth() - d) / 2;
      final var y = (getHeight() - d) / 2;
      g2.setStroke(new BasicStroke(AppPreferences.getScaled(2)));
      // On: the red ring the canvas draws. Off: a muted ring with a slash through it.
      final var muted = UIManager.getColor("Label.disabledForeground");
      g2.setColor(on ? ForkColors.unconnected() : muted != null ? muted : Color.GRAY);
      g2.drawOval(x, y, d, d);
      if (!on) g2.drawLine(x, y + d, x + d, y);
    } finally {
      g2.dispose();
    }
  }
}
