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

package com.dbn.plugin;

import com.dbn.common.file.FileTypeService;
import com.dbn.common.project.Projects;
import com.dbn.debugger.ExecutionConfigManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginStateListener;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.dbn.DatabaseNavigator.DBN_PLUGIN_ID;

public class DBNPluginStateListener implements PluginStateListener {
    @Override
    public void install(@NotNull IdeaPluginDescriptor descriptor) {
    }

    @Override
    public void uninstall(@NotNull IdeaPluginDescriptor descriptor) {
        if (!Objects.equals(descriptor.getPluginId(), DBN_PLUGIN_ID)) return;

        // bye bye...
        FileTypeService fileTypeService = FileTypeService.getInstance();
        fileTypeService.restoreFileAssociations();

        Project[] projects = Projects.getOpenProjects();
        for (Project project : projects) {
            ExecutionConfigManager executionConfigManager = ExecutionConfigManager.getInstance(project);
            executionConfigManager.removeRunConfigurations();
        }

        PluginConflictManager conflictManager = PluginConflictManager.getInstance();
        conflictManager.setConflictPrompted(false);
        conflictManager.setFileTypesClaimed(false);
    }
}
