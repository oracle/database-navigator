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

package com.dbn.navigation.options;

import com.dbn.common.options.CompositeProjectConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.navigation.options.ui.NavigationSettingsForm;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettings;
import com.dbn.options.TopLevelConfig;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
public class NavigationSettings extends CompositeProjectConfiguration<ProjectSettings, NavigationSettingsForm> implements TopLevelConfig {
    private final @Getter(lazy = true) ObjectsLookupSettings objectsLookupSettings = new ObjectsLookupSettings(this);

    public NavigationSettings(ProjectSettings parent) {
        super(parent);
    }

    public static NavigationSettings getInstance(@NotNull Project project) {
        return ProjectSettings.get(project).getNavigationSettings();
    }

    @NotNull
    @Override
    public String getId() {
        return "DBNavigator.Project.NavigationSettings";
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.navigation.title.Navigation");
    }

    @Override
    public String getHelpTopic() {
        return "navigationSettings";
    }

    @Override
    public ConfigId getConfigId() {
        return ConfigId.NAVIGATION;
    }

    @NotNull
    @Override
    public NavigationSettings getOriginalSettings() {
        return getInstance(getProject());
    }


    /*********************************************************
     *                     Configuration                     *
     *********************************************************/
    @NotNull
    @Override
    public NavigationSettingsForm createConfigurationEditor() {
        return new NavigationSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "navigation-settings";
    }

    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {getObjectsLookupSettings()};
    }
}
