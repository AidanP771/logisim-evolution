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

import com.cburch.logisim.fork.ForkFlag;
import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.gui.prefs.OptionsPanel;
import com.cburch.logisim.gui.prefs.PreferencesFrame;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** The "Fork UX" preferences tab: one checkbox per {@link ForkFlag}, grouped by category. */
public class ForkUxOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;

  private final JLabel intro = new JLabel();
  private final JLabel appearanceLabel = new JLabel();
  private final JComboBox<Appearance.Mode> appearance = new JComboBox<>(Appearance.Mode.values());
  private final Map<ForkFlag.Category, JLabel> categoryLabels = new EnumMap<>(ForkFlag.Category.class);
  private final List<FlagRow> rows = new ArrayList<>();

  private record FlagRow(ForkFlag flag, JCheckBox box, JLabel description) {}

  public ForkUxOptions(PreferencesFrame window) {
    super(window, new GridBagLayout());
    setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    final var gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.LINE_START;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.insets = new Insets(0, 0, 8, 0);
    add(intro, gbc);

    final var appearanceRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
    appearanceRow.add(appearanceLabel);
    appearanceRow.add(appearance);
    appearance.setSelectedItem(Appearance.getMode());
    appearance.setRenderer(
        new DefaultListCellRenderer() {
          @Override
          public Component getListCellRendererComponent(
              JList<?> list, Object value, int index, boolean selected, boolean focus) {
            final var text = value instanceof Appearance.Mode m ? modeLabel(m) : "";
            return super.getListCellRendererComponent(list, text, index, selected, focus);
          }
        });
    appearance.addActionListener(e -> {
      final var mode = (Appearance.Mode) appearance.getSelectedItem();
      if (mode != null && mode != Appearance.getMode()) {
        // After this event: switching theme rebuilds this combo's UI.
        SwingUtilities.invokeLater(() -> Appearance.setMode(mode));
      }
    });
    gbc.gridy++;
    gbc.insets = new Insets(0, 0, 4, 0);
    add(appearanceRow, gbc);

    ForkFlag.Category current = null;
    for (final var flag : ForkPreferences.getFlags()) {
      if (flag.getCategory() != current) {
        current = flag.getCategory();
        final var heading = new JLabel();
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        categoryLabels.put(current, heading);
        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 4, 0);
        add(heading, gbc);
      }
      final var box = new JCheckBox();
      box.setSelected(flag.isEnabled());
      box.addActionListener(e -> flag.setEnabled(box.isSelected()));
      ForkPreferences.addListener(flag, e -> box.setSelected(flag.isEnabled()));
      final var description = new JLabel();
      description.setFont(description.getFont().deriveFont(Font.PLAIN, description.getFont().getSize2D() - 1f));

      gbc.gridy++;
      gbc.insets = new Insets(0, 8, 0, 0);
      add(box, gbc);
      gbc.gridy++;
      gbc.insets = new Insets(0, 36, 4, 0);
      add(description, gbc);
      rows.add(new FlagRow(flag, box, description));
    }

    // Push everything to the top of the tab.
    gbc.gridy++;
    gbc.weighty = 1.0;
    add(new JPanel(), gbc);

    localeChanged();
  }

  private static String modeLabel(Appearance.Mode mode) {
    return switch (mode) {
      case LIGHT -> S.get("forkAppearanceLight");
      case DARK -> S.get("forkAppearanceDark");
      case SYSTEM -> S.get("forkAppearanceSystem");
      default -> S.get("forkAppearanceManual");
    };
  }

  /** The mode can change from the keymap or palette while this tab is open. */
  @Override
  public void addNotify() {
    super.addNotify();
    appearance.setSelectedItem(Appearance.getMode());
  }

  @Override
  public String getHelpText() {
    return S.get("forkUxHelp");
  }

  @Override
  public String getTitle() {
    return S.get("forkUxTitle");
  }

  @Override
  public void localeChanged() {
    intro.setText(S.get("forkUxIntro"));
    appearanceLabel.setText(S.get("forkAppearanceLabel"));
    appearance.repaint();
    for (final var entry : categoryLabels.entrySet()) {
      entry.getValue().setText(entry.getKey().getLabel());
    }
    for (final var row : rows) {
      row.box().setText(row.flag().getLabel());
      row.description().setText(row.flag().getDescription());
    }
  }
}
