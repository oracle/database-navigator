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

package com.dbn.execution.java;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.execution.ExecutionContext;
import com.dbn.execution.ExecutionOptions;
import com.dbn.execution.java.wrapper.Wrapper;
import com.dbn.execution.java.wrapper.WrapperBuilder;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class JavaExecutionContext extends ExecutionContext<JavaExecutionInput> {
    private Wrapper wrapper;
    public JavaExecutionContext(JavaExecutionInput input) {
        super(input);
    }

    public void initWrapper(DBJavaMethod method) throws Exception {
        wrapper = WrapperBuilder.getInstance().build(method);
    }

    @NotNull
    @Override
    public String getTargetName() {
        DBObjectRef<DBJavaMethod> method = getInput().getMethodRef();
        return method.getObjectType().getName() + " " + method.getObjectName();
    }

    @Nullable
    @Override
    public ConnectionHandler getTargetConnection() {
        return getInput().getConnection();
    }

    @Nullable
    @Override
    public SchemaId getTargetSchema() {
        return getInput().getTargetSchemaId();
    }

    public ExecutionOptions getOptions() {
        return getInput().getOptions();
    }
}
