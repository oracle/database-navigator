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
import com.dbn.execution.method.MethodExecutionInput;
import com.dbn.object.DBMethod;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.execution.configurations.RunConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public abstract class DBMethodRunConfigFactory<T extends DBMethodRunConfigType, C extends DBMethodRunConfig> extends DBRunConfigFactory<T, C> {
    protected DBMethodRunConfigFactory(T type, DBDebuggerType debuggerType) {
        super(type, debuggerType);
    }

    @Override
    public Icon getIcon(@NotNull RunConfiguration configuration) {
        C runConfiguration = (C) configuration;
        MethodExecutionInput executionInput = runConfiguration.getExecutionInput();
        if (executionInput == null || runConfiguration.getCategory() != DBRunConfigCategory.CUSTOM) {
            return getIcon();
        } else {
            DBObjectRef<DBMethod> methodRef = executionInput.getMethodRef();
            DBMethod method = methodRef.get();
            return method == null ? methodRef.getObjectType().getIcon() : method.getIcon();
        }
    }

    public abstract C createConfiguration(DBMethod method);
}
