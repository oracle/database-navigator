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

package com.dbn.data.record.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.locale.Formatter;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.ComponentAligner;
import com.dbn.common.ui.util.Cursors;
import com.dbn.data.record.DatasetRecord;
import com.dbn.data.type.DBDataType;
import com.dbn.object.DBColumn;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static com.dbn.common.ui.util.Accessibility.setAccessibleUnit;

public class RecordViewerColumnForm extends DBNFormBase implements ComponentAligner.Form {
    private JLabel columnLabel;
    private JPanel valueFieldPanel;
    private JLabel dataTypeLabel;
    private JPanel mainPanel;

    private final JTextField valueTextField;

    private final DBObjectRef<DBColumn> column;
    private final DatasetRecord record;

    RecordViewerColumnForm(RecordViewerForm parentForm, DatasetRecord record, DBColumn column) {
        super(parentForm);
        this.record = record;
        this.column = DBObjectRef.of(column);

        DBDataType dataType = column.getDataType();
        boolean auditColumn = column.isAudit();

        columnLabel.setIcon(column.getIcon());
        columnLabel.setText(column.getName());
        columnLabel.setForeground(auditColumn ? UIUtil.getLabelDisabledForeground() : UIUtil.getLabelForeground());

        dataTypeLabel.setText(dataType.getQualifiedName());
        dataTypeLabel.setForeground(UIUtil.getInactiveTextColor());

        valueTextField = new ColumnValueTextField(record, column);
        valueTextField.setPreferredSize(new JBDimension(200, -1));
        valueTextField.addKeyListener(keyAdapter);

        valueFieldPanel.add(valueTextField, BorderLayout.CENTER);
        valueTextField.setEditable(false);
        valueTextField.setCursor(Cursors.textCursor());
        valueTextField.setBackground(Colors.getTextFieldBackground());
        columnLabel.setLabelFor(valueTextField);

        setAccessibleUnit(valueTextField, dataTypeLabel.getText());
        updateColumnValue(column);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }


    public String getColumnName() {
        return columnLabel.getText();
    }

    private void updateColumnValue(DBColumn column) {
        Object value = record.getColumnValue(column);
        Formatter formatter = Formatter.getInstance(ensureProject());
        if (value instanceof String) {
            String userValue = (String) value;
            if (userValue.indexOf('\n') > -1) {
                userValue = userValue.replace('\n', ' ');
            } else {
            }
            valueTextField.setText(userValue);
        } else {
            String presentableValue = formatter.formatObject(value);
            valueTextField.setText(presentableValue);
        }
    }

    @NotNull
    public DBColumn getColumn() {
        return column.ensure();
    }

    @Override
    public Component[] getAlignableComponents() {
        return new Component[]{columnLabel, dataTypeLabel};
    }


/*    public Object getEditorValue() throws ParseException {
        DBDataType dataType = cell.getColumnInfo().getDataType();
        Class clazz = dataType.getTypeClass();
        String textValue = valueTextField.getText().trim();
        if (textValue.length() > 0) {
            Object value = getFormatter().parseObject(clazz, textValue);
            return dataType.getNativeDataType().getDataTypeDefinition().convert(value);
        } else {
            return null;
        }
    }*/

    public JTextField getViewComponent() {
        return valueTextField;
    }

    /*********************************************************
     *                     Listeners                         *
     *********************************************************/
    private final KeyListener keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (!e.isConsumed()) {
                RecordViewerForm parentForm = ensureParentComponent();
                if (e.getKeyCode() == 38) {//UP
                    parentForm.focusPreviousColumnPanel(RecordViewerColumnForm.this);
                    e.consume();
                } else if (e.getKeyCode() == 40) { // DOWN
                    parentForm.focusNextColumnPanel(RecordViewerColumnForm.this);
                    e.consume();
                }
            }
        }
    };

}
