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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** The "?" overlay: every fork binding, grouped by category, plus where keymap.json lives. */
public final class CheatSheet extends JDialog {
  private static final long serialVersionUID = 1L;
  private static boolean showing;

  private final JPanel table = new JPanel(new GridBagLayout());
  private final JPanel problems = new JPanel(new GridBagLayout());
  private final JTextField search = new JTextField();
  private Keymap keymap;

  private CheatSheet(Window owner) {
    super(owner, S.get("forkCheatSheetTitle"), Dialog.ModalityType.APPLICATION_MODAL);
    final var content = new JPanel(new BorderLayout(0, 8));
    content.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

    search.putClientProperty("JTextField.placeholderText", S.get("forkCheatSheetSearch"));
    search.putClientProperty("JTextField.showClearButton", true);
    search.getDocument().addDocumentListener(
        new DocumentListener() {
          @Override
          public void insertUpdate(DocumentEvent e) {
            fill();
          }

          @Override
          public void removeUpdate(DocumentEvent e) {
            fill();
          }

          @Override
          public void changedUpdate(DocumentEvent e) {
            fill();
          }
        });
    content.add(search, BorderLayout.NORTH);

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
    reload.addActionListener(e -> {
      keymap = KeymapStore.reload();
      fill();
    });
    final var close = new JButton(S.get("forkCheatSheetClose"));
    close.addActionListener(e -> dispose());
    buttons.add(reload);
    buttons.add(close);
    footer.add(buttons, BorderLayout.SOUTH);
    content.add(footer, BorderLayout.SOUTH);
    setContentPane(content);

    getRootPane().registerKeyboardAction(
        e -> {
          // Esc clears the search first, then closes.
          if (search.getText().isEmpty()) {
            dispose();
          } else {
            search.setText("");
          }
        },
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW);
    getRootPane().setDefaultButton(close);

    keymap = KeymapStore.get();
    fill();
    setSize(620, 640);
    setLocationRelativeTo(owner);
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowOpened(WindowEvent e) {
            search.requestFocusInWindow();
          }
        });
  }

  /**
   * Whether a row matches the search: every word of {@code query} must occur, ignoring case, in
   * the action label, its keys or its category. A blank query matches everything.
   */
  static boolean matches(String query, String label, List<String> keys, String category) {
    final var haystack =
        (label + " " + String.join(" ", keys) + " " + category).toLowerCase(Locale.ROOT);
    for (final var word : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
      if (!haystack.contains(word)) return false;
    }
    return true;
  }

  private void fill() {
    table.removeAll();
    final var query = search.getText();
    final var gbc = new GridBagConstraints();
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.LINE_START;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    ForkAction.Category current = null;
    // Grouped by category (registration order within each), whatever order features registered in.
    final var ordered = new ArrayList<>(ForkActions.all());
    ordered.sort(Comparator.comparing(ForkAction::getCategory));
    for (final var action : ordered) {
      final var keys = keysFor(action, keymap);
      if (!matches(query, action.getLabel(), keys, action.getCategory().getLabel())) continue;
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
    if (current == null) {
      gbc.insets = new Insets(0, 0, 0, 0);
      table.add(new JLabel(S.get("forkCheatSheetNoMatch")), gbc);
      gbc.gridy++;
    }
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
