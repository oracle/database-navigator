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

import com.dbn.common.thread.Background;
import com.dbn.common.thread.Threads;
import com.dbn.data.editor.ui.DataEditorComponent;
import com.dbn.data.editor.ui.TextFieldPopupProvider;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.type.DBDataType;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.options.DataEditorPopupSettings;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;

import static com.dbn.common.ui.util.TextFields.isEmptyText;

public class DatasetTableCellEditorWithPopup extends DatasetTableCellEditor<DataEditorComponent> {
    public DatasetTableCellEditorWithPopup(DatasetEditorTable table) {
        super(table);
    }

    @Override
    protected DataEditorComponent createEditorComponent(DatasetEditorTable table) {
        return new TextFieldWithPopup(table.getProject(), table);
    }

    @Override
    @NotNull
    public TextFieldWithPopup getEditorComponent() {
        return (TextFieldWithPopup) super.getEditorComponent();
    }

    @Override
    public void prepareEditor(@NotNull final DatasetEditorModelCell cell) {
        getEditorComponent().setUserValueHolder(cell);
        super.prepareEditor(cell);

        // show automatic popup
        TextFieldPopupProvider popupProvider = getEditorComponent().getAutoPopupProvider();
        if (popupProvider != null && showAutoPopup()) {
            Background.run(() -> {
                int delay = settings.getPopupSettings().getDelay();
                Threads.sleep(delay);

                if (!cell.isEditing()) return;
                popupProvider.showPopup();
            });
        }
    }

    @Override
    public void setEditable(boolean editable) {
        getEditorComponent().setEditable(editable);
    }


    private boolean showAutoPopup() {
        DatasetEditorModelCell cell = getCell();
        if (cell == null) return false;

        DBDataType dataType = cell.getColumnInfo().getDataType();
        long dataLength = dataType.getLength();
        if (!isEditable()) return true;

        DataEditorPopupSettings settings = this.settings.getPopupSettings();
        if (!settings.isActive()) return false;
        if (settings.getDataLengthThreshold() >= dataLength && dataLength != 0) return false;

        if (settings.isActiveIfEmpty()) return true;
        if (!isEmptyText(getTextField())) return true;
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

}
