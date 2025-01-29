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

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseFeature;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.ExecutionOption;
import com.dbn.execution.ExecutionOptions;
import com.dbn.execution.ExecutionTarget;
import com.dbn.execution.LocalExecutionInput;
import com.dbn.execution.java.result.JavaExecutionResult;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Commons.coalesce;

@Getter
@Setter
public class JavaExecutionInput extends LocalExecutionInput implements Comparable<JavaExecutionInput>, Cloneable<JavaExecutionInput> {
    private DBObjectRef<DBJavaMethod> method;

    private transient JavaExecutionResult executionResult;
    private final List<ArgumentValue> argumentValues = new ArrayList<>();
    private Map<String, JavaExecutionArgumentValue> argumentValueHistory = new HashMap<>();

    public JavaExecutionInput(Project project) {
        super(project, ExecutionTarget.METHOD);
        method = new DBObjectRef<>();

        ExecutionOptions options = getOptions();
        options.set(ExecutionOption.COMMIT_AFTER_EXECUTION, true);
        //setSessionId(SessionId.POOL);
    }

    public JavaExecutionInput(Project project, DBObjectRef<DBJavaMethod> method) {
        super(project, ExecutionTarget.METHOD);
        this.method = method;
        SchemaId methodSchema = method.getSchemaId();

        if (methodSchema != null) {
            setTargetSchemaId(methodSchema);
        }

        if (DatabaseFeature.DATABASE_LOGGING.isSupported(method)) {
            ConnectionHandler connection = Failsafe.nn(method.getConnection());
            getOptions().set(ExecutionOption.ENABLE_LOGGING, connection.isLoggingEnabled());
        }
    }

    public JavaExecutionContext initExecution(DBDebuggerType debuggerType) {
        JavaExecutionResult executionResult = new JavaExecutionResult(this, debuggerType);
        executionResult.setPrevious(this.executionResult);
        this.executionResult = executionResult;
        return initExecutionContext();
    }

    @Override
    protected JavaExecutionContext createExecutionContext() {
        return new JavaExecutionContext(this);
    }

    @Nullable
    @Override
    public ConnectionHandler getConnection() {
        return method == null ? null : method.getConnection();
    }

    @Override
    public ConnectionId getConnectionId() {
        return method.getConnectionId();
    }

    @Override
    public boolean hasExecutionVariables() {
        return false;
    }

    @Override
    public boolean isSchemaSelectionAllowed() {
        return DatabaseFeature.AUTHID_METHOD_EXECUTION.isSupported(getConnection());
    }

    @Override
    public boolean isSessionSelectionAllowed() {
        return true;
    }

    @Override
    public boolean isDatabaseLogProducer() {
        return true;
    }

    @Nullable
    public DBJavaMethod getMethod() {
        return DBObjectRef.get(method);
    }

    public DBObjectRef<DBJavaMethod> getMethodRef() {
        return method;
    }

    public boolean isObsolete() {
        ConnectionHandler connection = method.getConnection();
        return connection == null/* || getMethod() == null*/;
    }

    public boolean isInactive() {
        ConnectionHandler connection = getConnection();
        return connection != null && !connection.getSettings().isActive();
    }

    public void setInputValue(@NotNull DBJavaParameter argument, String value) {
        ArgumentValue argumentValue = getArgumentValue(argument);
        argumentValue.setValue(value);
    }

    public void setInputValue(@NotNull DBJavaField argument, String value) {
        ArgumentValue argumentValue = getArgumentValue(argument);
        argumentValue.setValue(value);
    }

    public String getInputValue(@NotNull DBJavaParameter argument) {
        ArgumentValue argumentValue = getArgumentValue(argument);
        return (String) argumentValue.getValue();
    }

    public String getInputValue(@NotNull DBJavaField argument) {
        ArgumentValue argumentValue = getArgumentValue(argument);
        return (String) argumentValue.getValue();
    }

    public List<String> getInputValueHistory(@NotNull DBJavaField argument) {
        ArgumentValue argumentValue = getArgumentValue(argument);

        ArgumentValueHolder<?> valueStore = argumentValue.getValueHolder();
        if (valueStore instanceof JavaExecutionArgumentValue) {
            JavaExecutionArgumentValue executionVariable = (JavaExecutionArgumentValue) valueStore;
            return executionVariable.getValueHistory();
        }
        return Collections.emptyList();
    }

    public List<String> getInputValueHistory(@NotNull DBJavaParameter argument) {
        ArgumentValue argumentValue = getArgumentValue(argument) ;

        ArgumentValueHolder<?> valueStore = argumentValue.getValueHolder();
        if (valueStore instanceof JavaExecutionArgumentValue) {
            JavaExecutionArgumentValue executionVariable = (JavaExecutionArgumentValue) valueStore;
            return executionVariable.getValueHistory();
        }
        return Collections.emptyList();
    }

    private ArgumentValue getArgumentValue(@NotNull DBJavaParameter argument) {
        for (ArgumentValue argumentValue : argumentValues) {
            if (argumentValue.matches(argument)) {
                return argumentValue;
            }
        }
        ArgumentValue argumentValue = new ArgumentValue(argument, null);
        argumentValue.setValueHolder(getExecutionVariable(argumentValue.getName()));
        argumentValues.add(argumentValue);
        return argumentValue;
    }

    private ArgumentValue getArgumentValue(@NotNull DBJavaField argument) {
        for (ArgumentValue argumentValue : argumentValues) {
            if (argumentValue.matches(argument)) {
                return argumentValue;
            }
        }

        ArgumentValue argumentValue = new ArgumentValue(argument, null);
        argumentValue.setValueHolder(getExecutionVariable(argumentValue.getName()));
        argumentValues.add(argumentValue);
        return argumentValue;
    }

    private JavaExecutionArgumentValue getExecutionVariable(String name) {
        for (JavaExecutionArgumentValue executionVariable : argumentValueHistory.values()) {
            if (Strings.equalsIgnoreCase(executionVariable.getName(), name)) {
                return executionVariable;
            }
        }
        JavaExecutionArgumentValue executionVariable = new JavaExecutionArgumentValue(name);
        argumentValueHistory.put(executionVariable.getName(), executionVariable);
        return executionVariable;
    }

    /*********************************************************
     *                 PersistentConfiguration               *
     *********************************************************/
    @Override
    public void readConfiguration(Element element) {
        super.readConfiguration(element);
        method.readState(element);
        setTargetSchemaId(SchemaId.get(stringAttribute(element, "execution-schema")));
        Element argumentsElement = coalesce(
                () -> element.getChild("argument-values"),
                () -> element.getChild("argument-actions")); // TODO temporary backward functionality
        if (argumentsElement != null) {
            for (Element valueElement : argumentsElement.getChildren()) {
                JavaExecutionArgumentValue argumentValue = new JavaExecutionArgumentValue(valueElement);
                argumentValueHistory.put(argumentValue.getName(), argumentValue);
            }
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        super.writeConfiguration(element);
        method.writeState(element);
        element.setAttribute("execution-schema", getTargetSchemaId() == null ? "" : getTargetSchemaId().id());

        Element argumentValuesElement = newElement(element, "argument-values");
        for (JavaExecutionArgumentValue executionVariable : argumentValueHistory.values()) {
            Element argumentElement = newElement(argumentValuesElement, "argument");
            executionVariable.writeState(argumentElement);
        }
    }

    @Override
    public int compareTo(@NotNull JavaExecutionInput executionInput) {
        DBObjectRef<DBJavaMethod> localMethod = method;
        DBObjectRef<DBJavaMethod> remoteMethod = executionInput.method;
        return localMethod.compareTo(remoteMethod);
    }

    @Override
    public JavaExecutionInput clone() {
        JavaExecutionInput clone = new JavaExecutionInput(getProject());
        clone.method = method;
        clone.setTargetSchemaId(getTargetSchemaId());
        clone.setOptions(ExecutionOptions.clone(getOptions()));
        clone.argumentValueHistory = new HashMap<>();
        for (JavaExecutionArgumentValue executionVariable : argumentValueHistory.values()) {
            clone.argumentValueHistory.put(
                    executionVariable.getName(),
                    executionVariable.clone());
        }
        return clone;
    }

}
