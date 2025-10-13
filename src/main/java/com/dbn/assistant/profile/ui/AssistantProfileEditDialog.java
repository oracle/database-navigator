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

import com.dbn.assistant.credential.AssistantCredentialBundle;
import com.dbn.assistant.profile.DeclaredAssistantProfile;
import com.dbn.common.routine.Consumer;
import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.Set;

@Getter
public class AssistantProfileEditDialog extends DBNDialog<AssistantProfileEditForm> {
    private final Set<String> usedTitles;
    private final DeclaredAssistantProfile profile;
    private final AssistantCredentialBundle credentials;
    private final Consumer<DeclaredAssistantProfile> onSave;


    public AssistantProfileEditDialog(Project project, DeclaredAssistantProfile profile, AssistantCredentialBundle credentials, Set<String> usedNames, Consumer<DeclaredAssistantProfile> onSave) {
        super(project, profile == null ? "Create Profile" : "Update Profile", true);
        this.profile = profile == null ? new DeclaredAssistantProfile() : profile;
        this.credentials = credentials;
        this.usedTitles = usedNames;
        this.onSave = onSave;
        renameAction(getOKAction(), profile == null ? "Create" : "Update");
        setModal(true);
        init();
    }

    @NotNull
    @Override
    protected AssistantProfileEditForm createForm() {
        return new AssistantProfileEditForm(this, usedTitles);
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
        AssistantProfileEditForm form = getForm();
        form.applyFormChanges();
        onSave.accept(profile);
        super.doOKAction();
    }
}

