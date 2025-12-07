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

package com.dbn.database.common.statement;

import com.dbn.common.util.TransientId;
import com.dbn.connection.jdbc.DBNCallableStatement;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NonNls
@Getter
public class StatementDefinition {
    private static final Pattern parameterPattern = Pattern.compile("\\{#(\\d+)}");
    private static final Pattern placeholderPattern = Pattern.compile("\\{(\\d+)}");

    private final String statementText;
    private final double sinceVersion;

    private final List<Integer> parameterIndices;
    private final List<Integer> placeholderIndices;

    private final TransientId id = TransientId.create();

    StatementDefinition(String statementText, String prefix, double sinceVersion) {
        this.sinceVersion = sinceVersion;
        this.parameterIndices = getDynamicContentIndices(statementText, parameterPattern);
        this.placeholderIndices = getDynamicContentIndices(statementText, placeholderPattern);

        statementText = statementText.trim();
        statementText = statementText.replaceAll("\\t", "    ");
        statementText = statementText.replaceAll("(?m)^ {12}", "");
        if (prefix != null) {
            statementText = statementText.replaceAll("\\[PREFIX]", prefix);
        }


        for (Integer parameterIndex : parameterIndices) {
            statementText = statementText.replaceAll("\\{#" + parameterIndex + "}", "?");
        }

        this.statementText = statementText;
    }

    private List<Integer> getDynamicContentIndices(String statementText, Pattern pattern) {
        ArrayList<Integer> indices = new ArrayList<>();
        Matcher matcher = pattern.matcher(statementText);
        while (matcher.find()) {
            String indexString = matcher.group(1);
            indices.add(Integer.parseInt(indexString));
        }
        return indices;
    }

    public boolean supports(double databaseVersion) {
        return databaseVersion >= sinceVersion;
    }


    DBNPreparedStatement<?> prepareStatement(DBNConnection connection, Object[] arguments) throws SQLException {
        String statementText = prepareStatementText(arguments);

        DBNPreparedStatement<?> preparedStatement = connection.prepareStatementCached(statementText);
        for (int i = 0; i < parameterIndices.size(); i++) {
            Integer argumentIndex = parameterIndices.get(i);
            Object argumentValue = arguments[argumentIndex];
            //todo this is just a workaround . i need to recheck it again .
            if (argumentValue instanceof InputStream) {
                preparedStatement.setBinaryStream(i + 1, (InputStream) argumentValue);
            } else {
                preparedStatement.setObject(i + 1, argumentValue);
            }
        }
        return preparedStatement;
    }

    DBNCallableStatement prepareCall(DBNConnection connection, Object[] arguments) throws SQLException {
        String statementText = prepareStatementText(arguments);

        DBNCallableStatement callableStatement = connection.prepareCallCached(statementText);
        for (int i = 0; i < parameterIndices.size(); i++) {
            Integer argumentIndex = parameterIndices.get(i);
            Object argumentValue = arguments[argumentIndex];
            callableStatement.setObject(i + 1, argumentValue);
        }
        return callableStatement;
    }

    String prepareStatementText(Object... arguments) {
        String statementText = this.statementText;
        for (Integer placeholderIndex : placeholderIndices) {
            String placeholderValue = Objects.toString(arguments[placeholderIndex]);
            placeholderValue = Matcher.quoteReplacement(placeholderValue);

            statementText = statementText.replaceAll("\\{" + placeholderIndex + "}", placeholderValue);
        }
        return statementText;
    }


    @Override
    public String toString() {
        return statementText;
    }

    public int getParameterCount() {
        return parameterIndices.size();
    }

    public int getPlaceholderCount() {
        return placeholderIndices.size();
    }
}
