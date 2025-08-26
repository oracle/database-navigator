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

package com.dbn.data.editor.ui.array;

import com.dbn.common.data.Data;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.UserValueHolder;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTextField;
import java.util.List;

import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.nls.NlsResources.txt;

class ArrayEditorAcceptAction extends ArrayEditorAction {
    ArrayEditorAcceptAction() {
        super(txt("app.data.action.AcceptChanges"), null, Icons.TEXT_CELL_EDIT_ACCEPT);
        //setShortcutSet(Keyboard.createShortcutSet(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ArrayEditorPopupProviderForm form = getArrayEditorForm(e);
        if (form == null) return;

        ArrayEditorList list = form.getEditorList();
        list.stopCellEditing();
        TextFieldWithPopup<?> editorComponent = form.getEditorComponent();
        UserValueHolder<?> userValueHolder = editorComponent.getUserValueHolder();

        Class<?> clazz = userValueHolder.getDataClass();

        List<String> stringData = list.getModel().getData();
        List<?> data = clazz == null ?
                Lists.convert(stringData, s -> Strings.isEmpty(s) ? null : s):
                Data.asTypeList(stringData, clazz);

        userValueHolder.updateUserValue(cast(data), false);

        JTextField textField = editorComponent.getTextField();
        String csvData = Data.listToCsv(data);
        textField.setText(csvData);
        form.hidePopup();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        ArrayEditorPopupProviderForm form = getArrayEditorForm(e);
        e.getPresentation().setEnabled(form != null && form.isChanged());
    }
}
