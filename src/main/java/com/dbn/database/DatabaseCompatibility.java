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

package com.dbn.database;

import com.dbn.common.property.PropertyHolderBase;
import com.dbn.common.util.TransientId;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseCompatibility extends PropertyHolderBase.IntStore<JdbcProperty> {

    private String identifierQuote;
    private final Map<TransientId, DatabaseActivityTrace> activityTraces = new ConcurrentHashMap<>();

    private DatabaseCompatibility() {}

    public static DatabaseCompatibility allFeatures() {
        DatabaseCompatibility compatibility = new DatabaseCompatibility();
        // mark all features as supported
        for (JdbcProperty property : JdbcProperty.values()) {
            if (property.isFeature()) {
                compatibility.set(property, true);
            }
        }
        return compatibility;
    }

    public static DatabaseCompatibility noFeatures() {
        return new DatabaseCompatibility();
    }

    public void markUnsupported(JdbcProperty feature) {
        set(feature, false);
    }

    public boolean isSupported(JdbcProperty feature) {
        return is(feature);
    };

    public DatabaseActivityTrace getActivityTrace(TransientId operationId) {
        return activityTraces.computeIfAbsent(operationId, id -> new DatabaseActivityTrace());
    }

    public String getIdentifierQuote() {
        return identifierQuote;
    }

    @Override
    protected JdbcProperty[] properties() {
        return JdbcProperty.VALUES;
    }

    public void read(DatabaseMetaData metaData) throws SQLException {
        String quoteString = metaData.getIdentifierQuoteString();
        identifierQuote = quoteString == null ? "" : quoteString.trim();

        //TODO JdbcProperty.SQL_DATASET_ALIASING (identify by database type?)
    }
}
