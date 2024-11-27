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

package com.dbn.editor.code.options;

import com.dbn.common.options.CompositeProjectConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.editor.code.options.ui.CodeEditorSettingsForm;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettings;
import com.dbn.options.TopLevelConfig;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
public class CodeEditorSettings extends CompositeProjectConfiguration<ProjectSettings, CodeEditorSettingsForm> implements TopLevelConfig {
    private final @Getter(lazy = true) CodeEditorGeneralSettings generalSettings           = new CodeEditorGeneralSettings(this);
    private final @Getter(lazy = true) CodeEditorConfirmationSettings confirmationSettings = new CodeEditorConfirmationSettings(this);

    public CodeEditorSettings(ProjectSettings parent) {
        super(parent);
    }

    public static CodeEditorSettings getInstance(@NotNull Project project) {
        return ProjectSettings.get(project).getCodeEditorSettings();
    }

    @NotNull
    @Override
    public String getId() {
        return "DBNavigator.Project.CodeEditorSettings";
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.codeEditor.title.CodeEditor");
    }

    @Override
    public String getHelpTopic() {
        return "codeEditor";
    }

    @Override
    public ConfigId getConfigId() {
        return ConfigId.CODE_EDITOR;
    }

    @NotNull
    @Override
    public CodeEditorSettings getOriginalSettings() {
        return getInstance(getProject());
    }

    /*********************************************************
     *                     Configuration                     *
     *********************************************************/
    @Override
    @NotNull
    public CodeEditorSettingsForm createConfigurationEditor() {
        return new CodeEditorSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "code-editor-settings";
    }

    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {
            getGeneralSettings(),
            getConfirmationSettings()};
    }
}
