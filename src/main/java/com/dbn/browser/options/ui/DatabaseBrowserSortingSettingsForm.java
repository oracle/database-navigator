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

package com.dbn.browser.options.ui;

import com.dbn.browser.options.DatabaseBrowserSortingSettings;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNEditableTable;
import com.dbn.common.ui.table.DBNEditableTableModel;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.table.Tables;
import com.dbn.common.ui.util.Accessibility;
import com.dbn.common.ui.util.Cursors;
import com.dbn.object.common.sorting.DBObjectComparator;
import com.dbn.object.common.sorting.DBObjectComparators;
import com.dbn.object.common.sorting.SortingType;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.dbn.common.util.Strings.cachedUpperCase;

public class DatabaseBrowserSortingSettingsForm extends ConfigurationEditorForm<DatabaseBrowserSortingSettings> {
    private JPanel mainPanel;
    private JBScrollPane sortingTypesScrollPanel;
    private JTable sortingTypeTable;

    public DatabaseBrowserSortingSettingsForm(DatabaseBrowserSortingSettings settings) {
        super(settings);
        sortingTypeTable = new SortingTypeTable(this, settings.getComparators());
        sortingTypesScrollPanel.setViewportView(sortingTypeTable);
        registerComponent(sortingTypeTable);
    }

    @Override
    protected void initAccessibility() {
        Accessibility.setAccessibleName(sortingTypeTable, "Objects sorting");
    }


    @Override
    public void applyFormChanges() throws ConfigurationException {
        SortingTypeTableModel model = (SortingTypeTableModel) sortingTypeTable.getModel();
        getConfiguration().setComparators(model.comparators);
    }

    @Override
    public void resetFormChanges() {
        sortingTypeTable.setModel(new SortingTypeTableModel(getConfiguration().getComparators()));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public static class SortingTypeTable extends DBNEditableTable<SortingTypeTableModel> {

        public SortingTypeTable(DBNForm parent, Collection<DBObjectComparator> comparators) {
            super(parent, new SortingTypeTableModel(comparators), true);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            setDefaultRenderer(DBObjectType.class, new DBNColoredTableCellRenderer() {
                @Override
                protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
                    DBObjectType objectType = (DBObjectType) value;
                    if (objectType != null) {
                        setIcon(objectType.getIcon());
                        append(cachedUpperCase(objectType.getName()), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    } else {
                        append("");
                    }
                }
            });

            setDefaultRenderer(SortingType.class, new DBNColoredTableCellRenderer() {
                @Override
                protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
                    SortingType sortingType = (SortingType) value;
                    String name = sortingType == null ? "" : sortingType.getName();

                    append(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }
            });

            Tables.attachValueSelector(this, 1, "Sorting Type", SortingType.values());
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            Point mouseLocation = e.getPoint();
            int columnIndex = columnAtPoint(mouseLocation);
            if (columnIndex == 1) {
                setCursor(Cursors.handCursor());
            } else {
                setCursor(Cursors.defaultCursor());
            }
            super.processMouseMotionEvent(e);
        }
    }

    public static class SortingTypeTableModel extends DBNEditableTableModel {
        private final List<DBObjectComparator> comparators;

        public SortingTypeTableModel(Collection<DBObjectComparator> comparators) {
            this.comparators = new ArrayList<>(comparators);
        }

        @Override
        public int getRowCount() {
            return comparators.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0: return "Object Type";
                case 1: return "Sorting Type";
            }
            return null;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0: return DBObjectType.class;
                case 1: return SortingType.class;
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0: return false;
                case 1: return true;
            }
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DBObjectComparator comparator = comparators.get(rowIndex);
            switch (columnIndex) {
                case 0: return comparator.getObjectType();
                case 1: return comparator.getSortingType();
            }
            return null;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex != 1) return;
            if (value instanceof SortingType) {
                SortingType sortingType = (SortingType) value;
                DBObjectComparator comparator = comparators.remove(rowIndex);
                comparators.add(rowIndex, DBObjectComparators.predefined(comparator.getObjectType(), sortingType));
            }
        }

        @Override
        public void insertRow(int rowIndex) {
            throw new UnsupportedOperationException("Row mutation not supported");
        }

        @Override
        public void removeRow(int rowIndex) {
            throw new UnsupportedOperationException("Row mutation not supported");
        }
    }
}
