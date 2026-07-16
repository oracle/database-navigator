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

package com.dbn.database.common.statement;

import com.dbn.language.common.quotes.QuoteDefinition;
import com.dbn.language.common.quotes.QuotePair;
import org.junit.Test;

import java.sql.SQLException;

import static com.dbn.language.common.quotes.QuoteEscaping.DATABASE;
import static org.junit.Assert.assertEquals;

public class StatementDefinitionTest {
    private static final QuoteDefinition SQLITE_QUOTE_DEFINITION = new QuoteDefinition(
            new QuotePair('"', '"'),
            new QuotePair('[', ']'),
            new QuotePair('`', '`'));
    private static final QuotePair SQLITE_QUOTES = new QuotePair('"', '"');

    @Test
    public void prepareStatementTextQuotesIdentifierPlaceholders() throws SQLException {
        StatementDefinition definition = new StatementDefinition(
                "select distinct {@2} from {@0}.{@1} where {@2} is not null order by {@2}",
                null,
                null,
                0.0);

        String statementText = definition.prepareStatementText(
                StatementDefinitionTest::enquoteSqliteIdentifier,
                "ma\"in;--",
                "ta\"ble /*comment*/",
                "col\uFF02umn");

        assertEquals(
                "select distinct \"col\uFF02umn\" from \"ma\"\"in;--\".\"ta\"\"ble /*comment*/\" where \"col\uFF02umn\" is not null order by \"col\uFF02umn\"",
                statementText);
    }

    @Test
    public void prepareStatementTextDoesNotDoubleQuoteAlreadyQuotedIdentifierValues() throws SQLException {
        StatementDefinition definition = new StatementDefinition(
                "PRAGMA {@0}.TABLE_INFO({@1})",
                null,
                null,
                0.0);

        String statementText = definition.prepareStatementText(
                StatementDefinitionTest::enquoteSqliteIdentifier,
                "\"main\"",
                "`dataset`");

        assertEquals(
                "PRAGMA \"main\".TABLE_INFO(`dataset`)",
                statementText);
    }

    @Test
    public void prepareStatementTextKeepsValuePlaceholdersParameterized() throws SQLException {
        StatementDefinition definition = new StatementDefinition(
                "select SQL from {@0}.SQLITE_MASTER where TBL_NAME = {#1}",
                null,
                null,
                0.0);

        String statementText = definition.prepareStatementText(
                StatementDefinitionTest::enquoteSqliteIdentifier,
                "main\";--",
                "dataset' or 1=1 --");

        assertEquals(
                "select SQL from \"main\"\";--\".SQLITE_MASTER where TBL_NAME = ?",
                statementText);
    }

    @Test
    public void prepareStatementTextAllowsControlledRawFragments() throws SQLException {
        StatementDefinition definition = new StatementDefinition(
                "drop {0} if exists {@1}.{@2}",
                null,
                null,
                0.0);

        String statementText = definition.prepareStatementText(
                StatementDefinitionTest::enquoteSqliteIdentifier,
                "table",
                "main\";--",
                "dataset");

        assertEquals(
                "drop table if exists \"main\"\";--\".\"dataset\"",
                statementText);
    }

    @Test
    public void prepareStatementTextQuotesJavaDdlIdentifierPlaceholders() throws SQLException {
        StatementDefinition definition = new StatementDefinition(
                "alter JAVA SOURCE {@0}.{@1} compile",
                null,
                null,
                0.0);

        String statementText = definition.prepareStatementText(
                StatementDefinitionTest::enquoteSqliteIdentifier,
                "owner\";--",
                "source name");

        assertEquals(
                "alter JAVA SOURCE \"owner\"\";--\".\"source name\" compile",
                statementText);
    }

    @Test
    public void prepareStatementTextRendersTypedValueLiterals() throws SQLException {
        StatementDefinition definition = new StatementDefinition(
                "BEGIN MY_PKG.RUN(name => {$0}, threshold => {$1}, run_date => {$2}, target => {@3}); END;",
                null,
                null,
                0.0);

        String statementText = definition.prepareStatementText(
                StatementDefinitionTest::enquoteSqliteIdentifier,
                "O'Brien'; DROP TABLE users; --",
                0.75,
                java.time.LocalDate.of(2026, 7, 9),
                "ML_TRAIN");

        assertEquals(
                "BEGIN MY_PKG.RUN(name => 'O''Brien''; DROP TABLE users; --', threshold => 0.75, run_date => DATE '2026-07-09', target => \"ML_TRAIN\"); END;",
                statementText);
    }

    @Test
    public void prepareStatementTextRendersNullLiterals() throws SQLException {
        StatementDefinition definition = new StatementDefinition(
                "BEGIN MY_PKG.RUN(comment_text => {$0}); END;",
                null,
                null,
                0.0);

        String statementText = definition.prepareStatementText(
                StatementDefinitionTest::enquoteSqliteIdentifier,
                (Object) null);

        assertEquals("BEGIN MY_PKG.RUN(comment_text => NULL); END;", statementText);
    }

    private static String enquoteSqliteIdentifier(String identifier) {
        if (SQLITE_QUOTE_DEFINITION.isQuoted(identifier)) return identifier;
        return SQLITE_QUOTES.quote(identifier, DATABASE);
    }
}
