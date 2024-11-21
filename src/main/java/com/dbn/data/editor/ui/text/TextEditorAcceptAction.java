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

package com.dbn.data.editor.ui.text;

import com.dbn.common.icon.Icons;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.UserValueHolder;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTextField;

class TextEditorAcceptAction extends TextEditorAction {
    public TextEditorAcceptAction() {
        super("Accept Changes", null, Icons.TEXT_CELL_EDIT_ACCEPT);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TextEditorPopupProviderForm form = getTextEditorForm(e);
        if (form == null) return;

        String text = form.getText().trim();
        TextFieldWithPopup<?> editorComponent = form.getEditorComponent();
        UserValueHolder userValueHolder = editorComponent.getUserValueHolder();
        userValueHolder.updateUserValue(text, false);

        if (userValueHolder.getUserValue() instanceof String) {
            JTextField textField = form.getTextField();
            editorComponent.setEditable(text.indexOf('\n') == -1);

            textField.setText(text);
        }
        form.hidePopup();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TextEditorPopupProviderForm form = getTextEditorForm(e);
        e.getPresentation().setEnabled(form != null && form.isChanged());
    }
}
