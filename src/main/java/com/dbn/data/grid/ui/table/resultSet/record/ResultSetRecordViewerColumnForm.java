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

package com.dbn.data.grid.ui.table.resultSet.record;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.ComponentAligner;
import com.dbn.common.ui.util.Cursors;
import com.dbn.data.grid.options.DataGridSettings;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.resultSet.ResultSetDataModelCell;
import com.dbn.data.type.DBDataType;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.ParseException;

public class ResultSetRecordViewerColumnForm extends DBNFormBase implements ComponentAligner.Form {
    private JLabel columnLabel;
    private JPanel valueFieldPanel;
    private JLabel dataTypeLabel;
    private JPanel mainPanel;

    private final JTextField valueTextField;
    private ResultSetDataModelCell<?, ?> cell;

    public ResultSetRecordViewerColumnForm(ResultSetRecordViewerForm parent, ResultSetDataModelCell<?, ?> cell, boolean showDataType) {
        super(parent);
        ColumnInfo columnInfo = cell.getColumnInfo();

        DBDataType dataType = columnInfo.getDataType();
        boolean auditColumn = DataGridSettings.isAuditColumn(getProject(), columnInfo.getName());

        columnLabel.setIcon(Icons.DBO_COLUMN);
        columnLabel.setText(columnInfo.getName());
        columnLabel.setForeground(auditColumn ? UIUtil.getLabelDisabledForeground() : UIUtil.getLabelForeground());
        if (showDataType) {
            dataTypeLabel.setText(dataType.getQualifiedName());
            dataTypeLabel.setForeground(UIUtil.getInactiveTextColor());
        } else {
            dataTypeLabel.setVisible(false);
        }

        valueTextField = new JTextField();
        valueTextField.setPreferredSize(new Dimension(200, -1));
        valueTextField.addKeyListener(keyAdapter);

        valueFieldPanel.add(valueTextField, BorderLayout.CENTER);
        valueTextField.setEditable(false);
        valueTextField.setCursor(Cursors.textCursor());
        valueTextField.setBackground(Colors.getTextFieldBackground());
        setCell(cell);
    }

    public String getColumnName() {
        return columnLabel.getText();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public void setCell(ResultSetDataModelCell<?, ?> cell) {
        this.cell = cell;

        if (cell.getUserValue() instanceof String) {
            String userValue = (String) cell.getUserValue();
            if (userValue.indexOf('\n') > -1) {
                userValue = userValue.replace('\n', ' ');
            } else {
            }
            valueTextField.setText(userValue);
        } else {
            String presentableValue = cell.getFormatter().formatObject(cell.getUserValue());
            valueTextField.setText(presentableValue);
        }
    }

    public ResultSetDataModelCell<?, ?> getCell() {
        return cell;
    }

    @Override
    public Component[] getAlignableComponents() {
        return new Component[] {columnLabel, dataTypeLabel};
    }

    public Object getEditorValue() throws ParseException {
        DBDataType dataType = cell.getColumnInfo().getDataType();
        Class clazz = dataType.getTypeClass();
        String textValue = valueTextField.getText().trim();
        if (textValue.length() > 0) {
            Object value = cell.getFormatter().parseObject(clazz, textValue);
            return dataType.getNativeType().getDefinition().convert(value);
        } else {
            return null;
        }
    }

    public JTextField getViewComponent() {
        return valueTextField;
    }

    /*********************************************************
     *                     Listeners                         *
     *********************************************************/
    KeyListener keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (!e.isConsumed()) {
                ResultSetRecordViewerForm parentForm = ensureParentComponent();
                if (e.getKeyCode() == 38) {//UP
                    parentForm.focusPreviousColumnPanel(ResultSetRecordViewerColumnForm.this);
                    e.consume();
                } else if (e.getKeyCode() == 40) { // DOWN
                    parentForm.focusNextColumnPanel(ResultSetRecordViewerColumnForm.this);
                    e.consume();
                }
            }
        }
    };


    @Override
    public void disposeInner() {
        super.disposeInner();
        cell = null;
    }
}
