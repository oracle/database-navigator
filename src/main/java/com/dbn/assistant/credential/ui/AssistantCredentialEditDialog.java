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

package com.dbn.assistant.credential.ui;

import com.dbn.assistant.credential.LocalCredential;
import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.Set;

@Getter
public class AssistantCredentialEditDialog extends DBNDialog<AssistantCredentialEditForm> {
    private final Set<String> usedTitles;
    private final LocalCredential credential;

    public AssistantCredentialEditDialog(Project project, LocalCredential credential, boolean create, Set<String> usedNames) {
        super(project, create ? "Create Credential" : "Update Credential", true);
        this.credential = credential;
        this.usedTitles = usedNames;
        renameAction(getOKAction(), create ? "Create" : "Update");
        setModal(true);
        init();
    }

    @NotNull
    @Override
    protected AssistantCredentialEditForm createForm() {
        return new AssistantCredentialEditForm(this, usedTitles);
    }


    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction(),
                getCancelAction()};
    }

    @Override
    public void doCancelAction() {
        close(0);
    }

    @Override
    protected void doOKAction() {
        // TODO create / update
        super.doOKAction();
    }
}

