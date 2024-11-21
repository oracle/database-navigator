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

package com.dbn.database.mysql;

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.util.Strings;
import com.dbn.common.util.WordTokenizer;
import com.dbn.connection.Resources;
import com.dbn.database.common.util.ResultSetStub;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.util.Strings.toUpperCase;

public class MySqlArgumentsResultSet extends StatefulDisposableBase implements ResultSetStub {
    private static class Argument {
        private String name;
        private String programName;
        private String methodName;
        private String methodType;
        private short overload;
        private short position;
        private short sequence;
        private String inOut = "IN";
        private String dataTypeOwner;
        private String dataTypePackage;
        private String dataTypeName;
        private int dataLength;
        private Integer dataPrecision;
        private Integer dataScale;
    }
    private final Iterator<Argument> arguments;
    private Argument currentArgument;

    MySqlArgumentsResultSet(ResultSet resultSet) throws SQLException {
        try {
            List<Argument> argumentList = new ArrayList<>();
            while (resultSet.next()) {
                String argumentsString = resultSet.getString("ARGUMENTS");
                WordTokenizer wordTokenizer = new WordTokenizer(argumentsString);

                String methodName = resultSet.getString("METHOD_NAME");
                String methodType = resultSet.getString("METHOD_TYPE");
                boolean betweenBrackets = false;
                boolean typePostfixSet = false;
                short argumentPosition = (short) (Objects.equals(methodType, "FUNCTION") ? 0 : 1);

                Argument argument = null;

                for (String token : wordTokenizer.getTokens()) {
                    if (argument == null) {
                        typePostfixSet = false;
                        argument = new Argument();
                        argument.methodName = methodName;
                        argument.methodType = methodType;
                        argument.position = argumentPosition;

                        argumentList.add(argument);
                        argumentPosition++;
                    }

                    // hit IN OUT or INOUT token and name is not set
                    if ((token.equalsIgnoreCase("IN") || token.equalsIgnoreCase("OUT") || token.equalsIgnoreCase("INOUT"))) {
                        if (argument.name != null) throwParseException(argumentsString, token, "Argument name should not be set.");
                        argument.inOut = toUpperCase(token);
                        continue;
                    }

                    // found open bracket => set betweenBrackets flag
                    if (Objects.equals(token, "(")) {
                        if (betweenBrackets) throwParseException(argumentsString, token, "Bracket already opened.");
                        if (argument.dataTypeName == null) throwParseException(argumentsString, token, "Data type not set yet.");
                        betweenBrackets = true;
                        continue;
                    }

                    // found close bracket => reset betweenBrackets flag
                    if (Objects.equals(token, ")")) {
                        if (!betweenBrackets) throwParseException(argumentsString, token, "No opened bracket.");
                        if (argument.dataPrecision == null && argument.dataScale == null) throwParseException(argumentsString, token, "Data precision and scale are not set yet.");
                        betweenBrackets = false;
                        continue;
                    }

                    // found comma token
                    if (Objects.equals(token, ",")) {
                        if (betweenBrackets) {
                            // between brackets
                            if (argument.dataPrecision == null) throwParseException(argumentsString, token, "Data precision is not set yet.");
                            continue;
                        } else {
                            // not between brackets => new argument
                            if (argument.name == null) throwParseException(argumentsString, token, "Argument name not set yet.");
                            if (argument.dataTypeName == null) throwParseException(argumentsString, token, "Data type not set yet.");
                            argument = null;
                            continue;
                        }
                    }

                    // number token
                    if (Strings.isInteger(token)) {
                        if (!betweenBrackets) throwParseException(argumentsString, token, "No bracket opened.");
                        if (argument.name == null) throwParseException(argumentsString, token, "Argument name not set yet.");
                        if (argument.dataTypeName == null) throwParseException(argumentsString, token, "Data type not set yet.");

                        // if precision not set then set it
                        if (argument.dataPrecision == null) {
                            argument.dataPrecision = Integer.valueOf(token);
                            continue;
                        }
                        // if scale not set then set it
                        if (argument.dataScale == null) {
                            argument.dataScale = Integer.valueOf(token);
                            continue;
                        }
                        throwParseException(argumentsString, token);
                    }

                    // if none of the conditions above are met
                    if (argument.name == null) {
                        argument.name = token;
                        continue;
                    }

                    if (argument.dataTypeName == null) {
                        argument.dataTypeName = token;
                        continue;
                    }

                    if (!typePostfixSet) {
                        typePostfixSet = true;
                        continue;
                    }

                    throwParseException(argumentsString, token);
                }
            }

            arguments = argumentList.iterator();

        } finally {
            Resources.close(resultSet);
        }

    }

    private static void throwParseException(String argumentsString, String token) throws SQLException {
        throw new SQLException("Could not parse argument actions \"" + argumentsString + "\". Unexpected token \"" + token + "\" found.");
    }

    private static void throwParseException(String argumentsString, String token, String customMessage) throws SQLException {
        throw new SQLException("Could not parse argument actions \"" + argumentsString + "\". Unexpected token \"" + token + "\" found. " + customMessage);
    }

    @Override
    public boolean next() throws SQLException {
        currentArgument = arguments.hasNext() ? arguments.next() : null;
        return currentArgument != null;
    }

    @Override
    public void close() throws SQLException {
        // nothing to close
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return
            Objects.equals(columnLabel, "ARGUMENT_NAME") ? currentArgument.name :
            Objects.equals(columnLabel, "METHOD_NAME") ? currentArgument.methodName :
            Objects.equals(columnLabel, "METHOD_TYPE") ? currentArgument.methodType :
            Objects.equals(columnLabel, "IN_OUT") ? currentArgument.inOut :
            Objects.equals(columnLabel, "DATA_TYPE_NAME") ? currentArgument.dataTypeName : null;
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        return
            Objects.equals(columnLabel, "POSITION") ? currentArgument.position :
            Objects.equals(columnLabel, "SEQUENCE") ? currentArgument.position :
            Objects.equals(columnLabel, "DATA_PRECISION") ? (currentArgument.dataPrecision == null ? 0 : currentArgument.dataPrecision) :
            Objects.equals(columnLabel, "DATA_SCALE") ? (currentArgument.dataScale == null ? 0 : currentArgument.dataScale) : 0;
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return
                Objects.equals(columnLabel, "POSITION") ? currentArgument.position :
                (short) (Objects.equals(columnLabel, "SEQUENCE") ? currentArgument.position :
                            Objects.equals(columnLabel, "DATA_PRECISION") ? (currentArgument.dataPrecision == null ? 0 : currentArgument.dataPrecision) :
                            Objects.equals(columnLabel, "DATA_SCALE") ? (currentArgument.dataScale == null ? 0 : currentArgument.dataScale) : 0);
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return getInt(columnLabel);
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
