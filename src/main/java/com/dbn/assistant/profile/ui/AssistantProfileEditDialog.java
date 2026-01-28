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

package com.dbn.assistant.profile.ui;

import com.dbn.assistant.profile.DeclaredAssistantProfile;
import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

@Getter
public class AssistantProfileEditDialog extends DBNDialog<AssistantProfileEditForm> {
    private final DeclaredAssistantProfile profile;
    private final AssistantProfileEditRequest request;

    public AssistantProfileEditDialog(Project project, AssistantProfileEditRequest request) {
        super(project, request.isNewProfile() ? "Create Profile" : "Update Profile", true);
        this.request = request;
        this.profile = initProfile();
        setModal(true);
        init();
    }

    private DeclaredAssistantProfile initProfile() {
        DeclaredAssistantProfile profile = request.getProfile();
        if (profile == null) {
            profile = new DeclaredAssistantProfile();
            profile.setProviderId(request.getProviderId());
        }
        return profile;
    }

    @NotNull
    @Override
    protected AssistantProfileEditForm createForm() {
        return new AssistantProfileEditForm(this);
    }


    @Override
    @NotNull
    protected final Action[] initializeActions() {
        String actionName = request.isNewProfile() ? "Create" : "Update";
        renameAction(getOKAction(), actionName);
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    public void doCancelAction() {
        close(0);
    }

    @Override
    protected void doOKAction() {
        AssistantProfileEditForm form = getForm();
        form.applyFormChanges();
        request.acceptProfile(profile);
        super.doOKAction();
    }
}

