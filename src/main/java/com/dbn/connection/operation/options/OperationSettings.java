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

package com.dbn.connection.operation.options;

import com.dbn.common.options.CompositeProjectConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.connection.operation.options.ui.OperationsSettingsForm;
import com.dbn.connection.transaction.options.TransactionManagerSettings;
import com.dbn.editor.session.options.SessionBrowserSettings;
import com.dbn.execution.compiler.options.CompilerSettings;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettings;
import com.dbn.options.TopLevelConfig;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
public class OperationSettings extends CompositeProjectConfiguration<ProjectSettings, OperationsSettingsForm> implements TopLevelConfig {
    private final @Getter(lazy = true) TransactionManagerSettings transactionManagerSettings = new TransactionManagerSettings(this);
    private final @Getter(lazy = true) SessionBrowserSettings sessionBrowserSettings         = new SessionBrowserSettings(this);
    private final @Getter(lazy = true) CompilerSettings compilerSettings                     = new CompilerSettings(this);


    public OperationSettings(ProjectSettings parent) {
        super(parent);
    }

    public static OperationSettings getInstance(@NotNull Project project) {
        return ProjectSettings.get(project).getOperationSettings();
    }

    @NotNull
    @Override
    public String getId() {
        return "DBNavigator.Project.OperationSettings";
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.operations.title.Operations");
    }

    @Override
    public String getHelpTopic() {
        return "operations";
    }

    @Override
    public ConfigId getConfigId() {
        return ConfigId.OPERATIONS;
    }

    @NotNull
    @Override
    public OperationSettings getOriginalSettings() {
        return getInstance(getProject());
    }

    /*********************************************************
     *                     Configuration                     *
     *********************************************************/
    @Override
    @NotNull
    public OperationsSettingsForm createConfigurationEditor() {
        return new OperationsSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "operation-settings";
    }

    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {
                getTransactionManagerSettings(),
                getSessionBrowserSettings(),
                getCompilerSettings()};
    }
}
