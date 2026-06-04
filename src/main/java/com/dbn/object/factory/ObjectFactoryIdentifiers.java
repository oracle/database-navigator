/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.object.factory;

import com.dbn.connection.ConnectionHandler;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.quotes.QuotePair;
import com.dbn.language.sql.SQLLanguage;
import lombok.experimental.UtilityClass;

import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.connection.DatabaseType.GENERIC;
import static com.dbn.language.common.quotes.QuoteEscaping.DATABASE;

@UtilityClass
public class ObjectFactoryIdentifiers {

    public static String quoteIdentifier(ConnectionHandler connection, String identifier) {
        QuotePair quotes = connection.getDatabaseType() == GENERIC ?
                connection.getCompatibility().getIdentifierQuotes() :
                connection.getCompatibilityInterface().getDefaultIdentifierQuotes();
        return quotes.quote(identifier, DATABASE);
    }

    public static boolean canUseDefaultCase(ConnectionHandler connection, String identifier) {
        if (!isUnquotedIdentifier(identifier)) return false;

        DBLanguageDialect languageDialect = connection.getLanguageDialect(SQLLanguage.INSTANCE);
        return languageDialect == null || !languageDialect.isReservedWord(identifier);
    }

    private static boolean isUnquotedIdentifier(String identifier) {
        if (isEmpty(identifier)) return false;

        char first = identifier.charAt(0);
        if (!Character.isLetter(first) && first != '_') return false;

        for (int i = 1; i < identifier.length(); i++) {
            char chr = identifier.charAt(i);
            if (!Character.isLetterOrDigit(chr) && chr != '_') return false;
        }
        return true;
    }
}
