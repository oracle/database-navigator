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

package com.dbn.debugger.common.config;

import com.dbn.debugger.DBDebuggerType;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class DBRunConfigFactory<T extends DBRunConfigType, C extends DBRunConfig> extends ConfigurationFactory {
    private final DBDebuggerType debuggerType;

    protected DBRunConfigFactory(T type, DBDebuggerType debuggerType) {
        super(type);
        this.debuggerType = debuggerType;
    }

    @NotNull
    @Override
    public RunConfiguration createConfiguration(String name, @NotNull RunConfiguration template) {
        RunConfiguration configuration = super.createConfiguration(name, template);
        if (template instanceof DBRunConfig) {
            DBRunConfig templateConfig = (DBRunConfig) template;
            DBRunConfigCategory category = templateConfig.getCategory();
            if (category == DBRunConfigCategory.TEMPLATE) {
                ((DBRunConfig)configuration).setCategory(DBRunConfigCategory.CUSTOM);
            }
        }
        return configuration;
    }

    @NotNull
    @Override
    public T getType() {
        return (T) super.getType();
    }

    public abstract C createConfiguration(Project project, String name, DBRunConfigCategory category);

    @NotNull
    @Override
    public String getId() {
        return getName();
    }
}
