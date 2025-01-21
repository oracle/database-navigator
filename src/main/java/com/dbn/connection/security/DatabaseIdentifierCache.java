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

package com.dbn.connection.security;

import com.dbn.common.collections.ConcurrentStringInternMap;
import com.dbn.connection.ConnectionComponentBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.language.common.QuotePair;

import java.util.function.Function;

/**
 * The DatabaseIdentifierCache class provides functionality to monitor and manage
 * quoted identifiers within a database context. It maintains a thread-safe map
 * of identifiers to their respective quoted versions. This is useful for ensuring
 * consistent and secure handling of database identifiers by applying a quoting mechanism.
 *
 * @author Dan Cioca (Oracle)
 */
public class DatabaseIdentifierCache extends ConnectionComponentBase {
    private final ConcurrentStringInternMap quotedIdentifiers = new ConcurrentStringInternMap();

    public DatabaseIdentifierCache(ConnectionHandler connection) {
        super(connection);
    }

    /**
     * Retrieves the quoted representation of the provided database identifier.
     * If a quoted version of the identifier is already cached, it returns the cached value.
     * Otherwise, it generates a quoted identifier using the appropriate quoting mechanism.
     *
     * @param identifier The database identifier whose quoted version is being requested.
     * @return The quoted version of the provided database identifier.
     */
    public String getQuotedIdentifier(String identifier) {
        String quotedIdentifier = quotedIdentifiers.get(identifier);
        if (quotedIdentifier == null) {
            return quoteIdentifier(identifier);
        }

        return quotedIdentifier;
    }

    /**
     * Registers a database identifier along with its quoting function in the cache.
     * If the identifier is not already present in the cache, the provided quoting function
     * is applied to compute the quoted version, which is then stored for future retrieval.
     *
     * @param identifier        The database identifier to be registered.
     * @param identifierQuoter  A function that takes the identifier as input and returns its quoted version.
     */
    public void registerIdentifier(String identifier, Function<String, String> identifierQuoter) {
        quotedIdentifiers.computeIfAbsent(identifier, identifierQuoter);
    }

    /**
     * Quotes a given database identifier using the appropriate quoting mechanism based on the database type.
     * If the database type is generic, it uses the identifier quote character provided by the connection.
     * Otherwise, it uses the default identifier quotes defined by the database compatibility interface.
     *
     * @param identifier The database identifier that needs to be quoted.
     * @return A quoted version of the provided identifier, ensuring safe usage in database operations.
     */
    private String quoteIdentifier(String identifier) {
        ConnectionHandler connection = this.getConnection();
        ConnectionDatabaseSettings databaseSettings = connection.getSettings().getDatabaseSettings();
        if (databaseSettings.getDatabaseType() == DatabaseType.GENERIC) {
            String identifierQuotes = connection.getCompatibility().getIdentifierQuote();
            return identifierQuotes + identifier + identifierQuotes;
        } else {
            DatabaseCompatibilityInterface compatibility = connection.getCompatibilityInterface();
            QuotePair quotes = compatibility.getDefaultIdentifierQuotes();
            return quotes.quote(identifier);
        }
    }

    @Override
    public void disposeInner() {
        quotedIdentifiers.clear();
    }
}
