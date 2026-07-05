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

import com.dbn.common.locale.Formatter;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.state.ProtectedContent;
import com.dbn.common.state.ProtectedContents;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.type.GenericDataType;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.language.common.psi.ExecVariablePsiElement;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.execution.statement.variables.VariableNames.adjust;
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
public class StatementExecutionVariable extends VariableValueProvider implements Comparable<StatementExecutionVariable>, PersistentStateElement {
    private int offset;
    private String name;
    private GenericDataType dataType;
    private final ProtectedContents valueHistory = ProtectedContents.statementExecutionVariableValues();
    private VariableValueProvider previewValueProvider;

    private transient String error;

    public StatementExecutionVariable() {}

    public StatementExecutionVariable(StatementExecutionVariable source) {
        this.dataType = source.dataType;
        this.name = source.name;
        valueHistory.copyFrom(source.valueHistory);
    }

    public StatementExecutionVariable(ExecVariablePsiElement variablePsiElement) {
        this.name = adjust(variablePsiElement.getText());
        this.offset = variablePsiElement.getTextOffset();
    }

    @Override
    public String getValue() {
        return  previewValueProvider == null ?
                valueHistory.getValue() :
                previewValueProvider.getValue();
    }

    public GenericDataType getDataType() {
        return previewValueProvider == null ? dataType : previewValueProvider.getDataType();
    }

    public Object getExecutionValue(@NotNull ConnectionHandler connection) {
        error = null; // reset error
        String value = getValue();
        if (isEmpty(value)) return null;

        GenericDataType dataType = getDataType();
        if (dataType == GenericDataType.LITERAL) return value;

        Formatter formatter = Formatter.getInstance(connection.getProject());
        if (dataType == GenericDataType.DATE_TIME){
            try {
                Date date = formatter.parseDateTime(value);
                return new java.sql.Timestamp(date.getTime());
            } catch (ParseException e) {
                conditionallyLog(e);
                try {
                    Date date = formatter.parseDate(value);
                    return new java.sql.Date(date.getTime());
                } catch (ParseException e1) {
                    conditionallyLog(e1);
                    error = txt("msg.execution.error.InvalidDate");
                }
            }
            return null;
        }

        if (dataType == GenericDataType.NUMERIC){
            try {
                return formatter.parseNumber(value);
            } catch (ParseException e) {
                conditionallyLog(e);
                error = txt("msg.execution.error.InvalidNumber");
            }
            return null;
        }

        throw new IllegalArgumentException(txt("msg.execution.exception.VariableDataTypeUnsupported", this.dataType.getName()));
    }

    public String getPreviewValue(@NotNull ConnectionHandler connection) {
        error = null;
        DatabaseMetadataInterface metadataInterface = connection.getMetadataInterface();
        GenericDataType dataType = getDataType();
        String value = getValue();
        if (isEmpty(value)) return null;

        if (dataType == GenericDataType.LITERAL) {
            value = Strings.replace(value, "'", "''");
            return  '\'' + value + '\'';
        }

        Object executionValue = getExecutionValue(connection);
        if (executionValue == null) return null;

        if (dataType == GenericDataType.DATE_TIME){
            Date date = (Date) executionValue;
            return metadataInterface.createDateString(date);
        }

        return value;
    }

    public boolean hasError() {
        return error != null;
    }

    public void setValue(String value) {
        valueHistory.setValue(value);
    }

    public List<String> getValueHistory() {
        return valueHistory.values();
    }

    @NotNull
    public VariableValueProvider getPreviewValueProvider() {
        return previewValueProvider == null ? this : previewValueProvider;
    }

    public boolean isProvided() {
        return !valueHistory.isEmpty();
    }

    @Override
    public int compareTo(@NotNull StatementExecutionVariable o) {
        return name.compareTo(o.name);
    }

    @Override
    public void readState(Element element) {
        name = adjust(stringAttribute(element, "name"));
        dataType = enumAttribute(element, "data-type", GenericDataType.class);
        // TODO cleanup - attribute rename backward compatibility;
        if (dataType == null) enumAttribute(element, "dataType", GenericDataType.class);
        valueHistory.clear();

        for (Element child : element.getChildren()) {
            ProtectedContent value = valueHistory.newContent();
            value.readState(child);
            valueHistory.add(value);
        }
    }

    @Override
    public void writeState(Element element) {
        element.setAttribute("name", name);
        element.setAttribute("data-type", dataType.name());
        for (var value : valueHistory) {
            if (value.isEmpty()) continue;

            Element valueElement = newElement(element, "value");
            value.writeState(valueElement);
        }
    }

    public void populate(StatementExecutionVariable variable) {
        setValue(variable.getValue());
    }
}
