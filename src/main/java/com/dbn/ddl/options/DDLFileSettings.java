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

package com.dbn.ddl.options;

import com.dbn.common.options.CompositeProjectConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.ddl.options.ui.DDFileSettingsForm;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettings;
import com.dbn.options.TopLevelConfig;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
public class DDLFileSettings extends CompositeProjectConfiguration<ProjectSettings, DDFileSettingsForm> implements TopLevelConfig {
    private final @Getter(lazy = true) DDLFileExtensionSettings extensionSettings = new DDLFileExtensionSettings(this);
    private final @Getter(lazy = true) DDLFileGeneralSettings generalSettings = new DDLFileGeneralSettings(this);

    public DDLFileSettings(ProjectSettings parent) {
        super(parent);
    }

    public static DDLFileSettings getInstance(@NotNull Project project) {
        return ProjectSettings.get(project).getDdlFileSettings();
    }

    @NotNull
    @Override
    public String getId() {
        return "DBNavigator.Project.DDLFileSettings";
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.ddlFiles.title.DdlFiles");
    }

    @Override
    public String getHelpTopic() {
        return "ddlFileSettings";
    }

    @Override
    public ConfigId getConfigId() {
        return ConfigId.DDL_FILES;
    }

    @NotNull
    @Override
    public DDLFileSettings getOriginalSettings() {
        return getInstance(getProject());
    }

    /********************************************************
    *                     Configuration                     *
    *********************************************************/
    @Override
    @NotNull
    public DDFileSettingsForm createConfigurationEditor() {
        return new DDFileSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "ddl-file-settings";
    }

    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {
                getExtensionSettings(),
                getGeneralSettings()};
    }
}
