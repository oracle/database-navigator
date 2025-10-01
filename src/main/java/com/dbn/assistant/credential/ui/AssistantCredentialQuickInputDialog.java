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

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Dialogs;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.common.util.Modality.nonModal;

@Getter
public class AssistantCredentialQuickInputDialog extends DBNDialog<AssistantCredentialQuickInputForm> {
    private final AIProvider provider;
    private final Consumer<AssistantCredential> onSave;

    public AssistantCredentialQuickInputDialog(Project project, AIProvider provider, Consumer<AssistantCredential> onSave) {
        super(project, "Provide Credential", true);
        this.provider = provider;
        this.onSave = onSave;
        setModal(true);
        setAutoSize(true);
        setResizable(false);
        init();
    }

    @NotNull
    @Override
    protected AssistantCredentialQuickInputForm createForm() {
        return new AssistantCredentialQuickInputForm(this, provider);
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
        AssistantCredentialQuickInputForm form = getForm();
        form.applyFormChanges();
        onSave.accept(form.getCredential());
        super.doOKAction();
    }

    public static void promptCredentialCreate(@NotNull Project project, AIProvider provider, Runnable callback) {
        Consumer<AssistantCredential> onSave = credential -> {
            AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
            assistantSettings.getCredentialSettings().getCredentials().addCredential(credential);

            Dispatch.run(nonModal(), callback);
        };

        Dialogs.show(() -> new AssistantCredentialQuickInputDialog(project, provider, onSave));
    }

    public static void promptCredentialUpdate(@NotNull Project project, AssistantCredential credential, Runnable callback) {
        AIProviderId providerId = credential.getProviderId();
        Consumer<AssistantCredential> onSave = cred -> {
            credential.setKey(cred.getKey());
            Dispatch.run(nonModal(), callback);
        };

        AIProvider provider = AIProviderData.getProvider(AssistantType.PUBLIC, providerId);
        Dialogs.show(() -> new AssistantCredentialQuickInputDialog(project, provider, onSave));
    }
}

