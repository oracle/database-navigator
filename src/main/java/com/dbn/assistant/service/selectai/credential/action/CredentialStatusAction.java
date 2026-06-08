/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.service.selectai.credential.action;

import com.dbn.assistant.service.selectai.credential.ui.CredentialManagementForm;
import com.dbn.common.icon.Icons;
import com.dbn.object.DBCredential;
import com.dbn.object.management.ObjectManagementService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

/**
 * Toggle action for the credential management dialogs, allowing to quickly enable or disable a Credential
 * @author Dan Cioca (Oracle)
 */
public class CredentialStatusAction extends CredentialManagementAction {

    public CredentialStatusAction() {
        super(txt("app.assistant.action.AssistantEnableDisableCredential"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        DBCredential credential = getSelectedCredential(e);
        if (credential == null) return;

        ObjectManagementService managementService = ObjectManagementService.getInstance(project);
        if (credential.isEnabled())
            managementService.disableObject(credential, null); else
            managementService.enableObject(credential, null);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        DBCredential credential = getSelectedCredential(e);
        boolean enabled = credential != null && credential.isEnabled();

        Presentation presentation = e.getPresentation();
        presentation.setIcon(enabled ? Icons.ACTION_CHECK_BOX_SELECTED: Icons.ACTION_CHECK_BOX);
        presentation.setText(enabled ?
                txt("app.assistant.action.DisableCredential") :
                txt("app.assistant.action.EnableCredential"));
        presentation.setEnabled(isEnabled(e));
    }


    private static boolean isEnabled(@NotNull AnActionEvent e) {
        CredentialManagementForm managementForm = getManagementForm(e);
        if (managementForm == null) return false;
        if (managementForm.isLoading()) return false;
        if (managementForm.getSelectedCredential() == null) return false;

        return true;
    }
}
