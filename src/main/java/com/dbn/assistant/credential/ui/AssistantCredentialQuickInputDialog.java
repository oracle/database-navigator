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
import com.dbn.assistant.credential.AssistantCredentialBundle;
import com.dbn.assistant.profile.AssistantProfile;
import com.dbn.assistant.profile.AssistantProfileBundle;
import com.dbn.assistant.profile.DeclaredAssistantProfile;
import com.dbn.assistant.profile.ui.AssistantProfileEditDialog;
import com.dbn.assistant.profile.ui.AssistantProfileEditRequest;
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

import static com.dbn.assistant.profile.AssistantProfileLookup.getImplicitProfile;
import static com.dbn.common.util.Modality.nonModal;

@Getter
public class AssistantCredentialQuickInputDialog extends DBNDialog<AssistantCredentialQuickInputForm> {
    private final AIProvider provider;
    private final AssistantProfile profile;
    private final Consumer<AssistantProfile> profileConsumer;
    private final Consumer<AssistantCredential> credentialConsumer;

    public AssistantCredentialQuickInputDialog(
            Project project,
            AIProvider provider,
            AssistantProfile profile,
            Consumer<AssistantProfile> profileConsumer,
            Consumer<AssistantCredential> credentialConsumer) {
        super(project, "Provide Credential", true);
        this.provider = provider;
        this.profile = profile;
        this.profileConsumer = profileConsumer;
        this.credentialConsumer = credentialConsumer;
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
                createAction("Advanced Setup", () -> openAdvancedSettings()),
                getCancelAction()};
    }

    private void openAdvancedSettings() {
        doCancelAction();
        Project project = getProject();
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        AssistantCredentialBundle credentials = assistantSettings.getCredentialSettings().getCredentials();
        AssistantProfileBundle profiles = assistantSettings.getProfileSettings().getProfiles();

        DeclaredAssistantProfile declaredProfile = null;
        if (profile instanceof DeclaredAssistantProfile) {
            declaredProfile = (DeclaredAssistantProfile) profile;
        }
        boolean createProfile = declaredProfile == null;

        AssistantProfileEditRequest request = AssistantProfileEditRequest
                .builder()
                .profile(declaredProfile)
                .profiles(profiles)
                .credentials(credentials)
                .providerId(provider.getId())
                .saveConsumer(p -> {
                    if (createProfile) profiles.addDeclaredProfile(p);
                    profileConsumer.accept(p);
                })
                .build();

        Dialogs.show(() -> new AssistantProfileEditDialog(project, request));
    }

    @Override
    public void doCancelAction() {
        close(0);
    }

    @Override
    protected void doOKAction() {
        AssistantCredentialQuickInputForm form = getForm();
        form.applyFormChanges();
        credentialConsumer.accept(form.getCredential());
        super.doOKAction();
    }

    public static void promptCredentialCreate(@NotNull Project project, AssistantProfile profile, Consumer<AssistantProfile> profileConsumer) {
        AIProvider provider = profile.getProvider();

        Consumer<AssistantCredential> credentialConsumer = credential -> {
            AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
            assistantSettings.getCredentialSettings().getCredentials().addCredential(credential);
            credential.updateSecrets( null);

            AssistantProfile targetProfile;
            if (profile instanceof DeclaredAssistantProfile declaredProfile) {
                declaredProfile.setCredentialId(credential.getId());
                targetProfile = declaredProfile;
            } else {
                targetProfile = getImplicitProfile(project, provider.getId());
            }

            Dispatch.run(nonModal(), () -> profileConsumer.accept(targetProfile));
        };

        Dialogs.show(() -> new AssistantCredentialQuickInputDialog(project, provider, profile, profileConsumer, credentialConsumer));
    }

    public static void promptCredentialUpdate(@NotNull Project project, AssistantProfile profile, AssistantCredential credential, Consumer<AssistantProfile> profileConsumer) {
        AIProviderId providerId = credential.getProviderId();
        Consumer<AssistantCredential> cretentialConsumer = cred -> {
            credential.updateFrom(cred);
            Dispatch.run(nonModal(), () -> profileConsumer.accept(profile));
        };

        AIProvider provider = AIProviderData.getProvider(AssistantType.PUBLIC, providerId);
        Dialogs.show(() -> new AssistantCredentialQuickInputDialog(project, provider, profile, profileConsumer, cretentialConsumer));
    }
}

