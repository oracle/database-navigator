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

import com.dbn.common.ui.misc.DBNButton;
import com.dbn.common.ui.util.Borders;
import com.dbn.data.editor.ui.TextFieldPopupProvider;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.type.DBDataType;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.options.DataEditorPopupSettings;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DatasetTableCellEditorWithPopup extends DatasetTableCellEditor {
    public DatasetTableCellEditorWithPopup(DatasetEditorTable table) {
        super(table, new CustomTextFieldWithPopup(table));
    }

    @Override
    @NotNull
    public TextFieldWithPopup<?> getEditorComponent() {
        return (TextFieldWithPopup) super.getEditorComponent();
    }

    @Override
    public void prepareEditor(@NotNull final DatasetEditorModelCell cell) {
        getEditorComponent().setUserValueHolder(cell);
        super.prepareEditor(cell);

        // show automatic popup
        TextFieldPopupProvider popupProvider = getEditorComponent().getAutoPopupProvider();
        if (popupProvider != null && showAutoPopup()) {
            Thread popupThread = new Thread(() -> {
                try {
                    Thread.sleep(settings.getPopupSettings().getDelay());
                } catch (InterruptedException e) {
                    conditionallyLog(e);
                }

                if (cell.isEditing()) {
                    popupProvider.showPopup();
                }
            });
            popupThread.start();
        }
    }

    @Override
    public void setEditable(boolean editable) {
        getEditorComponent().setEditable(editable);
    }


    private boolean showAutoPopup() {
        DataEditorPopupSettings settings = this.settings.getPopupSettings();
        DatasetEditorModelCell cell = getCell();
        if (cell != null) {
            DBDataType dataType = cell.getColumnInfo().getDataType();
            long dataLength = dataType.getLength();
            if (!isEditable()) {
                return true;
            } else  if (settings.isActive() && (settings.getDataLengthThreshold() < dataLength || dataLength == 0)) {
                if (settings.isActiveIfEmpty() || getTextField().getText().length() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void fireEditingCanceled() {
        getEditorComponent().hideActivePopup();
        super.fireEditingCanceled();
    }

    @Override
    protected void fireEditingStopped() {
        getEditorComponent().hideActivePopup();
        super.fireEditingStopped();
    }

    /********************************************************
     *                      KeyListener                     *
     ********************************************************/
    @Override
    public void keyPressed(KeyEvent keyEvent) {
        if (!keyEvent.isConsumed()) {
            TextFieldPopupProvider popupProviderForm = getEditorComponent().getActivePopupProvider();
            if (popupProviderForm != null) {
                popupProviderForm.handleKeyPressedEvent(keyEvent);

            } else {
                popupProviderForm = getEditorComponent().getPopupProvider(keyEvent);
                if (popupProviderForm != null) {
                    getEditorComponent().hideActivePopup();
                    popupProviderForm.showPopup();
                } else {
                    super.keyPressed(keyEvent);
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        TextFieldPopupProvider popupProviderForm = getEditorComponent().getActivePopupProvider();
        if (popupProviderForm != null) {
            popupProviderForm.handleKeyReleasedEvent(keyEvent);

        }
    }

    /********************************************************
     *                  TextFieldWithPopup                  *
     ********************************************************/

    private static class CustomTextFieldWithPopup extends TextFieldWithPopup<JTable> {
        private CustomTextFieldWithPopup(DatasetEditorTable table) {
            super(table.getProject(), table);
            setBackground(table.getBackground());
        }

        @Override
        public void customizeTextField(JTextField textField) {
            textField.setBorder(Borders.EMPTY_BORDER);
            textField.setMargin(JBUI.emptyInsets());
            JTable table = getTableComponent();
            textField.setPreferredSize(new Dimension(textField.getPreferredSize().width, table.getRowHeight()));
            //textField.setBorder(new CompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(new Insets(1, 1, 1, 1))));
        }

        @Override
        public JComponent createButton(Icon icon, String name) {
            DBNButton button = new DBNButton(icon, name);
            JTable table = getTableComponent();
            if (table == null) return button;

            button.setBorder(Borders.insetBorder(1));
            button.setOpaque(false);
            int rowHeight = table.getRowHeight();
            button.setPreferredSize(new Dimension(Math.max(20, rowHeight), rowHeight - 2));
            table.addPropertyChangeListener(e -> {
                Object newProperty = e.getNewValue();
                if (newProperty instanceof Font) {
                    int rowHeight1 = table.getRowHeight();
                    button.setPreferredSize(new Dimension(Math.max(20, rowHeight1), table.getRowHeight() - 2));
                }
            });

            return button;
        }

        JTable getTableComponent() {
            return getParentComponent();
        }

        @Override
        public void setEditable(boolean editable) {
            super.setEditable(editable);
            setBackground(getTextField().getBackground());
        }
    }
}
