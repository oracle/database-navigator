/*
 * Copyright 2025 Oracle and/or its affiliates
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

import com.dbn.common.ui.util.Accessibility;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Point;

/**
 * An abstract base class for creating accessible table components within the DBN framework.
 * This class extends {@link JTable} and provides enhanced accessibility features using ARIA standards.
 * It serves as a foundation for creating tables that support accessibility for rows, columns, and cells.
 *
 * @param <T> the type of {@link DBNTableModel} used by the table.
 */
abstract class DBNTableAriaBase<T extends DBNTableModel> extends JTable {
    public DBNTableAriaBase(TableModel dm) {
        super(dm);
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleTable();
        }
        return accessibleContext;
    }

    protected class AccessibleTable extends AccessibleJTable {

        @Override
        public Accessible getAccessibleAt(int r, int c) {
            if (r >= 0 && c < 0) c = 0;
            if (r < 0 && c >= 0) r = 0;
            return super.getAccessibleAt(r, c);
        }

        @Override
        public Accessible getAccessibleChild(int i) {
            if (i < 0 || i >= getAccessibleChildrenCount()) return null;

            int column = getAccessibleColumnAtIndex(i);
            int row = getAccessibleRowAtIndex(i);

            String accessibleDescription = getColumnName(column) + ", row " + (row + 1) + ", column " + (column + 1);

            AccessibleTableCell cell = new AccessibleTableCell(DBNTableAriaBase.this, row, column, getAccessibleIndexAt(row, column));
            Accessibility.setAccessibleDescription(cell, accessibleDescription);
            return cell;
        }

        @Override
        public Accessible getAccessibleAt(Point p) {
            int column = columnAtPoint(p);
            int row = rowAtPoint(p);

            if (column == -1) return null;
            if (row == -1) return null;

            int index = getAccessibleIndexAt(row, column);
            return getAccessibleChild(index);
        }

        protected class AccessibleTableCell extends AccessibleJTableCell {
            private final int row;
            private final int column;

            public AccessibleTableCell(JTable table, int row, int columns, int index) {
                super(table, row, columns, index);
                this.row = row;
                this.column = columns;
            }

            @Override
            protected Component getCurrentComponent() {
                return DBNTableAriaBase.this
                        .getCellRenderer(row, column)
                        .getTableCellRendererComponent(DBNTableAriaBase.this, getValueAt(row, column), false, false, row, column);
            }

            @Override
            protected AccessibleContext getCurrentAccessibleContext() {
                Component c = getCurrentComponent();
                if (c instanceof Accessible) {
                    return c.getAccessibleContext();
                }
                // Note: don't call "super" as 1) we know for sure the cell is not accessible
                // and 2) the super implementation is incorrect anyway
                return null;
            }
        }
    }
}
