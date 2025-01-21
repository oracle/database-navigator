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

import com.dbn.common.icon.Icons;
import com.dbn.data.editor.ui.UserValueHolder;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

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
        UserValueHolder userValueHolder = form.getEditorComponent().getUserValueHolder();
        userValueHolder.updateUserValue(list.getModel().getData(), false);

/*
        String text = editorTextArea.getText().trim();

        if (userValueHolder.getUserValue() instanceof String) {
            JTextField textField = getTextField();
            getEditorComponent().setEditable(text.indexOf('\n') == -1);

            textField.setText(text);
        }
*/
        form.hidePopup();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        ArrayEditorPopupProviderForm form = getArrayEditorForm(e);
        e.getPresentation().setEnabled(form != null && form.isChanged());
    }
}
