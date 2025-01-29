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

package com.dbn.data.editor.text.ui;

import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Dialogs;
import com.dbn.data.editor.ui.DataEditorComponent;
import com.dbn.data.editor.ui.UserValueHolder;
import com.dbn.data.type.DBDataType;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.common.util.Strings.cachedLowerCase;
import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.common.util.Strings.toUpperCase;

public class TextEditorDialog extends DBNDialog<TextEditorForm> {
    private final DataEditorComponent textEditorAdapter;
    private TextEditorDialog(Project project, DataEditorComponent textEditorAdapter){
        super(project, getTitle(textEditorAdapter), true);
        this.textEditorAdapter = textEditorAdapter;
        renameAction(getCancelAction(), "Close");
        getOKAction().setEnabled(false);
        setModal(true);
        init();
    }

    @NotNull
    @Override
    protected TextEditorForm createForm() {
        UserValueHolder userValueHolder = textEditorAdapter.getUserValueHolder();
        return new TextEditorForm(this, documentListener, userValueHolder, textEditorAdapter);
    }

    @NotNull
    private static String getTitle(DataEditorComponent textEditorAdapter) {
        UserValueHolder userValueHolder = textEditorAdapter.getUserValueHolder();
        DBDataType dataType = userValueHolder.getDataType();
        String dataTypeName = dataType == null ? "OBJECT" : dataType.getName();
        DBObjectType objectType = userValueHolder.getObjectType();
        return "Edit " + cachedUpperCase(dataTypeName) + " content (" + cachedLowerCase(objectType.getName()) + " " + toUpperCase(userValueHolder.getName()) + ")";
    }

    public static void show(Project project, DataEditorComponent textEditorAdapter) {
        Dialogs.show(() -> new TextEditorDialog(project, textEditorAdapter));
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction(),
                getCancelAction(),
                getHelpAction()
        };
    }

    @Override
    protected void doOKAction() {
        String text = getForm().getText();
        Progress.modal(getProject(), null, false,
                txt("prc.dataEditor.title.UpdatingData"),
                txt("prc.dataEditor.text.UpdatingValue"),
                progress -> {
            UserValueHolder<String> userValueHolder = textEditorAdapter.getUserValueHolder();
            userValueHolder.updateUserValue(text, false);
            textEditorAdapter.afterUpdate();
        });
        super.doOKAction();
    }

    private final DocumentListener documentListener = new DocumentListener() {
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            renameAction(getCancelAction(), "Cancel");
            getOKAction().setEnabled(true);
        }
    };
}
