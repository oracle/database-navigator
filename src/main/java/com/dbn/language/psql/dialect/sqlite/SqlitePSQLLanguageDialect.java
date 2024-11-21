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

package com.dbn.language.psql.dialect.sqlite;

import com.dbn.language.common.DBLanguageDialectIdentifier;
import com.dbn.language.common.DBLanguageSyntaxHighlighter;
import com.dbn.language.psql.dialect.PSQLLanguageDialect;
import com.dbn.language.sql.dialect.sqlite.SqliteSQLParserDefinition;

public class SqlitePSQLLanguageDialect extends PSQLLanguageDialect {

    public SqlitePSQLLanguageDialect() {
        super(DBLanguageDialectIdentifier.SQLITE_PSQL);
    }

    @Override
    protected DBLanguageSyntaxHighlighter createSyntaxHighlighter() {
        return new SqlitePSQLHighlighter(this);
    }

    @Override
    protected SqliteSQLParserDefinition createParserDefinition() {
        SqlitePSQLParser parser = new SqlitePSQLParser(this);
        return new SqliteSQLParserDefinition(parser);
    }
}