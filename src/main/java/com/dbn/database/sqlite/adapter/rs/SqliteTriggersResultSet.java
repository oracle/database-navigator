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

package com.dbn.database.sqlite.adapter.rs;

import com.dbn.common.util.Strings;
import com.dbn.database.common.util.WrappedResultSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqliteTriggersResultSet extends WrappedResultSet {
    private static final Pattern TRIGGER_EVENT_REGEX = Pattern.compile("(before|after|instead\\s+of)\\s+(delete|insert|update)", Pattern.CASE_INSENSITIVE);

    public SqliteTriggersResultSet(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        boolean isType = Objects.equals(columnLabel, "TRIGGER_TYPE");
        boolean isEvent = Objects.equals(columnLabel, "TRIGGERING_EVENT");
        if (isType || isEvent) {
            String sourceCode = inner.getString("SOURCE_CODE");

            Matcher m = TRIGGER_EVENT_REGEX.matcher(sourceCode);
            if (m.find()) {
                int start = m.start();
                int end = m.end();
                String definition = sourceCode.substring(start, end);
                if (isType) {
                    return
                        Strings.containsIgnoreCase(definition, "before") ? "BEFORE" :
                        Strings.containsIgnoreCase(definition, "after") ? "AFTER" :
                        Strings.containsIgnoreCase(definition, "instead") ? "INSTEAD OF" : "";
                }
                if (isEvent) {
                    return
                        Strings.containsIgnoreCase(definition, "delete") ? "DELETE" :
                        Strings.containsIgnoreCase(definition, "insert") ? "INSERT" :
                        Strings.containsIgnoreCase(definition, "update") ? "UPDATE" : "";

                }
            }
        }
        return inner.getString(columnLabel);
    }
}
