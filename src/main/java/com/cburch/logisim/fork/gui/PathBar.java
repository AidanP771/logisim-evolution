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

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Fork UX ({@code ui.pathBar}): an editable path field at the top of Logisim's file dialogs.
 *
 * <p>It shows the folder the chooser is in. Typing or pasting a path and pressing Enter goes to
 * that folder, or, for a file, selects it and approves the dialog. {@code ~} means the home
 * folder, relative paths are relative to the current folder, and surrounding quotes (as Windows
 * "Copy as path" adds) are ignored. Menu+L focuses the field, as in a browser.
 */
public final class PathBar extends JPanel {
  private static final long serialVersionUID = 1L;

  private final JFileChooser chooser;
  private final JTextField field = new JTextField();

  private PathBar(JFileChooser chooser) {
    super(new BorderLayout(6, 0));
    this.chooser = chooser;
    setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
    final var label = new JLabel(S.get("forkPathBarLabel"));
    label.setLabelFor(field);
    add(label, BorderLayout.WEST);
    add(field, BorderLayout.CENTER);
    field.setToolTipText(S.get("forkPathBarTip"));
    showDirectory();

    chooser.addPropertyChangeListener(
        JFileChooser.DIRECTORY_CHANGED_PROPERTY, e -> showDirectory());
    field.addActionListener(e -> go());
    field.addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent e) {
            SwingUtilities.invokeLater(field::selectAll);
          }
        });
    field.getDocument().addDocumentListener(
        new DocumentListener() {
          @Override
          public void insertUpdate(DocumentEvent e) {
            clearError();
          }

          @Override
          public void removeUpdate(DocumentEvent e) {
            clearError();
          }

          @Override
          public void changedUpdate(DocumentEvent e) {
            clearError();
          }
        });
  }

  /** Puts a path bar above {@code chooser} in {@code dialog} and binds Menu+L to it. */
  public static void install(JFileChooser chooser, JDialog dialog) {
    final var bar = new PathBar(chooser);
    final var old = dialog.getContentPane();
    final var content = new JPanel(new BorderLayout());
    content.add(bar, BorderLayout.NORTH);
    content.add(old, BorderLayout.CENTER);
    dialog.setContentPane(content);
    dialog.getRootPane().registerKeyboardAction(
        e -> bar.field.requestFocusInWindow(),
        KeyStroke.getKeyStroke(KeyEvent.VK_L, AppPreferences.hotkeyMenuMask),
        JComponent.WHEN_IN_FOCUSED_WINDOW);
    dialog.pack();
  }

  /**
   * The file {@code text} names: {@code ~} is {@code home}, a relative path is relative to {@code
   * base}, and surrounding quotes are dropped. Returns {@code null} for blank text.
   */
  static File resolve(String text, File base, String home) {
    var t = text.trim();
    if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
      t = t.substring(1, t.length() - 1).trim();
    }
    if (t.isEmpty()) return null;
    if (home != null && (t.equals("~") || t.startsWith("~/") || t.startsWith("~\\"))) {
      t = home + t.substring(1);
    }
    var file = new File(t);
    if (!file.isAbsolute() && base != null) file = new File(base, t);
    return file.toPath().normalize().toFile();
  }

  private void showDirectory() {
    final var dir = chooser.getCurrentDirectory();
    field.setText(dir == null ? "" : dir.getAbsolutePath());
    clearError();
  }

  private void go() {
    final var file =
        resolve(field.getText(), chooser.getCurrentDirectory(), System.getProperty("user.home"));
    if (file == null) return;
    if (file.isDirectory()) {
      chooser.setCurrentDirectory(file);
      if (chooser.getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY) {
        chooser.setSelectedFile(file);
      }
      showDirectory();
      return;
    }
    final var parent = file.getParentFile();
    final var saving = chooser.getDialogType() == JFileChooser.SAVE_DIALOG;
    if (file.isFile() || (saving && parent != null && parent.isDirectory())) {
      if (parent != null) chooser.setCurrentDirectory(parent);
      chooser.setSelectedFile(file);
      chooser.approveSelection();
      return;
    }
    field.putClientProperty("JComponent.outline", "error");
    field.setToolTipText(S.get("forkPathBarMissing", file.getPath()));
    Toolkit.getDefaultToolkit().beep();
  }

  private void clearError() {
    if (field.getClientProperty("JComponent.outline") == null) return;
    field.putClientProperty("JComponent.outline", null);
    field.setToolTipText(S.get("forkPathBarTip"));
  }
}
