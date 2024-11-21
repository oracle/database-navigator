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

package com.dbn.code.common.completion.options;

import com.dbn.code.common.completion.options.filter.CodeCompletionFiltersSettings;
import com.dbn.code.common.completion.options.general.CodeCompletionFormatSettings;
import com.dbn.code.common.completion.options.sorting.CodeCompletionSortingSettings;
import com.dbn.code.common.completion.options.ui.CodeCompletionSettingsForm;
import com.dbn.common.options.CompositeProjectConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.common.util.XmlContents;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettings;
import com.dbn.options.TopLevelConfig;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
public class CodeCompletionSettings extends CompositeProjectConfiguration<ProjectSettings, CodeCompletionSettingsForm> implements TopLevelConfig {
    private final @Getter(lazy = true) CodeCompletionFiltersSettings filterSettings = new CodeCompletionFiltersSettings(this);
    private final @Getter(lazy = true) CodeCompletionSortingSettings sortingSettings = new CodeCompletionSortingSettings(this);
    private final @Getter(lazy = true) CodeCompletionFormatSettings formatSettings = new CodeCompletionFormatSettings(this);

    public CodeCompletionSettings(ProjectSettings parent) {
        super(parent);
        loadDefaults();
    }

    public static CodeCompletionSettings getInstance(@NotNull Project project) {
        return ProjectSettings.get(project).getCodeCompletionSettings();
    }

    @NotNull
    @Override
    public String getId() {
        return "DBNavigator.Project.CodeCompletionSettings";
    }


    @Override
    public String getDisplayName() {
        return txt("cfg.codeCompletion.title.CodeCompletion");
    }

    @Override
    public String getHelpTopic() {
        return "codeEditor";
    }

    @Override
    public ConfigId getConfigId() {
        return ConfigId.CODE_COMPLETION;
    }

    private void loadDefaults() {
        Element element = loadDefinition();
        readConfiguration(element);
    }

    @SneakyThrows
    private Element loadDefinition() {
        return XmlContents.fileToElement(getClass(), "default-settings.xml");
    }

    /*********************************************************
     *                     Configuration                      *
     *********************************************************/

    @Override
    @NotNull
    public CodeCompletionSettingsForm createConfigurationEditor() {
        return new CodeCompletionSettingsForm(this);
    }

    @NotNull
    @Override
    public CodeCompletionSettings getOriginalSettings() {
        return CodeCompletionSettings.getInstance(getProject());
    }

    @Override
    public String getConfigElementName() {
        return "code-completion-settings";
    }

    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[]{
                getFilterSettings(),
                getSortingSettings(),
                getFormatSettings()};
    }
}
