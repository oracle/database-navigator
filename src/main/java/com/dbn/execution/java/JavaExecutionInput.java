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
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseFeature;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.ExecutionOption;
import com.dbn.execution.ExecutionOptions;
import com.dbn.execution.ExecutionTarget;
import com.dbn.execution.LocalExecutionInput;
import com.dbn.execution.common.input.ExecutionValue;
import com.dbn.execution.common.input.ExecutionVariable;
import com.dbn.execution.common.input.ValueHolder;
import com.dbn.execution.java.result.JavaExecutionResult;
import com.dbn.object.DBJavaClass;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
public class JavaExecutionInput extends LocalExecutionInput implements Comparable<JavaExecutionInput>, Cloneable<JavaExecutionInput> {
    private DBObjectRef<DBJavaMethod> method;

    private transient JavaExecutionResult executionResult;
    private final Map<String, ExecutionValue<String>> inputValues = new LinkedHashMap<>();
    private Map<String, ExecutionVariable> executionVariables = new LinkedHashMap<>();

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

    /**
     * Initializes all database elements required for showing the method input form
     * It makes sure all parameters are loaded, including their java class details if applicable
     * <br>
     * This is to be executed in background before the method execution dialog is shown
     */
    public void initDatabaseElements() {
        DBJavaMethod method = getMethod();
        if (method == null) return;

        List<DBJavaParameter> parameters = method.getParameters();
        for (DBJavaParameter parameter : parameters) {
            if (parameter.isPlainValue()) continue;

            DBJavaClass parameterClass = parameter.getJavaClass();
            initClass(parameterClass);
        }
    }

    private void initClass(DBJavaClass javaClass) {
        if (javaClass == null) return;

        List<DBJavaField> fields = javaClass.getFields();
        for (DBJavaField field : fields) {
            if (field.isPlainValue()) continue;
            initClass(field.getJavaClass());
        }
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
        //return DatabaseFeature.AUTHID_METHOD_EXECUTION.isSupported(getConnection());
        // TODO verify if execution on foreign schema is allowed
        return false;
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

    @Nullable
    public String getInputValue(String path) {
        ExecutionValue<String> fieldValue = inputValues.get(path);
        return fieldValue == null ? null : fieldValue.getValueHolder().getValue() ;
    }

    public String ensureInputValue(String path) {
        ExecutionValue<String> fieldValue = prepareInputValue(path);
        return fieldValue.getValueHolder().getValue();
    }

    public void setInputValue(String path, String value) {
        ExecutionValue<String> fieldValue = prepareInputValue(path);
        fieldValue.getValueHolder().setValue(value);
    }

    public List<String> getInputValueHistory(String path) {
        ExecutionValue<String> fieldValue = prepareInputValue(path) ;

        ValueHolder<?> valueStore = fieldValue.getValueHolder();
        if (valueStore instanceof ExecutionVariable) {
            ExecutionVariable executionVariable = (ExecutionVariable) valueStore;
            return executionVariable.getValueHistory();
        }
        return Collections.emptyList();
    }

    @NotNull
    private ExecutionValue<String> prepareInputValue(String path) {
        return inputValues.computeIfAbsent(path, p -> new ExecutionValue<>(path, getExecutionVariable(path)));
    }

    @NotNull
    private ExecutionVariable getExecutionVariable(String path) {
        return executionVariables.computeIfAbsent(path, p -> new ExecutionVariable(p) );
    }

    /*********************************************************
     *                 PersistentConfiguration               *
     *********************************************************/
    @Override
    public void readConfiguration(Element element) {
        super.readConfiguration(element);
        method.readState(element);
        setTargetSchemaId(SchemaId.get(stringAttribute(element, "execution-schema")));
        Element variablesElement = element.getChild("execution-variables");
        if (variablesElement != null) {
            for (Element variableElement : variablesElement.getChildren()) {
                ExecutionVariable executionVariable = new ExecutionVariable(variableElement);
                executionVariables.put(executionVariable.getPath(), executionVariable);
            }
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        super.writeConfiguration(element);
        method.writeState(element);
        element.setAttribute("execution-schema", getTargetSchemaId() == null ? "" : getTargetSchemaId().id());

        Element variablesElement = newElement(element, "execution-variables");
        for (ExecutionVariable executionVariable : executionVariables.values()) {
            Element variableElement = newElement(variablesElement, "variable");
            executionVariable.writeState(variableElement);
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
        clone.executionVariables = new HashMap<>();
        for (ExecutionVariable executionVariable : executionVariables.values()) {
            clone.executionVariables.put(
                    executionVariable.getPath(),
                    executionVariable.clone());
        }
        return clone;
    }

}
