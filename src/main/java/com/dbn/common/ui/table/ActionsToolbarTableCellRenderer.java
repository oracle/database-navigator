/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.common.ui.table;

import com.dbn.common.color.Colors;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Mouse;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

public class ActionsToolbarTableCellRenderer implements TableCellRenderer {
    private final JPanel mainPanel = new JPanel();

    public ActionsToolbarTableCellRenderer() {
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.setBackground(Colors.getTableBackground());
    }

    public ActionsToolbarTableCellRenderer withAction(Icon icon, Runnable action) {
        JLabel actionLabel = new JLabel(icon);
        actionLabel.setBorder(Borders.lineBorder(Colors.getTableBackground(), 1));
        actionLabel.addMouseListener(Mouse.listener().onClick(e -> action.run()));
        mainPanel.add(actionLabel);
        return this;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return mainPanel;
    }
}
