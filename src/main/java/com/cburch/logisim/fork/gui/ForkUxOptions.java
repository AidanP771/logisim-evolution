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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** The "Fork UX" preferences tab: one checkbox per {@link ForkFlag}, grouped by category. */
public class ForkUxOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;

  private final JLabel intro = new JLabel();
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
    for (final var entry : categoryLabels.entrySet()) {
      entry.getValue().setText(entry.getKey().getLabel());
    }
    for (final var row : rows) {
      row.box().setText(row.flag().getLabel());
      row.description().setText(row.flag().getDescription());
    }
  }
}
