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

package com.dbn.assistant.profile;

import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.common.routine.Consumer;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;

import static com.dbn.assistant.credential.AssistantCredentialLookup.getCredential;
import static com.dbn.assistant.credential.ui.AssistantCredentialQuickInputDialog.promptCredentialCreate;
import static com.dbn.assistant.credential.ui.AssistantCredentialQuickInputDialog.promptCredentialUpdate;

@UtilityClass
public class AssistantProfileUtil {

    public static void verifyAssistantProfile(Project project, AssistantProfile profile, Consumer<AssistantProfile> callback) {
        if (profile instanceof PotentialAssistantProfile) {
            promptCredentialCreate(project, profile, callback);
            return;
        }

        if (profile instanceof ImplicitAssistantProfile implicitProfile) {
            AssistantCredential credential = implicitProfile.getCredential();
            verifyAssistantCredential(project, profile, credential, callback);
            return;
        }

        if (profile instanceof DeclaredAssistantProfile declaredProfile) {
            String credentialId = declaredProfile.getCredentialId();
            AssistantCredential credential = getCredential(project, credentialId);

            verifyAssistantCredential(project, profile, credential, callback);
        }
    }

    private static void verifyAssistantCredential(Project project, AssistantProfile profile, AssistantCredential credential, Consumer<AssistantProfile> profileConsumer) {
        if (credential == null) {
            promptCredentialCreate(project, profile, profileConsumer);

        } else if (!credential.isProvided()) {
            promptCredentialUpdate(project, profile, credential, profileConsumer);

        } else {
            profileConsumer.accept(profile);
        }
    }
}
