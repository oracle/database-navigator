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

import com.dbn.browser.options.DatabaseBrowserEditorSettings;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNEditableTable;
import com.dbn.common.ui.table.DBNEditableTableModel;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.table.Tables;
import com.dbn.common.ui.util.Accessibility;
import com.dbn.common.ui.util.Cursors;
import com.dbn.object.common.editor.DefaultEditorOption;
import com.dbn.object.common.editor.DefaultEditorType;
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
import java.util.List;

import static com.dbn.common.ui.util.Accessibility.setAccessibleDescription;
import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.nls.NlsResources.txt;

public class DatabaseBrowserEditorSettingsForm extends ConfigurationEditorForm<DatabaseBrowserEditorSettings> {
    private JPanel mainPanel;
    private JBScrollPane editorTypesScrollPanel;
    private JTable editorTypeTable;


    public DatabaseBrowserEditorSettingsForm(DatabaseBrowserEditorSettings settings) {
        super(settings);
        editorTypeTable = new EditorTypeTable(this, settings.getOptions());
        editorTypesScrollPanel.setViewportView(editorTypeTable);
        registerComponent(editorTypeTable);
    }

    @Override
    protected void initAccessibility() {
        Accessibility.setAccessibleName(editorTypeTable, txt("cfg.browser.aria.DefaultEditorTypes"));
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        EditorTypeTableModel model = (EditorTypeTableModel) editorTypeTable.getModel();
        getConfiguration().setOptions(model.options);
    }

    @Override
    public void resetFormChanges() {
        editorTypeTable.setModel(new EditorTypeTableModel(getConfiguration().getOptions()));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public static class EditorTypeTable extends DBNEditableTable<EditorTypeTableModel> {

        EditorTypeTable(DBNForm parent, List<DefaultEditorOption> options) {
            super(parent, new EditorTypeTableModel(options), true);
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

            setDefaultRenderer(DefaultEditorType.class, new DBNColoredTableCellRenderer() {
                @Override
                protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
                    DefaultEditorType editorType = (DefaultEditorType) value;

                    append(editorType.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    setAccessibleDescription(this, txt("cfg.browser.aria.ChangeDefaultEditorTypeHint"));
                }
            });

            Tables.attachValueSelector(this, 1, txt("cfg.browser.title.EditorType"), DefaultEditorType.values());
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

    public static class EditorTypeTableModel extends DBNEditableTableModel {
        private final List<DefaultEditorOption> options;

        public EditorTypeTableModel(List<DefaultEditorOption> options) {
            this.options = new ArrayList<>(options);
        }

        @Override
        public int getRowCount() {
            return options.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> txt("cfg.browser.column.ObjectType");
                case 1 -> txt("cfg.browser.column.DefaultEditor");
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> DBObjectType.class;
                case 1 -> DefaultEditorType.class;
                default -> null;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return switch (columnIndex) {
                case 0 -> false;
                case 1 -> true;
                default -> false;
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DefaultEditorOption option = options.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> option.getObjectType();
                case 1 -> option.getEditorType();
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 1) {
                DefaultEditorType editorType = (DefaultEditorType) value;
                if (editorType != null) {
                    DefaultEditorOption option = options.remove(rowIndex);
                    options.add(rowIndex, new DefaultEditorOption(option.getObjectType(), editorType));
                }
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
