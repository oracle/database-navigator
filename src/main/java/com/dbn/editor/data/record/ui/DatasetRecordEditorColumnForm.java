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

package com.dbn.editor.data.record.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.locale.Formatter;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.ComponentAligner;
import com.dbn.common.ui.util.Cursors;
import com.dbn.data.editor.ui.BasicDataEditorComponent;
import com.dbn.data.editor.ui.DataEditorComponent;
import com.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dbn.data.editor.ui.ListPopupValuesProviderBase;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.TextFieldWithTextEditor;
import com.dbn.data.editor.ui.UserValueHolder;
import com.dbn.data.grid.options.DataGridSettings;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.DBNativeDataType;
import com.dbn.data.type.DataTypeDefinition;
import com.dbn.data.type.GenericDataType;
import com.dbn.data.value.LargeObjectValue;
import com.dbn.editor.data.model.DatasetEditorColumnInfo;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.model.DatasetEditorModelRow;
import com.dbn.editor.data.options.DataEditorSettings;
import com.dbn.editor.data.options.DataEditorValueListPopupSettings;
import com.dbn.object.DBColumn;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.ParseException;
import java.util.List;

import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.editor.data.model.RecordStatus.DELETED;

public class DatasetRecordEditorColumnForm extends DBNFormBase implements ComponentAligner.Form {
    private JLabel columnLabel;
    private JPanel valueFieldPanel;
    private JLabel dataTypeLabel;
    private JPanel mainPanel;

    private DatasetEditorModelCell cell;
    private final DataEditorComponent editorComponent;

    public DatasetRecordEditorColumnForm(DatasetRecordEditorForm parentForm, DatasetEditorModelCell cell) {
        super(parentForm);
        DatasetEditorColumnInfo columnInfo = cell.getColumnInfo();
        DBColumn column = cell.getColumn();
        DBDataType dataType = column.getDataType();
        Project project = column.getProject();

        boolean editable = cell.getRow().getModel().isEditable();
        boolean auditColumn = columnInfo.isAuditColumn();

        columnLabel.setIcon(column.getIcon());
        columnLabel.setText(column.getName());
        columnLabel.setForeground(auditColumn ? UIUtil.getLabelDisabledForeground() : UIUtil.getLabelForeground());
        dataTypeLabel.setText(dataType.getQualifiedName());
        dataTypeLabel.setForeground(UIUtil.getInactiveTextColor());


        if (editable && auditColumn) {
            DataGridSettings dataGridSettings = DataGridSettings.getInstance(project);
            editable = dataGridSettings.getAuditColumnSettings().isAllowEditing();
        }

        DBNativeDataType nativeDataType = dataType.getNativeType();
        if (nativeDataType != null) {
            DataTypeDefinition dataTypeDefinition = nativeDataType.getDefinition();
            GenericDataType genericDataType = dataTypeDefinition.getGenericDataType();

            DataEditorSettings dataEditorSettings = DataEditorSettings.getInstance(project);

            if (genericDataType.is(GenericDataType.DATE_TIME, GenericDataType.LITERAL, GenericDataType.ARRAY)) {
                TextFieldWithPopup textFieldWithPopup = new TextFieldWithPopup(project);

                textFieldWithPopup.setPreferredSize(new Dimension(300, -1));
                JTextField valueTextField = textFieldWithPopup.getTextField();
                valueTextField.addKeyListener(keyAdapter);
                valueTextField.addFocusListener(focusListener);
                onTextChange(valueTextField, e -> getEditorComponent().setForeground(Colors.getTextFieldForeground()));

                if (editable) {
                    switch (genericDataType) {
                        case DATE_TIME: textFieldWithPopup.createCalendarPopup(false); break;
                        case ARRAY: textFieldWithPopup.createArrayEditorPopup(false); break;
                        case LITERAL: {
                            long dataLength = dataType.getLength();
                            DataEditorValueListPopupSettings valueListPopupSettings = dataEditorSettings.getValueListPopupSettings();

                            if (!column.isPrimaryKey() && !column.isUniqueKey() && dataLength <= valueListPopupSettings.getDataLengthThreshold()) {
                                ListPopupValuesProvider valuesProvider = new ListPopupValuesProviderBase("Possible Values List", false) {
                                    @Override
                                    public List<String> getValues() {
                                        return columnInfo.getPossibleValues();
                                    }
                                };
                                textFieldWithPopup.createValuesListPopup(valuesProvider, valueListPopupSettings.isShowPopupButton());
                            }

                            if (dataLength > 20 && !column.isPrimaryKey() && !column.isForeignKey()) {
                                textFieldWithPopup.createTextEditorPopup(false);
                            }
                            break;
                        }
                    }

                } else {
                    textFieldWithPopup.setEditable(false);
                }
                editorComponent = textFieldWithPopup;
            } else if (genericDataType.is(GenericDataType.BLOB, GenericDataType.CLOB)) {
                editorComponent = new TextFieldWithTextEditor(project);
            } else {
                editorComponent = new BasicDataEditorComponent();
            }
        } else {
            editorComponent = new BasicDataEditorComponent();
            editorComponent.setEnabled(false);
            editorComponent.setEditable(false);
        }

        valueFieldPanel.add((Component) editorComponent, BorderLayout.CENTER);
        editorComponent.getTextField().setCursor(Cursors.textCursor());
        setCell(cell);

        Disposer.register(this, editorComponent);
    }

    @NotNull
    public DatasetRecordEditorForm getParentForm() {
        return ensureParentComponent();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public String getColumnName() {
        return columnLabel.getText();
    }


    public void setCell(DatasetEditorModelCell cell) {
        if (this.cell != null) updateUserValue(false);
        this.cell = cell;

        DatasetEditorModelRow row = cell.getRow();
        boolean editable = row.isNot(DELETED) && row.getModel().isEditable();
        editorComponent.setEnabled(editable);
        editorComponent.setUserValueHolder(cell);

        Formatter formatter = cell.getFormatter();
        if (cell.getUserValue() instanceof String) {
            String userValue = (String) cell.getUserValue();
            if (userValue.indexOf('\n') > -1) {
                userValue = userValue.replace('\n', ' ');
                editorComponent.setEditable(false);
            } else {
                editorComponent.setEditable(editable);
            }
            editorComponent.setText(userValue);
        } else {
            Object userValue = cell.getUserValue();
            editable = editable && !(userValue instanceof LargeObjectValue);
            editorComponent.setEditable(editable);
            String presentableValue = formatter.formatObject(userValue);
            editorComponent.setText(presentableValue);
        }
        JTextField valueTextField = editorComponent.getTextField();
        valueTextField.setBackground(Colors.getTextFieldBackground());
    }

    public DatasetEditorModelCell getCell() {
        return cell;
    }

    @Override
    public Component[] getAlignableComponents() {
        return new Component[]{columnLabel, dataTypeLabel};
    }

    public JComponent getEditorComponent() {
        return editorComponent.getTextField();
    }


    public Object getEditorValue() throws ParseException {
        DBDataType dataType = cell.getColumnInfo().getDataType();
        Class clazz = dataType.getTypeClass();
        String textValue = editorComponent.getText().trim();
        if (textValue.length() > 0) {
            Object value = cell.getFormatter().parseObject(clazz, textValue);
            DBNativeDataType nativeDataType = dataType.getNativeType();
            return nativeDataType == null ? null : nativeDataType.getDefinition().convert(value);
        } else {
            return null;
        }
    }

    private void updateUserValue(boolean highlightError) {
        if (editorComponent == null) return;

        JTextField valueTextField = editorComponent.getTextField();
        if (!valueTextField.isEditable()) return;

        try {
            Object value = getEditorValue();
            UserValueHolder<Object> userValueHolder = editorComponent.getUserValueHolder();
            userValueHolder.updateUserValue(value, false);
            valueTextField.setForeground(Colors.getTextFieldForeground());
        } catch (ParseException e) {
            conditionallyLog(e);
            if (highlightError) {
                valueTextField.setForeground(JBColor.RED);
            }

            //DBDataType dataType = cell.getColumnInfo().getDataType();
            //MessageUtil.showErrorDialog("Can not convert " + valueTextField.getText() + " to " + dataType.getName());
        }
    }

    /*********************************************************
     *                     Listeners                         *
     *********************************************************/

    private final KeyListener keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.isConsumed()) return;

            DatasetRecordEditorForm parentComponent = ensureParentComponent();
            if (e.getKeyCode() == 38) {//UP
                parentComponent.focusPreviousColumnPanel(DatasetRecordEditorColumnForm.this);
                e.consume();
            } else if (e.getKeyCode() == 40) { // DOWN
                parentComponent.focusNextColumnPanel(DatasetRecordEditorColumnForm.this);
                e.consume();
            }
        }
    };


    private final FocusListener focusListener = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            if (e.getOppositeComponent() == null) return;

            JTextField valueTextField = editorComponent.getTextField();
            DataEditorSettings settings = cell.getRow().getModel().getSettings();
            if (settings.getGeneralSettings().getSelectContentOnCellEdit().value()) {
                valueTextField.selectAll();
            }

            Rectangle rectangle = new Rectangle(mainPanel.getLocation(), mainPanel.getSize());
            getParentForm().getColumnsPanel().scrollRectToVisible(rectangle);
        }

        @Override
        public void focusLost(FocusEvent e) {
            updateUserValue(true);
        }
    };
}
