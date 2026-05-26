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

package com.dbn.execution.statement.variables;

import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.GenericDataType;
import com.dbn.execution.statement.StatementExecutionManager;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.element.util.IdentifierCategory;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.ExecVariablePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.lookup.ObjectLookupAdapter;
import com.dbn.object.DBColumn;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.replaceIgnoreCase;
import static com.dbn.execution.statement.variables.VariableNames.adjust;

@Getter
public class StatementExecutionVariablesBundle extends StatefulDisposableBase implements StatefulDisposable {
    public static final Comparator<StatementExecutionVariable> NAME_COMPARATOR = Comparator.comparing(StatementExecutionVariable::getName);
    public static final Comparator<StatementExecutionVariable> OFFSET_COMPARATOR = Comparator.comparingInt(StatementExecutionVariable::getOffset);
    public static final Comparator<StatementExecutionVariable> NAME_LENGTH_COMPARATOR = (o1, o2) -> o2.getName().length() - o1.getName().length();

    private List<StatementExecutionVariable> variables = new ArrayList<>();
    private transient List<StatementExecutionVariable> bindVariables = new ArrayList<>();

    public StatementExecutionVariablesBundle(List<ExecVariablePsiElement> variablePsiElements) {
        initialize(variablePsiElements);
    }

    // package-private constructor for tests; PSI-free setup
    StatementExecutionVariablesBundle(StatementExecutionVariable... variables) {
        this.variables = List.of(variables);
    }

    public void initialize(List<ExecVariablePsiElement> variablePsiElements) {
        List<StatementExecutionVariable> variables = new ArrayList<>();
        for (ExecVariablePsiElement variablePsiElement : variablePsiElements) {
            String variableName = variablePsiElement.getText();
            StatementExecutionVariable variable = getVariable(variableName);
            if (variable == null) {
                variable = new StatementExecutionVariable(variablePsiElement);
            } else {
                variable.setOffset(variablePsiElement.getTextOffset());
            }

            if (variable.getDataType() == null) {
                DBDataType dataType = lookupDataType(variablePsiElement);
                if (dataType != null && dataType.isNative()) {
                    variable.setDataType(dataType.getGenericDataType());
                } else {
                    GenericDataType variableDataType = cachedVariableDataType(variablePsiElement);
                    variable.setDataType(nvl(variableDataType, GenericDataType.LITERAL));
                }
            }
            uniqueAddVariable(variables, variable);
        }
        this.variables = variables;
    }

    public void cacheVariableDataTypes(ConnectionHandler connection) {
        StatementExecutionManager executionManager = StatementExecutionManager.getInstance(connection.getProject());
        StatementExecutionVariableTypes variableTypes = executionManager.getExecutionVariableTypes();
        for (StatementExecutionVariable variable : variables) {
            variableTypes.setVariableDataType(
                    connection.getConnectionId(),
                    variable.getName(),
                    variable.getDataType());
        }

    }

    @Nullable
    private GenericDataType cachedVariableDataType(ExecVariablePsiElement variablePsiElement) {
        String variableName = variablePsiElement.getText();
        ConnectionId connectionId = variablePsiElement.getConnectionId();
        if (connectionId == null) return null;

        StatementExecutionManager executionManager = StatementExecutionManager.getInstance(variablePsiElement.getProject());
        StatementExecutionVariableTypes executionVariableTypes = executionManager.getExecutionVariableTypes();
        return executionVariableTypes.getVariableDataType(connectionId, variableName);
    }

    private void uniqueAddVariable(List<StatementExecutionVariable> variables, StatementExecutionVariable variable) {
        if (Lists.noneMatch(variables, v -> Objects.equals(v.getName(), variable.getName()))) {
            variables.add(variable);
        }
    }

    public boolean isProvided() {
        for (StatementExecutionVariable variable : variables) {
            if (!variable.isProvided()) {
                return false;
            }
        }
        return true;
    }

    public void populate(Map<String, StatementExecutionVariable> variableCache, boolean force) {
        for (StatementExecutionVariable variable : variables) {
            if (!variable.isProvided() || force) {
                StatementExecutionVariable cacheVariable = variableCache.get(variable.getName());
                if (cacheVariable != null) {
                    variable.populate(cacheVariable);
                }
            }
        }
    }

    public boolean hasErrors() {
        return variables.stream().anyMatch(v -> v.hasError());
    }

    private static final Pattern VARIABLE_PATTERN = Pattern.compile(":([\\w$]+)");

    public String prepareExecutableStatementText(String statementText) {
        StringBuilder builder = new StringBuilder();
        List<StatementExecutionVariable> bindVariables = new ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(statementText);
        int cursor = 0;
        while (matcher.find()) {
            String variableName = matcher.group(1);
            StatementExecutionVariable variable = getVariable(variableName);
            if (variable == null) continue;

            builder.append(statementText, cursor, matcher.start()).append('?');
            bindVariables.add(variable);
            cursor = matcher.end();
        }
        builder.append(statementText, cursor, statementText.length());

        this.bindVariables = bindVariables;
        return builder.toString();
    }

    public void bindVariables(ConnectionHandler connection, PreparedStatement statement) throws SQLException {
        for (int i = 0; i < bindVariables.size(); i++) {
            Object value = bindVariables.get(i).getExecutionValue(connection);
            statement.setObject(i + 1, value);
        }
    }

    private static DBDataType lookupDataType(ExecVariablePsiElement variablePsiElement) {
        BasePsiElement conditionPsiElement = variablePsiElement.findEnclosingElement(ElementTypeAttribute.CONDITION);
        if (conditionPsiElement == null) return null;

        ObjectLookupAdapter lookupAdapter = new ObjectLookupAdapter(variablePsiElement, IdentifierCategory.REFERENCE, DBObjectType.COLUMN);
        BasePsiElement basePsiElement = lookupAdapter.findInScope(conditionPsiElement);
        if (basePsiElement instanceof IdentifierPsiElement columnPsiElement) {
            DBObject object = columnPsiElement.getUnderlyingObject();
            if (object instanceof DBColumn column) {
                return column.getDataType();
            }
        }
        return null;
    }

    @Nullable
    public StatementExecutionVariable getVariable(String name) {
        name = adjust(name);
        for (StatementExecutionVariable variable : variables) {
            if (Strings.equalsIgnoreCase(variable.getName(), name)) {
                return variable;
            }
        }
        return null;
    }

    public String preparePreviewStatementText(@NotNull ConnectionHandler connection, String statementText) {
        List<StatementExecutionVariable> variables = new ArrayList<>(this.variables);
        variables.sort(NAME_LENGTH_COMPARATOR);
        for (StatementExecutionVariable variable : variables) {
            String value = variable.getPreviewValue(connection);
            String name = ":" + variable.getName();
            if (isEmpty(value)) {
                statementText = replaceIgnoreCase(statementText, name, "NULL /*" + variable.getName() + "*/");
            } else {
                statementText = replaceIgnoreCase(statementText, name, value + " /*" + name + "*/");
            }
        }
        return statementText;
    }

    public void verifyExecutionVariables(@NotNull ConnectionHandler connection) {
        for (StatementExecutionVariable variable : this.variables) {
            variable.getExecutionValue(connection);
        }
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
