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

package com.dbn.database.postgres;

import com.dbn.common.util.Strings;
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
import static com.dbn.database.DatabaseFeature.OBJECT_SOURCE_EDITING;
import static com.dbn.database.DatabaseFeature.READONLY_CONNECTIVITY;
import static com.dbn.database.DatabaseFeature.SESSION_BROWSING;
import static com.dbn.database.DatabaseFeature.SESSION_CURRENT_SQL;
import static com.dbn.database.DatabaseFeature.SESSION_KILL;
import static com.dbn.database.DatabaseFeature.UPDATABLE_RESULT_SETS;

public class PostgresCompatibilityInterface extends DatabaseCompatibilityInterfaceImpl {

    public static final QuoteDefinition IDENTIFIER_QUOTE_DEFINITION = new QuoteDefinition(new QuotePair('"', '"'));

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
                //DATABASE_TRIGGER,
                DatabaseObjectTypeId.FUNCTION,
                DatabaseObjectTypeId.ARGUMENT,
                DatabaseObjectTypeId.SEQUENCE,
                DatabaseObjectTypeId.SYSTEM_PRIVILEGE,
                DatabaseObjectTypeId.GRANTED_PRIVILEGE);
    }

    @Override
    public List<DatabaseFeature> getSupportedFeatures() {
        return Arrays.asList(
                SESSION_BROWSING,
                SESSION_KILL,
                SESSION_CURRENT_SQL,
                UPDATABLE_RESULT_SETS,
                OBJECT_SOURCE_EDITING,
                CURRENT_SCHEMA,
                CONSTRAINT_MANIPULATION,
                READONLY_CONNECTIVITY);
    }

    @Override
    public SessionStatus getSessionStatus(String statusName) {
        if (Strings.isEmpty(statusName)) return SessionStatus.INACTIVE;
        if (statusName.equalsIgnoreCase("active")) return SessionStatus.ACTIVE;
        if (statusName.equalsIgnoreCase("idle")) return SessionStatus.INACTIVE;
        return SessionStatus.SNIPED;
    }

    @Override
    public QuoteDefinition getIdentifierQuotes() {
        return IDENTIFIER_QUOTE_DEFINITION;
    }

    @Override
    public String getDefaultAlternativeStatementDelimiter() {
        return null;
    }

    @Override
    public String getExplainPlanStatementPrefix() {
        return "explain analyze verbose ";
    }
}
