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

import com.cburch.logisim.fork.keymap.ForkAction;
import com.cburch.logisim.fork.keymap.ForkActions;
import com.cburch.logisim.fork.keymap.Keymap;
import com.cburch.logisim.fork.keymap.KeymapStore;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** The "?" overlay: every fork binding, grouped by category, plus where keymap.json lives. */
public final class CheatSheet extends JDialog {
  private static final long serialVersionUID = 1L;
  private static boolean showing;

  private final JPanel table = new JPanel(new GridBagLayout());
  private final JPanel problems = new JPanel(new GridBagLayout());

  private CheatSheet(Window owner) {
    super(owner, S.get("forkCheatSheetTitle"), Dialog.ModalityType.APPLICATION_MODAL);
    final var content = new JPanel(new BorderLayout(0, 8));
    content.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

    final var scroll = new JScrollPane(table);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    content.add(scroll, BorderLayout.CENTER);

    final var footer = new JPanel(new BorderLayout(0, 4));
    final var pathRow = new JPanel(new BorderLayout(6, 0));
    pathRow.add(new JLabel(S.get("forkCheatSheetKeymapFile")), BorderLayout.WEST);
    final var path = new JTextField(KeymapStore.keymapFile().toString());
    path.setEditable(false);
    pathRow.add(path, BorderLayout.CENTER);
    footer.add(pathRow, BorderLayout.NORTH);
    footer.add(problems, BorderLayout.CENTER);
    final var buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    final var reload = new JButton(S.get("forkCheatSheetReload"));
    reload.addActionListener(e -> fill(KeymapStore.reload()));
    final var close = new JButton(S.get("forkCheatSheetClose"));
    close.addActionListener(e -> dispose());
    buttons.add(reload);
    buttons.add(close);
    footer.add(buttons, BorderLayout.SOUTH);
    content.add(footer, BorderLayout.SOUTH);
    setContentPane(content);

    getRootPane().registerKeyboardAction(
        e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW);
    getRootPane().setDefaultButton(close);

    fill(KeymapStore.get());
    setSize(620, 640);
    setLocationRelativeTo(owner);
  }

  private void fill(Keymap keymap) {
    table.removeAll();
    final var gbc = new GridBagConstraints();
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.LINE_START;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    ForkAction.Category current = null;
    for (final var action : ForkActions.all()) {
      if (action.getCategory() != current) {
        current = action.getCategory();
        final var heading = new JLabel(current.getLabel());
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(gbc.gridy == 0 ? 0 : 10, 0, 4, 0);
        table.add(heading, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
      }
      final var keys = keysFor(action, keymap);
      final var label = new JLabel(action.getLabel());
      final var keyLabel = new JLabel(keys.isEmpty() ? "—" : String.join("   ", keys));
      keyLabel.setFont(keyLabel.getFont().deriveFont(Font.BOLD));
      if (!action.isEnabled()) {
        label.setEnabled(false);
        keyLabel.setEnabled(false);
        label.setToolTipText(S.get("forkCheatSheetDisabled"));
      }
      gbc.insets = new Insets(1, 8, 1, 12);
      gbc.gridx = 0;
      gbc.weightx = 1;
      table.add(label, gbc);
      gbc.gridx = 1;
      gbc.weightx = 0;
      table.add(keyLabel, gbc);
      gbc.gridy++;
    }
    gbc.gridx = 0;
    gbc.weighty = 1;
    table.add(new JPanel(), gbc);

    problems.removeAll();
    final var pgbc = new GridBagConstraints();
    pgbc.gridx = 0;
    pgbc.gridy = 0;
    pgbc.anchor = GridBagConstraints.LINE_START;
    for (final var problem : keymap.getProblems()) {
      problems.add(new JLabel("⚠ " + problem), pgbc);
      pgbc.gridy++;
    }
    table.revalidate();
    table.repaint();
    problems.revalidate();
  }

  private static List<String> keysFor(ForkAction action, Keymap keymap) {
    final var keys = new ArrayList<String>();
    if (action.isRemappable()) {
      for (final var binding : keymap.getBindings(action)) {
        final var text = binding.display();
        if (!keys.contains(text)) keys.add(text);
      }
    } else {
      keys.addAll(action.getDefaultBindings());
    }
    return keys;
  }

  /** Shows the cheat sheet over {@code owner}. */
  public static void show(Window owner) {
    SwingUtilities.invokeLater(
        () -> {
          if (showing) return;
          showing = true;
          try {
            new CheatSheet(owner).setVisible(true);
          } finally {
            showing = false;
          }
        });
  }
}
