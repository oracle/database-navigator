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

package com.dbn.project;

import com.dbn.DatabaseNavigator;
import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.state.StateEncryption;
import com.dbn.connection.config.ConnectionBundleSettings;
import com.dbn.options.ProjectWorkspaceSettingsManager;
import com.dbn.plugin.PluginConflictManager;
import com.dbn.plugin.PluginStatusManager;
import com.dbn.vfs.DatabaseFileManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Compatibility
public class ProjectStartupActivity implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        DatabaseNavigator.getInstance();
        ConnectionBundleSettings.getInstance(project);
        DatabaseBrowserManager.getInstance(project);
        DatabaseAssistantManager.getInstance(project);
        ProjectComponentsInitializer.getInstance(project);
        ProjectWorkspaceSettingsManager.getInstance(project);

        evaluatePluginStatus(project);
        assesPluginConflict(project);
        initializeDatabaseAssistant(project);
        initializeStateEncryption();
        reopenDatabaseEditors(project);
        return null;
    }

    private static void evaluatePluginStatus(Project project) {
        PluginStatusManager pluginStatusManager = PluginStatusManager.getInstance();
        pluginStatusManager.evaluatePluginStatus(project);
    }

    private static void assesPluginConflict(Project project) {
        PluginConflictManager conflictManager = PluginConflictManager.getInstance();
        conflictManager.assesPluginConflict(project);
    }

    private static void initializeStateEncryption() {
        StateEncryption.initialize();
    }

    private static void initializeDatabaseAssistant(Project project) {
        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
        assistantManager.initializeAssistant();
    }

    private static void reopenDatabaseEditors(Project project) {
        DatabaseFileManager fileManager = DatabaseFileManager.getInstance(project);
        fileManager.reopenDatabaseEditors();
    }
}
