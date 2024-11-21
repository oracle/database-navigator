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

import com.dbn.common.icon.Icons;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.jdbc.config.DBStatementJdbcRunConfigFactory;
import com.dbn.debugger.jdwp.config.DBJdwpStatementRunConfigFactory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

@Getter
public class DBStatementRunConfigType extends DBRunConfigType<DBStatementRunConfigFactory> {
    public static final String DEFAULT_RUNNER_NAME = "DB Statement Runner";
    private final DBStatementRunConfigFactory[] configurationFactories = new DBStatementRunConfigFactory[]{
            new DBStatementJdbcRunConfigFactory(this),
            new DBJdwpStatementRunConfigFactory(this)};


    @NotNull
    @Override
    public String getDisplayName() {
        return "DB Statement";
    }

    @Override
    public String getConfigurationTypeDescription() {
        return "DB Navigator - Statement Runner";
    }

    @Override
    public Icon getIcon() {
        return Icons.EXEC_STATEMENT_CONFIG;
    }

    @Override
    @NotNull
    public String getId() {
        return "DBNStatementRunConfiguration";
    }

    @Override
    public String getDefaultRunnerName() {
        return DEFAULT_RUNNER_NAME;
    }

    @Override
    public DBStatementRunConfigFactory getConfigurationFactory(DBDebuggerType debuggerType) {
        for (DBStatementRunConfigFactory configurationFactory : configurationFactories) {
            if (configurationFactory.getDebuggerType() == debuggerType) {
                return configurationFactory;
            }
        }
        return null;
    }
}
