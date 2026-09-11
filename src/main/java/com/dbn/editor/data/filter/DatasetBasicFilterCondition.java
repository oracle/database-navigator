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

package com.dbn.editor.data.filter;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.locale.Formatter;
import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.GenericDataType;
import com.dbn.database.common.statement.SqlLiterals;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.data.filter.ui.DatasetBasicFilterConditionForm;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DatasetBasicFilterCondition extends BasicConfiguration<DatasetBasicFilter, DatasetBasicFilterConditionForm> {
    private static final Pattern SAFE_DATE_EXPRESSION = Pattern.compile(
            "(?i)(?:current_date|current_time|current_timestamp|localtime|localtimestamp|sysdate|systimestamp|now\\(\\)|getdate\\(\\))");

    private String columnName = "";
    private ConditionOperator operator;
    private String value = "";
    private boolean active = true;

    public DatasetBasicFilterCondition(DatasetBasicFilter parent){
        super(parent);
    }

    public DatasetBasicFilterCondition(DatasetBasicFilter parent, String columnName, Object value, ConditionOperator operator, boolean active) {
        super(parent);
        this.columnName = columnName;
        this.operator = operator;
        this.value = value == null ? "" : value.toString();
        this.active = active;
    }

    public DatasetBasicFilterCondition(DatasetBasicFilter parent, String columnName, Object value, ConditionOperator operator) {
        super(parent);
        this.columnName = columnName;
        this.value = value == null ? "" : value.toString();
        this.operator = operator == null ? Strings.isEmpty(this.value) ? ConditionOperator.IS_NULL : ConditionOperator.EQUAL : operator;

        this.active = true;
    }

    public DatasetBasicFilter getFilter() {
        return getParent();
    }

    public void appendConditionString(StringBuilder buffer, DBDataset dataset) {
        DatasetBasicFilterConditionForm editorForm = getSettingsEditor();

        String columnName = Strings.nvle(this.columnName);
        ConditionOperator operator = this.operator == null ? ConditionOperator.EQUAL : this.operator;
        String value = this.value;

        if (isValid(editorForm)) {
            operator = editorForm.getSelectedOperator();
            DBColumn selectedColumn = editorForm.getSelectedColumn();
            if (selectedColumn != null) {
                columnName = selectedColumn.getName();
                value = editorForm.getValue();
            }
        }
        if (operator == null) operator = ConditionOperator.EQUAL;

        DBColumn column = dataset.getColumn(columnName);
        DBDataType dataType = column == null ? null : column.getDataType();
        if (column == null || dataType == null || !dataType.isNative()) {
            buffer.append("1 = 0");
            return;
        }

        GenericDataType genericDataType = dataType.getGenericDataType();
        String renderedValue = operator.isTerminal() ? "" : renderValue(operator, value, genericDataType, dataset);
        if (renderedValue == null) {
            buffer.append("1 = 0");
            return;
        }

        buffer.append(column.getName(true));
        buffer.append(" ");
        buffer.append(operator.getText());
        if (!renderedValue.isEmpty()) {
            buffer.append(" ");
            buffer.append(renderedValue);
        }
    }

    private String renderValue(ConditionOperator operator, String value, GenericDataType genericDataType, DBDataset dataset) {
        if (operator == ConditionOperator.IN || operator == ConditionOperator.NOT_IN) {
            StringTokenizer tokenizer = new StringTokenizer(Strings.nvle(value), ",");
            StringBuilder valueBuilder = new StringBuilder("(");
            while (tokenizer.hasMoreTokens()) {
                String renderedValue = renderScalarValue(tokenizer.nextToken().trim(), genericDataType, dataset);
                if (renderedValue == null) return null;
                if (valueBuilder.length() > 1) valueBuilder.append(", ");
                valueBuilder.append(renderedValue);
            }
            if (valueBuilder.length() == 1) valueBuilder.append("NULL");
            return valueBuilder.append(")").toString();
        }

        return renderScalarValue(value, genericDataType, dataset);
    }

    private String renderScalarValue(String value, GenericDataType genericDataType, DBDataset dataset) {
        String trimmedValue = Strings.nvle(value).trim();
        if (genericDataType == GenericDataType.NUMERIC) {
            return renderNumericValue(trimmedValue, Formatter.getInstance(dataset.getProject()));
        }

        if (genericDataType == GenericDataType.DATE_TIME) {
            return renderDateValue(trimmedValue, dataset);
        }

        if (genericDataType == GenericDataType.BOOLEAN) {
            if (trimmedValue.equalsIgnoreCase("true")) return "TRUE";
            if (trimmedValue.equalsIgnoreCase("false")) return "FALSE";
            return null;
        }

        return quoteValue(trimmedValue);
    }

    static String renderNumericValue(String value, Formatter formatter) {
        if (value.isEmpty()) return "NULL";

        ParsePosition position = new ParsePosition(0);
        Number number = formatter.getNumberFormat().parse(value, position);
        if (number == null || position.getErrorIndex() >= 0 || position.getIndex() != value.length()) {
            return null;
        }

        try {
            return SqlLiterals.renderLiteral(number);
        } catch (IllegalArgumentException e) {
            conditionallyLog(e);
            return null;
        }
    }

    private String renderDateValue(String value, DBDataset dataset) {
        if (value.isEmpty()) return "NULL";

        ConnectionHandler connection = Failsafe.nn(dataset.getConnection());
        DatabaseMetadataInterface metadata = connection.getMetadataInterface();
        Formatter formatter = Formatter.getInstance(dataset.getProject());
        try {
            Date date = formatter.parseDateTime(value);
            return metadata.createDateString(date);
        } catch (ParseException e) {
            conditionallyLog(e);
            try {
                Date date = formatter.parseDate(value);
                return metadata.createDateString(date);
            } catch (ParseException e1) {
                conditionallyLog(e1);
                return SAFE_DATE_EXPRESSION.matcher(value).matches() ? value : null;
            }
        }
    }

    @NotNull
    private String quoteValue(String value) {
        return SqlLiterals.renderLiteral(value);
    }

    /****************************************************
    *                   Configuration                  *
    ****************************************************/
    @Override
    @NotNull
    public DatasetBasicFilterConditionForm createConfigurationEditor() {
        DBDataset dataset = getFilter().lookupDataset();
        return new DatasetBasicFilterConditionForm(dataset, this);
    }

    @Override
    public void readConfiguration(Element element) {
       columnName = stringAttribute(element, "column");
       operator = ConditionOperator.get(stringAttribute(element, "operator"));
       if (operator == null) operator = ConditionOperator.EQUAL;
       value = element.getAttributeValue("value");
       active = booleanAttribute(element, "active", true);
    }

    @Override
    public void writeConfiguration(Element element) {
        element.setAttribute("column", columnName);
        element.setAttribute("operator", operator.getText());
        element.setAttribute("value", value);
        element.setAttribute("active", Boolean.toString(active));
    }
}
