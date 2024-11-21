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

package com.dbn.code.common.style.options;

import com.dbn.code.common.style.options.ui.CodeStyleSettingsForm;
import com.dbn.code.psql.style.PSQLCodeStyle;
import com.dbn.code.psql.style.options.PSQLCodeStyleSettings;
import com.dbn.code.sql.style.SQLCodeStyle;
import com.dbn.code.sql.style.options.SQLCodeStyleSettings;
import com.dbn.common.options.CompositeProjectConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.options.ProjectSettings;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ProjectCodeStyleSettings extends CompositeProjectConfiguration<ProjectSettings, CodeStyleSettingsForm> {
    public ProjectCodeStyleSettings(ProjectSettings parent){
        super(parent);
    }

    public static ProjectCodeStyleSettings getInstance(@NotNull Project project) {
        return ProjectSettings.get(project).getCodeStyleSettings();
    }

    @NotNull
    @Override
    public String getId() {
        return "DBNavigator.Project.CodeStyleSettings";
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.codeStyle.title.CodeStyle");
    }

    @Override
    @NotNull
    public CodeStyleSettingsForm createConfigurationEditor() {
        return new CodeStyleSettingsForm(this);
    }

    public SQLCodeStyleSettings getSQLCodeStyleSettings() {
        return SQLCodeStyle.settings(getProject());
    }

    public PSQLCodeStyleSettings getPSQLCodeStyleSettings() {
        return PSQLCodeStyle.settings(getProject());
    }

    /*********************************************************
    *                     Configuration                      *
    *********************************************************/
    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {
                getSQLCodeStyleSettings(),
                getPSQLCodeStyleSettings()};
    }
}
