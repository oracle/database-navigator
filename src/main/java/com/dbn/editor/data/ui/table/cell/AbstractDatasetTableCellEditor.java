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

package com.dbn.editor.data.ui.table.cell;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.locale.Formatter;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Strings;
import com.dbn.data.editor.ui.DataEditorComponent;
import com.dbn.data.type.DBDataType;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.model.DatasetEditorModelCellValueListener;
import com.dbn.editor.data.options.DataEditorGeneralSettings;
import com.dbn.editor.data.options.DataEditorSettings;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.text.ParseException;
import java.util.EventObject;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public abstract class AbstractDatasetTableCellEditor extends AbstractCellEditor implements TableCellEditor, StatefulDisposable {
    private final WeakRef<DataEditorComponent> editorComponent;
    private final WeakRef<DatasetEditorTable> table;
    private WeakRef<DatasetEditorModelCell> cell;
    private int clickCountToStart = 1;
    protected DataEditorSettings settings;

    AbstractDatasetTableCellEditor(@NotNull DatasetEditorTable table, DataEditorComponent editorComponent) {
        this.table = WeakRef.of(table);
        this.editorComponent = WeakRef.of(editorComponent);

        Project project = table.getProject();
        this.settings = DataEditorSettings.getInstance(project);

        this.clickCountToStart = 2;
        editorComponent.getTextField().addActionListener(new EditorDelegate());
        ProjectEvents.subscribe(project, this, DatasetEditorModelCellValueListener.TOPIC, cellValueListener());

        table.addPropertyChangeListener(evt -> {
            Object newValue = evt.getNewValue();
            if (newValue instanceof Font) {
                Font newFont = (Font) newValue;
                getEditorComponent().setFont(newFont);
            }
        });

        Disposer.register(table, this);
        Disposer.register(this, editorComponent);
    }

    @NotNull
    private DatasetEditorModelCellValueListener cellValueListener() {
        return cell -> {
            if (cell == getCell()) {
                Dispatch.run(() -> setCellValueToEditor());
            }
        };
    }

    public DatasetEditorTable getTable() {
        return table.ensure();
    }

    @NotNull
    public Project getProject() {
        return getTable().getProject();
    }


    @NotNull
    public DataEditorComponent getEditorComponent() {
        return editorComponent.ensure();
    }

    public void setCell(@Nullable DatasetEditorModelCell cell) {
        this.cell = WeakRef.of(cell);
    }

    @Nullable
    public DatasetEditorModelCell getCell() {
        return WeakRef.get(cell);
    }

    public JTextField getTextField() {
        return getEditorComponent().getTextField();
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        if (e instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent) e;
            return mouseEvent.getClickCount() >= clickCountToStart;
        }
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject event) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    @Override
    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        cell = WeakRef.of((DatasetEditorModelCell) value);
        setCellValueToEditor();
        return (Component) getEditorComponent();
    }

    private void setCellValueToEditor() {
        DataEditorComponent editorComponent = getEditorComponent();
        DatasetEditorModelCell cell = getCell();

        if (cell != null) {
            Object userValue = cell.getUserValue();
            if (userValue instanceof String) {
                editorComponent.setText((String) userValue);
            } else {
                Formatter formatter = cell.getFormatter();
                String stringValue = formatter.formatObject(userValue);
                editorComponent.setText(stringValue);
            }
        } else {
            editorComponent.setText("");
        }
    }

    @Override
    public Object getCellEditorValue() {
        DatasetEditorModelCell cell = getCell();
        if (cell != null) {
            DBDataType dataType = cell.getColumnInfo().getDataType();
            Class clazz = dataType.getTypeClass();
            try {
                String textValue = getEditorComponent().getText();


                boolean trim = true;
                if (clazz == String.class) {
                    DataEditorGeneralSettings generalSettings = settings.getGeneralSettings();
                    boolean isEmpty = Strings.isEmptyOrSpaces(textValue);
                    trim = (isEmpty && generalSettings.getConvertEmptyStringsToNull().value()) ||
                            (!isEmpty && generalSettings.getTrimWhitespaces().value());
                }

                if (trim) textValue = textValue.trim();

                if (!textValue.isEmpty()) {
                    Formatter formatter = cell.getFormatter();
                    Object value = formatter.parseObject(clazz, textValue);
                    return dataType.getNativeType().getDefinition().convert(value);
                } else {
                    return null;
                }
            } catch (ParseException e) {
                conditionallyLog(e);
                throw new IllegalArgumentException("Can not convert " + getEditorComponent().getText() + " to " + dataType.getName());
            }
        }
        return null;
    }

    public String getCellEditorTextValue() {
        return getEditorComponent().getText().trim();
    }

    public boolean isEnabled() {
        return getEditorComponent().isEnabled();
    }

    public void setEnabled(boolean enabled) {
        getEditorComponent().setEnabled(enabled);
    }

    /********************************************************
     *                    EditorDelegate                    *
     ********************************************************/
    protected class EditorDelegate implements ActionListener, ItemListener, Serializable {

        @Override
        public void actionPerformed(ActionEvent e) {
            AbstractDatasetTableCellEditor.this.stopCellEditing();
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            AbstractDatasetTableCellEditor.this.stopCellEditing();
        }
    }


    /********************************************************
     *                    Disposable                        *
     ********************************************************/

    @Getter
    @Setter
    private boolean disposed;

    @Override
    public void disposeInner() {
        nullify();
    }
}
