/*
 * Copyright 2025 Oracle and/or its affiliates
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

import com.dbn.common.icon.Icons;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.jdwp.config.DBJdwpJavaRunConfigFactory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

@Getter
public class DBJavaRunConfigType extends DBRunConfigType<DBJavaRunConfigFactory> {
    public static final String DEFAULT_RUNNER_NAME = txt("cfg.execution.title.JavaMethodRunnerDefault");

    private final DBJavaRunConfigFactory[] configurationFactories = new DBJavaRunConfigFactory[]{
            new DBJdwpJavaRunConfigFactory(this)};


    @NotNull
    @Override
    public String getDisplayName() {
        return txt("cfg.execution.title.JavaMethodRunner");
    }

    @Override
    public String getConfigurationTypeDescription() {
        return txt("cfg.execution.text.JavaMethodRunner");
    }

    @Override
    public Icon getIcon() {
        return Icons.EXEC_METHOD_CONFIG;
    }

    @Override
    @NotNull
    public String getId() {
        return "DBNJavaMethodRunConfiguration";
    }

    @Override
    public String getDefaultRunnerName() {
        return DEFAULT_RUNNER_NAME;
    }

    @Override
    public DBJavaRunConfigFactory getConfigurationFactory(DBDebuggerType debuggerType) {
        for (DBJavaRunConfigFactory configurationFactory : configurationFactories) {
            if (configurationFactory.getDebuggerType() == debuggerType) {
                return configurationFactory;
            }
        }
        return null;
    }
}
