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

package com.dbn.execution.method;

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
import com.dbn.execution.common.input.ExecutionVariable;
import com.dbn.execution.common.input.ValueHolder;
import com.dbn.execution.method.result.MethodExecutionResult;
import com.dbn.object.DBArgument;
import com.dbn.object.DBMethod;
import com.dbn.object.DBType;
import com.dbn.object.DBTypeAttribute;
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
public class MethodExecutionInput extends LocalExecutionInput implements Comparable<MethodExecutionInput>, Cloneable<MethodExecutionInput> {
    private DBObjectRef<DBMethod> method;

    private transient MethodExecutionResult executionResult;
    private final List<ArgumentValue> argumentValues = new ArrayList<>();
    private Map<String, ExecutionVariable> argumentValueHistory = new HashMap<>();

    public MethodExecutionInput(Project project) {
        super(project, ExecutionTarget.METHOD);
        method = new DBObjectRef<>();

        ExecutionOptions options = getOptions();
        options.set(ExecutionOption.COMMIT_AFTER_EXECUTION, true);
        //setSessionId(SessionId.POOL);
    }

    public MethodExecutionInput(Project project, DBObjectRef<DBMethod> method) {
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

    public MethodExecutionContext initExecution(DBDebuggerType debuggerType) {
        MethodExecutionResult executionResult = new MethodExecutionResult(this, debuggerType);
        executionResult.setPrevious(this.executionResult);
        this.executionResult = executionResult;
        return initExecutionContext();
    }

    /**
     * Initializes all database elements required for showing the method input form
     * It makes sure all arguments are loaded, including their declared type details if applicable
     * <br>
     * This is to be executed in background before the method execution dialog is shown
     */
    public void initDatabaseElements() {
        DBMethod method = getMethod();
        if (method == null) return;

        List<DBArgument> arguments = method.getArguments();
        for (DBArgument argument : arguments) {
            DBType declaredType = argument.getDataType().getDeclaredType();
            if (declaredType != null) {
                declaredType.getAttributes();
            }
        }
    }

    @Override
    protected MethodExecutionContext createExecutionContext() {
        return new MethodExecutionContext(this);
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
    public DBMethod getMethod() {
        return DBObjectRef.get(method);
    }

    public DBObjectRef<DBMethod> getMethodRef() {
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

    public void setInputValue(@NotNull DBArgument argument, DBTypeAttribute typeAttribute, String value) {
        ArgumentValue argumentValue = getArgumentValue(argument, typeAttribute);
        argumentValue.setValue(value);
    }

    public void setInputValue(@NotNull DBArgument argument, String value) {
        ArgumentValue argumentValue = getArgumentValue(argument);
        argumentValue.setValue(value);
    }

    public String getInputValue(@NotNull DBArgument argument) {
        ArgumentValue argumentValue = getArgumentValue(argument);
        return (String) argumentValue.getValue();
    }

    public List<String> getInputValueHistory(@NotNull DBArgument argument, @Nullable DBTypeAttribute typeAttribute) {
        ArgumentValue argumentValue =
                typeAttribute == null ?
                        getArgumentValue(argument) :
                        getArgumentValue(argument, typeAttribute);

        ValueHolder valueStore = argumentValue.getValueHolder();
        if (valueStore instanceof ExecutionVariable) {
            ExecutionVariable executionVariable = (ExecutionVariable) valueStore;
            return executionVariable.getValueHistory();
        }
        return Collections.emptyList();
    }

    public String getInputValue(DBArgument argument, DBTypeAttribute typeAttribute) {
        ArgumentValue argumentValue = getArgumentValue(argument, typeAttribute);
        return (String) argumentValue.getValue();
    }

    private ArgumentValue getArgumentValue(@NotNull DBArgument argument) {
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

    private ArgumentValue getArgumentValue(DBArgument argument, DBTypeAttribute attribute) {
        for (ArgumentValue argumentValue : argumentValues) {
            if (argumentValue.matches(argument)  && argumentValue.matches(attribute)) {
                return argumentValue;
            }
        }

        ArgumentValue argumentValue = new ArgumentValue(argument, attribute, null);
        argumentValue.setValueHolder(getExecutionVariable(argumentValue.getName()));
        argumentValues.add(argumentValue);
        return argumentValue;
    }

    private ExecutionVariable getExecutionVariable(String name) {
        for (ExecutionVariable executionVariable : argumentValueHistory.values()) {
            if (Strings.equalsIgnoreCase(executionVariable.getPath(), name)) {
                return executionVariable;
            }
        }
        ExecutionVariable executionVariable = new ExecutionVariable(name);
        argumentValueHistory.put(executionVariable.getPath(), executionVariable);
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
                ExecutionVariable argumentValue = new ExecutionVariable(valueElement);
                argumentValueHistory.put(argumentValue.getPath(), argumentValue);
            }
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        super.writeConfiguration(element);
        method.writeState(element);
        element.setAttribute("execution-schema", getTargetSchemaId() == null ? "" : getTargetSchemaId().id());

        Element argumentValuesElement = newElement(element, "argument-values");
        for (ExecutionVariable executionVariable : argumentValueHistory.values()) {
            Element argumentElement = newElement(argumentValuesElement, "argument");
            executionVariable.writeState(argumentElement);
        }
    }

    @Override
    public int compareTo(@NotNull MethodExecutionInput executionInput) {
        DBObjectRef<DBMethod> localMethod = method;
        DBObjectRef<DBMethod> remoteMethod = executionInput.method;
        return localMethod.compareTo(remoteMethod);
    }

    @Override
    public MethodExecutionInput clone() {
        MethodExecutionInput clone = new MethodExecutionInput(getProject());
        clone.method = method;
        clone.setTargetSchemaId(getTargetSchemaId());
        clone.setOptions(ExecutionOptions.clone(getOptions()));
        clone.argumentValueHistory = new HashMap<>();
        for (ExecutionVariable executionVariable : argumentValueHistory.values()) {
            clone.argumentValueHistory.put(
                    executionVariable.getPath(),
                    executionVariable.clone());
        }
        return clone;
    }

}
