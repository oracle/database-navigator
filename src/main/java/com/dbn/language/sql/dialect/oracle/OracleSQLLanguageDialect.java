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

package com.dbn.language.sql.dialect.oracle;

import com.dbn.language.common.ChameleonTokenType;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguageDialectIdentifier;
import com.dbn.language.common.DBLanguageSyntaxHighlighter;
import com.dbn.language.common.element.TokenPairTemplate;
import com.dbn.language.sql.dialect.SQLLanguageDialect;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class OracleSQLLanguageDialect extends SQLLanguageDialect {
    private static final TokenPairTemplate[] TOKEN_PAIR_TEMPLATES = new TokenPairTemplate[] {
            TokenPairTemplate.PARENTHESES,
            TokenPairTemplate.BEGIN_END};

    public OracleSQLLanguageDialect() {
        super(DBLanguageDialectIdentifier.ORACLE_SQL);
    }


    @Override
    protected Set<ChameleonTokenType> createChameleonTokenTypes() {
        Set<ChameleonTokenType> tokenTypes = new HashSet<>();
        DBLanguageDialect plsql = DBLanguageDialect.get(DBLanguageDialectIdentifier.ORACLE_PLSQL);
        tokenTypes.add(new ChameleonTokenType(this, plsql));
        return tokenTypes;
    }

    @Nullable
    @Override
    protected DBLanguageDialectIdentifier getChameleonDialectIdentifier() {
        return DBLanguageDialectIdentifier.ORACLE_PLSQL;
    }

    @Override
    public TokenPairTemplate[] getTokenPairTemplates() {
        return TOKEN_PAIR_TEMPLATES;
    }

    @Override
    protected DBLanguageSyntaxHighlighter createSyntaxHighlighter() {
        return new OracleSQLHighlighter(this);
}

    @Override
    protected OracleSQLParserDefinition createParserDefinition() {
        OracleSQLParser parser = new OracleSQLParser(this);
        return new OracleSQLParserDefinition(parser);
    }

}
