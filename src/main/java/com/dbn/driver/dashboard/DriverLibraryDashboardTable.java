/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.driver.dashboard;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.util.Tooltips;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.nls.NlsResources.txt;

class DriverLibraryDashboardTable extends DBNTable<DriverLibraryDashboardTableModel> {
    DriverLibraryDashboardTable(@NotNull DBNComponent parent, DriverLibraryDashboardTableModel model) {
        super(parent, model, true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setDefaultRenderer(DriverLibraryDashboardTableModel.Row.class, new CellRenderer());
        getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());
        setFixedColumnWidth(0, 105);
        setFixedColumnWidth(2, 125);
        initTableSorter();
        setAccessibleName(this, txt("app.driver.aria.LibraryDashboard"));
    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        super.setModel(dataModel);
        initTableSorter();
    }

    private class CellRenderer extends DBNColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int rowIndex, int columnIndex) {
            DriverLibraryDashboardTableModel.Row row = (DriverLibraryDashboardTableModel.Row) value;
            String presentableValue = getModel().getPresentableValue(row, columnIndex);
            append(presentableValue == null ? "" : presentableValue, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }

    private class StatusCellRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new BorderLayout(5, 0));
        private final AsyncProcessIcon processIcon = new AsyncProcessIcon("Downloading driver library");
        private final JLabel label = new JLabel();

        private StatusCellRenderer() {
            panel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            panel.add(processIcon, BorderLayout.WEST);
            panel.add(label, BorderLayout.CENTER);
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable sourceTable, Object value, boolean selected, boolean focused, int rowIndex, int columnIndex) {
            DriverLibraryDashboardTable dashboardTable = (DriverLibraryDashboardTable) sourceTable;
            DriverLibraryDashboardTableModel model = dashboardTable.getModel();
            DriverLibraryDashboardTableModel.Row row = (DriverLibraryDashboardTableModel.Row) value;
            if (row == null) return panel;

            processIcon.setVisible(row.status() == DriverLibraryDashboardStatus.DOWNLOADING);
            label.setText(model.getPresentableValue(row, columnIndex));
            Tooltips.setToolTipText(label, row.errorMessage());
            panel.setBackground(selected ? dashboardTable.getSelectionBackground() : dashboardTable.getBackground());
            label.setForeground(selected ? dashboardTable.getSelectionForeground() : dashboardTable.getForeground());
            return panel;
        }
    }
}
