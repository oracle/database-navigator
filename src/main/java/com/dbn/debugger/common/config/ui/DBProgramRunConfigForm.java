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

package com.dbn.debugger.common.config.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.common.config.DBRunConfig;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import lombok.Getter;

@Getter
public abstract class DBProgramRunConfigForm<T extends DBRunConfig<?>> extends DBNFormBase {
    private final DBDebuggerType debuggerType;


    public DBProgramRunConfigForm(Project project, DBDebuggerType debuggerType) {
        super(null, project);
        this.debuggerType = debuggerType;
    }

    public abstract void readConfiguration(T configuration);

    public abstract void writeConfiguration(T configuration) throws ConfigurationException;
}
