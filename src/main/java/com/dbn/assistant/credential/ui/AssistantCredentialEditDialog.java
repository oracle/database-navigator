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

import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

@Getter
public class AssistantCredentialEditDialog extends DBNDialog<AssistantCredentialEditForm> {
    private final AssistantCredential credential;
    private final AssistantCredentialEditRequest request;

    public AssistantCredentialEditDialog(Project project, AssistantCredentialEditRequest request) {
        super(project, request.isNewCredential() ? "Create Credential" : "Update Credential", true);
        this.request = request;
        this.credential = initCredential();

        renameAction(getOKAction(), request.isNewCredential() ? "Create" : "Update");
        setModal(true);
        setAutoSize(true);
        init();
    }

    private AssistantCredential initCredential() {
        AssistantCredential credential = request.getCredential();
        if (credential == null) {
            credential = new AssistantCredential();
            credential.setProviderId(request.getProviderId());
        }
        return credential;
    }

    @NotNull
    @Override
    protected AssistantCredentialEditForm createForm() {
        return new AssistantCredentialEditForm(this);
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
        AssistantCredentialEditForm form = getForm();
        form.applyFormChanges();
        request.acceptCredential(credential);
        super.doOKAction();
    }
}

