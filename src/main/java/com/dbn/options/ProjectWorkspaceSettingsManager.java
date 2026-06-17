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

package com.dbn.options;

import com.dbn.assistant.credential.AssistantCredentialSettings;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.options.ConfigMonitor;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.ConfigActivity.INITIALIZING;
import static com.dbn.common.options.ConfigStorage.WORKSPACE;
import static com.dbn.common.options.setting.Settings.newStateElement;


@State(
        name = ProjectWorkspaceSettingsManager.COMPONENT_NAME,
        storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
public class ProjectWorkspaceSettingsManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.Workspace.Settings";

    protected ProjectWorkspaceSettingsManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    public static ProjectWorkspaceSettingsManager getInstance(@NotNull Project project) {
        return projectService(project, ProjectWorkspaceSettingsManager.class);
    }

    private ProjectSettings getProjectSettings() {
        ProjectSettingsManager projectSettingsManager = ProjectSettingsManager.getInstance(getProject());
        return projectSettingsManager.getProjectSettings();
    }

    @Override
    public void initializeComponent() {
        restoreKeychainSecrets();
    }

    /**
     * Restores authentication passwords from the IDE keychain
     * (to be used once on component initialization)
     */
    private void restoreKeychainSecrets() {
        // LOCAL CREDENTIALS
        AssistantSettings assistantSettings = getProjectSettings().getAssistantSettings();
        AssistantCredentialSettings credentialSettings = assistantSettings.getCredentialSettings();
        credentialSettings.getCredentials().initSecrets();
    }

    @Nullable
    @Override
    public Element getComponentState() {
        try {
            ConfigMonitor.set(WORKSPACE, true);
            Element element = newStateElement();
            ProjectSettings projectSettings = getProjectSettings();
            projectSettings.writeConfiguration(element);
            return element;
        } finally {
            ConfigMonitor.set(WORKSPACE, false);
        }
    }

    @Override
    public synchronized void loadComponentState(@NotNull Element element) {
        try {
            ConfigMonitor.set(WORKSPACE, true);
            ConfigMonitor.set(INITIALIZING, true);
            ProjectSettings projectSettings = getProjectSettings();
            projectSettings.readConfiguration(element);
        } finally {
            ConfigMonitor.set(WORKSPACE, false);
            ConfigMonitor.set(INITIALIZING, false);
        }
    }
}
