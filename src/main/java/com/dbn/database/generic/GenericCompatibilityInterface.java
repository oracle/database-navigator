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

package com.dbn.database.generic;

import com.dbn.common.util.Strings;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseCompatibilityInterfaceImpl;
import com.dbn.editor.session.SessionStatus;
import com.dbn.language.common.QuoteDefinition;
import com.dbn.language.common.QuotePair;

import java.util.Arrays;
import java.util.List;

import static com.dbn.database.DatabaseFeature.CONNECTION_ERROR_RECOVERY;
import static com.dbn.database.DatabaseFeature.CONSTRAINT_MANIPULATION;
import static com.dbn.database.DatabaseFeature.CURRENT_SCHEMA;
import static com.dbn.database.DatabaseFeature.OBJECT_CHANGE_MONITORING;
import static com.dbn.database.DatabaseFeature.OBJECT_SOURCE_EDITING;
import static com.dbn.database.DatabaseFeature.READONLY_CONNECTIVITY;
import static com.dbn.database.DatabaseFeature.SESSION_CURRENT_SQL;
import static com.dbn.database.DatabaseFeature.UPDATABLE_RESULT_SETS;

public class GenericCompatibilityInterface extends DatabaseCompatibilityInterfaceImpl {
    private static final QuoteDefinition IDENTIFIER_QUOTE_DEFINITION = new QuoteDefinition(new QuotePair('"', '"'));

    @Override
    public List<DatabaseFeature> getSupportedFeatures() {
        return Arrays.asList(
                OBJECT_SOURCE_EDITING,
                OBJECT_CHANGE_MONITORING,
                SESSION_CURRENT_SQL,
                CONNECTION_ERROR_RECOVERY,
                UPDATABLE_RESULT_SETS,
                CURRENT_SCHEMA,
                CONSTRAINT_MANIPULATION,
                READONLY_CONNECTIVITY);
    }

    @Override
    public List<DatabaseObjectTypeId> getSupportedObjectTypes() {
        return Arrays.asList(
                DatabaseObjectTypeId.CONSOLE,
                DatabaseObjectTypeId.SCHEMA,
                DatabaseObjectTypeId.TABLE,
                DatabaseObjectTypeId.VIEW,
                DatabaseObjectTypeId.COLUMN,
                DatabaseObjectTypeId.CONSTRAINT,
                DatabaseObjectTypeId.INDEX,
                DatabaseObjectTypeId.TRIGGER,
                DatabaseObjectTypeId.FUNCTION,
                DatabaseObjectTypeId.PROCEDURE,
                DatabaseObjectTypeId.ARGUMENT);
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
    public SessionStatus getSessionStatus(String statusName) {
        return Strings.isEmpty(statusName) ? SessionStatus.INACTIVE : SessionStatus.ACTIVE;
    }

    @Override
    public String getExplainPlanStatementPrefix() {
        return null;
    }
}
