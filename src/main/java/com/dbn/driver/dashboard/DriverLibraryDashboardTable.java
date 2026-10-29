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
import com.dbn.driver.dashboard.DriverLibraryDashboardTableModel.DashboardDriverPackage;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import java.awt.Font;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.nls.NlsResources.txt;
import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.STYLE_ITALIC;

class DriverLibraryDashboardTable extends DBNTable<DriverLibraryDashboardTableModel> {
    DriverLibraryDashboardTable(@NotNull DBNComponent parent, DriverLibraryDashboardTableModel model) {
        super(parent, model, true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setDefaultRenderer(DashboardDriverPackage.class, new CellRenderer());
        setProportionalColumnWidths(15, 30, 15, 35, 5);
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
            DashboardDriverPackage driverPackage = (DashboardDriverPackage) value;
            String presentableValue = getModel().getPresentableValue(driverPackage, columnIndex);
            boolean inProgress = switch (driverPackage.status()) {
                case RESOLVING, DOWNLOADING -> true;
                default -> false;
            };
            setFont(inProgress ? table.getFont().deriveFont(Font.ITALIC) : table.getFont());
            SimpleTextAttributes attributes;
            if (inProgress) {
                attributes = driverPackage.supported() ? GRAY_ITALIC_ATTRIBUTES :
                        GRAYED_ATTRIBUTES.derive(STYLE_ITALIC, null, null, null);
            } else if (!driverPackage.supported()) {
                attributes = GRAYED_ATTRIBUTES;
            } else {
                attributes = REGULAR_ATTRIBUTES;
            }
            setIcon(null);
            append(presentableValue == null ? "" : presentableValue, attributes);
        }
    }
}
