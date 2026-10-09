/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.connection.context.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.context.DatabaseContextSelection;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

/**
 * Dialog for selecting the database context required by an operation.
 *
 * <p>The caller owns the context-specific validation and receives the selected
 * context through the Continue callback after the dialog closes.</p>
 */
public class DatabaseContextSelectionDialog extends DBNDialog<DatabaseContextSelectionForm> {
    private final DatabaseContextSelectorInput input;

    public DatabaseContextSelectionDialog(
            @NotNull Project project,
            @NotNull DatabaseContextSelectorInput input) {
        super(project, input.getTitle(), true);
        this.input = input;
        setDefaultSize(520, 300);
        init();
        updateDialogButtons();
    }

    @NotNull
    @Override
    protected DatabaseContextSelectionForm createForm() {
        DatabaseContextSelectionForm form = new DatabaseContextSelectionForm(getProject(), input);
        form.setContinueButtonVisible(false);
        form.setContentChangeCallback(this::updateDialogButtons);
        return form;
    }

    @Override
    public void updateDialogButtons() {
        if (isDisposed()) return;

        setOKActionEnabled(getForm().isSelectionValid());
    }

    @Override
    @NotNull
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt("msg.shared.button.Continue"));
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    protected void doOKAction() {
        DatabaseContextSelection selection = getForm().getSelection();
        if (selection == null || !getForm().isSelectionValid()) return;

        super.doOKAction();
        input.getContinueCallback().accept(selection);
    }
}
