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

package com.dbn.execution.java.result;

import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ref.WeakRef;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.ExecutionResultBase;
import com.dbn.execution.common.input.ExecutionValue;
import com.dbn.execution.common.input.ValueHolder;
import com.dbn.execution.java.JavaExecutionContext;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.result.ui.JavaExecutionResultForm;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Lists.sortedCopy;
import static com.dbn.object.DBOrderedObject.POSITION_COMPARATOR;

@Getter
@Setter
public class JavaExecutionResult extends ExecutionResultBase<JavaExecutionResultForm> {
    private final WeakRef<JavaExecutionInput> executionInput;
    private final List<ExecutionValue> fieldValues = new ArrayList<>();
    private final DBDebuggerType debuggerType;
    private String logOutput;
    private int executionDuration;

    private Map<DBObjectRef<DBJavaParameter>, ResultSetDataModel> cursorModels = DisposableContainers.map(this);

    public JavaExecutionResult(JavaExecutionInput executionInput, DBDebuggerType debuggerType) {
        this.executionInput = WeakRef.of(executionInput);
        this.debuggerType = debuggerType;
    }

    public void calculateExecDuration() {
        this.executionDuration = (int) (System.currentTimeMillis() - getExecutionContext().getExecutionTimestamp());
    }

    private void addComplexArgumentValues(String parentName, List<DBJavaField> parameter, Struct value) throws SQLException {
        Object[] returnObjects = value.getAttributes();
        for (int i=0; i < returnObjects.length; i++){
            Object returnObject = returnObjects[i];
            if(returnObject instanceof Struct){
                List<DBJavaField> fields = parameter.get(i).getJavaClass().getFields();
                fields = sortedCopy(fields, POSITION_COMPARATOR);
                addComplexArgumentValues(parentName + "." + parameter.get(i).getName(), fields, (java.sql.Struct) returnObject);
            } else {
                addArgumentValue(parentName + "." + parameter.get(i).getName(), returnObject);
            }
        }
    }

    public void addArgumentValue(String parameter, Object value) throws SQLException {
        ValueHolder<Object> valueStore = ValueHolder.basic(value);
        if(value instanceof Struct) {
            List<DBJavaField> fields = getMethod().getReturnClass().getFields();
            fields = sortedCopy(fields, POSITION_COMPARATOR);
            addComplexArgumentValues("", fields, (java.sql.Struct) value);
        } else {
            ExecutionValue fieldValue = new ExecutionValue(parameter, valueStore);
            fieldValues.add(fieldValue);
        }
    }

    @Nullable
    @Override
    public JavaExecutionResultForm createForm() {
        return new JavaExecutionResultForm(this);
    }

    @Override
    @NotNull
    public String getName() {
        return getMethod().getName();
    }

    @Override
    public Icon getIcon() {
        return getMethod().getOriginalIcon();
    }

    @NotNull
    public JavaExecutionInput getExecutionInput() {
        return executionInput.ensure();
    }

    public JavaExecutionContext getExecutionContext() {
        return getExecutionInput().getExecutionContext();
    }

    @NotNull
    public DBJavaMethod getMethod() {
        return Failsafe.nn(getExecutionInput().getMethod());
    }


    @Override
    @NotNull
    public Project getProject() {
        return getMethod().getProject();
    }

    @Override
    public ConnectionId getConnectionId() {
        return getExecutionInput().getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return getMethod().getConnection();
    }

    @Override
    public DBLanguagePsiFile createPreviewFile() {
        return null;
    }


    public ResultSetDataModel getTableModel(DBJavaParameter argument) {
        return cursorModels.get(argument.ref());
    }

    /********************************************************
     *                    Data Provider                     *
     ********************************************************/
    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.JAVA_EXECUTION_RESULT.is(dataId)) return this;
        return null;
    }
}
