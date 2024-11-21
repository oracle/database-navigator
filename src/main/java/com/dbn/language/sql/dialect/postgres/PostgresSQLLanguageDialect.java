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

package com.dbn.language.sql.dialect.postgres;

import com.dbn.language.common.ChameleonTokenType;
import com.dbn.language.common.DBLanguageDialectIdentifier;
import com.dbn.language.common.DBLanguageSyntaxHighlighter;
import com.dbn.language.sql.dialect.SQLLanguageDialect;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class PostgresSQLLanguageDialect extends SQLLanguageDialect {
    public PostgresSQLLanguageDialect() {
        super(DBLanguageDialectIdentifier.POSTGRES_SQL);
    }

    @Override
    protected Set<ChameleonTokenType> createChameleonTokenTypes() {
        return null;
    }

    @Override
    protected DBLanguageSyntaxHighlighter createSyntaxHighlighter() {
        return new PostgresSQLHighlighter(this);
}

    @Override
    protected PostgresSQLParserDefinition createParserDefinition() {
        PostgresSQLParser parser = new PostgresSQLParser(this);
        return new PostgresSQLParserDefinition(parser);
    }

    @Nullable
    @Override
    protected DBLanguageDialectIdentifier getChameleonDialectIdentifier() {
        return DBLanguageDialectIdentifier.POSTGRES_PSQL;
    }
}