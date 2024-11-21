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

import com.dbn.common.util.Strings;
import com.dbn.data.sorting.SortDirection;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseCompatibilityInterfaceImpl;
import com.dbn.editor.session.SessionStatus;
import com.dbn.language.common.QuoteDefinition;
import com.dbn.language.common.QuotePair;

import java.util.Arrays;
import java.util.List;

import static com.dbn.database.DatabaseFeature.CONSTRAINT_MANIPULATION;
import static com.dbn.database.DatabaseFeature.CURRENT_SCHEMA;
import static com.dbn.database.DatabaseFeature.OBJECT_CHANGE_MONITORING;
import static com.dbn.database.DatabaseFeature.OBJECT_SOURCE_EDITING;
import static com.dbn.database.DatabaseFeature.READONLY_CONNECTIVITY;
import static com.dbn.database.DatabaseFeature.SESSION_BROWSING;
import static com.dbn.database.DatabaseFeature.SESSION_KILL;
import static com.dbn.database.DatabaseFeature.UPDATABLE_RESULT_SETS;

public class MySqlCompatibilityInterface extends DatabaseCompatibilityInterfaceImpl {
    private static final QuoteDefinition IDENTIFIER_QUOTE_DEFINITION = new QuoteDefinition(new QuotePair('`', '`'));

    @Override
    public List<DatabaseObjectTypeId> getSupportedObjectTypes() {
        return Arrays.asList(
                DatabaseObjectTypeId.CONSOLE,
                DatabaseObjectTypeId.CHARSET,
                DatabaseObjectTypeId.USER,
                DatabaseObjectTypeId.SCHEMA,
                DatabaseObjectTypeId.TABLE,
                DatabaseObjectTypeId.VIEW,
                DatabaseObjectTypeId.COLUMN,
                DatabaseObjectTypeId.CONSTRAINT,
                DatabaseObjectTypeId.INDEX,
                DatabaseObjectTypeId.DATASET_TRIGGER,
                DatabaseObjectTypeId.FUNCTION,
                DatabaseObjectTypeId.PROCEDURE,
                DatabaseObjectTypeId.ARGUMENT,
                DatabaseObjectTypeId.SYSTEM_PRIVILEGE,
                DatabaseObjectTypeId.GRANTED_PRIVILEGE);
    }

    @Override
    public List<DatabaseFeature> getSupportedFeatures() {
        return Arrays.asList(
                SESSION_BROWSING,
                SESSION_KILL,
                OBJECT_CHANGE_MONITORING,
                OBJECT_SOURCE_EDITING,
                UPDATABLE_RESULT_SETS,
                CURRENT_SCHEMA,
                CONSTRAINT_MANIPULATION,
                READONLY_CONNECTIVITY);
    }

    @Override
    public QuoteDefinition getIdentifierQuotes() {
        return IDENTIFIER_QUOTE_DEFINITION;
    }

    @Override
    public String getDefaultAlternativeStatementDelimiter() {
        return "$$";
    }

    @Override
    public String getOrderByClause(String columnName, SortDirection sortDirection, boolean nullsFirst) {
        nullsFirst = (nullsFirst && sortDirection == SortDirection.ASCENDING) || (!nullsFirst && sortDirection == SortDirection.DESCENDING);
        return "(" + columnName + " is" + (nullsFirst ? "" : " not") + " null), " + columnName + " " + sortDirection.getSqlToken();
    }

    @Override
    public String getExplainPlanStatementPrefix() {
        return null;
    }

    @Override
    public String getSessionBrowserColumnName(String columnName) {
        if (columnName.equalsIgnoreCase("id")) return "SESSION_ID";
        if (columnName.equalsIgnoreCase("user")) return "USER";
        if (columnName.equalsIgnoreCase("host")) return "HOST";
        if (columnName.equalsIgnoreCase("db")) return "DATABASE";
        if (columnName.equalsIgnoreCase("command")) return "COMMAND";
        if (columnName.equalsIgnoreCase("time")) return "TIME";
        if (columnName.equalsIgnoreCase("state")) return "STATUS";
        if (columnName.equalsIgnoreCase("info")) return "CLIENT_INFO";
        return super.getSessionBrowserColumnName(columnName);
    }

    @Override
    public SessionStatus getSessionStatus(String statusName) {
        if (Strings.isEmpty(statusName)) return SessionStatus.INACTIVE;
        else return SessionStatus.ACTIVE;
    }
}
