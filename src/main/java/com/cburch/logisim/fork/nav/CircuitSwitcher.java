/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.nav;

import static com.cburch.logisim.fork.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.proj.Project;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Ctrl+Tab / Ctrl+Shift+Tab circuit switcher, like an editor's tab switcher.
 *
 * <p>While Ctrl is held a small popup lists the project's circuits in most-recently-used order;
 * each Tab moves the highlight, releasing Ctrl opens the highlighted circuit and Esc cancels.
 *
 * <p>This deliberately uses the physical Ctrl key on every platform: on macOS Cmd+Tab belongs to
 * the system app switcher (an accepted exception to "use the menu modifier"). Swing would
 * otherwise use Ctrl+Tab for focus traversal, so the switcher watches key events through a
 * {@link KeyEventDispatcher}, which runs before focus handling.
 */
public final class CircuitSwitcher implements KeyEventDispatcher {
  private static CircuitSwitcher instance;

  private Popup popup;

  private CircuitSwitcher() {}

  /** Starts listening. Safe to call repeatedly; only the first call acts. */
  public static synchronized void install() {
    if (instance != null) return;
    instance = new CircuitSwitcher();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(instance);
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent e) {
    final var isCtrlTab =
        e.getKeyCode() == KeyEvent.VK_TAB
            || (e.getID() == KeyEvent.KEY_TYPED && e.getKeyChar() == '\t');
    final var ctrlOnly =
        (e.getModifiersEx()
                & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK))
            == InputEvent.CTRL_DOWN_MASK;

    if (popup != null) {
      switch (e.getID()) {
        case KeyEvent.KEY_PRESSED -> {
          if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            close(false);
            return true;
          }
          if (e.getKeyCode() == KeyEvent.VK_TAB) {
            popup.move(e.isShiftDown() ? -1 : 1);
            return true;
          }
        }
        case KeyEvent.KEY_RELEASED -> {
          if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            close(true);
            return false;
          }
        }
        default -> {
          // other events are swallowed below
        }
      }
      // Swallow everything else while the switcher is up.
      return true;
    }

    if (!isCtrlTab || !ctrlOnly || !ForkPreferences.CIRCUIT_SWITCHER.isEnabled()) return false;
    final var window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    if (!(window instanceof Frame frame)) return false;
    if (e.getID() != KeyEvent.KEY_PRESSED) return true; // typed/released halves of Ctrl+Tab

    final var project = frame.getProject();
    final var recent = CircuitHistory.forProject(project).getRecent();
    final var all = project.getLogisimFile().getCircuits();
    final var ordered = SwitcherOrder.order(recent, all);
    if (ordered.size() < 2) return true;
    popup = new Popup(frame, project, ordered);
    popup.move(e.isShiftDown() ? -1 : 1);
    popup.showOver(frame);
    return true;
  }

  private void close(boolean commit) {
    final var p = popup;
    popup = null;
    if (p == null) return;
    p.dispose();
    if (commit) {
      final var target = p.selected();
      if (target != null && target != p.project.getCurrentCircuit()) {
        // After the key event has finished dispatching.
        SwingUtilities.invokeLater(() -> p.project.setCurrentCircuit(target));
      }
    }
  }

  /** The floating list. It never takes focus, so keys keep flowing through the dispatcher. */
  private final class Popup extends JWindow {
    private static final long serialVersionUID = 1L;
    private final transient Project project;
    private final JList<String> list;
    private final transient List<Circuit> circuits;

    Popup(Frame owner, Project project, List<Circuit> circuits) {
      super(owner);
      this.project = project;
      this.circuits = circuits;
      setFocusableWindowState(false);

      final var names = circuits.stream().map(Circuit::getName).toArray(String[]::new);
      list = new JList<>(names);
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      list.setVisibleRowCount(Math.min(names.length, 12));

      final var title = new JLabel(S.get("forkSwitcherTitle"));
      title.setFont(title.getFont().deriveFont(Font.BOLD));
      title.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 2));

      final var panel = new JPanel(new BorderLayout());
      var border = UIManager.getColor("controlShadow");
      panel.setBorder(
          BorderFactory.createCompoundBorder(
              border == null
                  ? BorderFactory.createEtchedBorder()
                  : BorderFactory.createLineBorder(border),
              BorderFactory.createEmptyBorder(8, 10, 8, 10)));
      panel.add(title, BorderLayout.NORTH);
      panel.add(list, BorderLayout.CENTER);
      setContentPane(panel);
      pack();

      // Losing the main window (e.g. Cmd+Tab away) cancels the switch.
      owner.addWindowListener(
          new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent e) {
              owner.removeWindowListener(this);
              if (popup == Popup.this) close(false);
            }
          });
    }

    void move(int delta) {
      final var size = circuits.size();
      final var current = Math.max(list.getSelectedIndex(), 0);
      list.setSelectedIndex(Math.floorMod(current + delta, size));
    }

    Circuit selected() {
      final var i = list.getSelectedIndex();
      return i < 0 ? null : circuits.get(i);
    }

    void showOver(Frame owner) {
      setLocationRelativeTo(owner);
      setVisible(true);
    }
  }
}
