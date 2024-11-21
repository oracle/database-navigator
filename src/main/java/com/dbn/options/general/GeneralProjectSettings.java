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

package com.dbn.options.general;

import com.dbn.common.environment.options.EnvironmentSettings;
import com.dbn.common.locale.options.RegionalSettings;
import com.dbn.common.options.CompositeProjectConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettings;
import com.dbn.options.TopLevelConfig;
import com.dbn.options.general.ui.GeneralProjectSettingsForm;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
public class GeneralProjectSettings extends CompositeProjectConfiguration<ProjectSettings, GeneralProjectSettingsForm> implements TopLevelConfig {
    private final @Getter(lazy = true) RegionalSettings regionalSettings       = new RegionalSettings(this);
    private final @Getter(lazy = true) EnvironmentSettings environmentSettings = new EnvironmentSettings(this);

    public GeneralProjectSettings(ProjectSettings parent) {
        super(parent);
    }

    public static GeneralProjectSettings getInstance(@NotNull Project project) {
        return ProjectSettings.get(project).getGeneralSettings();
    }

    @NotNull
    @Override
    public String getId() {
        return "DBNavigator.Project.GeneralSettings";
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.general.title.General");
    }

    @Override
    public ConfigId getConfigId() {
        return ConfigId.GENERAL;
    }

    @NotNull
    @Override
    public GeneralProjectSettings getOriginalSettings() {
        return getInstance(getProject());
    }

    /*********************************************************
     *                      Configuration                    *
     *********************************************************/
    @Override
    @NotNull
    public GeneralProjectSettingsForm createConfigurationEditor() {
        return new GeneralProjectSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "general-settings";
    }

    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {
                getRegionalSettings(),
                getEnvironmentSettings()};
    }

}
