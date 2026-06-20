/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.dev.language;

import com.dbn.connection.DatabaseType;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguageDialectIdentifier;
import com.dbn.language.common.DBLanguageParser;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.psql.dialect.PSQLLanguageDialect;
import com.dbn.language.psql.dialect.oracle.OraclePLSQLParser;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.language.sql.dialect.SQLLanguageDialect;
import com.dbn.language.sql.dialect.iso92.Iso92SQLParser;
import com.dbn.language.sql.dialect.mysql.MysqlSQLParser;
import com.dbn.language.sql.dialect.oracle.OracleSQLParser;
import com.dbn.language.sql.dialect.postgres.PostgresSQLParser;
import com.dbn.language.sql.dialect.sqlite.SqliteSQLParser;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;

import static com.dbn.connection.DatabaseType.ISO92;
import static com.dbn.connection.DatabaseType.MYSQL;
import static com.dbn.connection.DatabaseType.ORACLE;
import static com.dbn.connection.DatabaseType.POSTGRES;
import static com.dbn.connection.DatabaseType.SQLITE;
import static com.dbn.language.common.DBLanguageDialectIdentifier.ISO92_SQL;
import static com.dbn.language.common.DBLanguageDialectIdentifier.MYSQL_SQL;
import static com.dbn.language.common.DBLanguageDialectIdentifier.ORACLE_PLSQL;
import static com.dbn.language.common.DBLanguageDialectIdentifier.ORACLE_SQL;
import static com.dbn.language.common.DBLanguageDialectIdentifier.POSTGRES_SQL;
import static com.dbn.language.common.DBLanguageDialectIdentifier.SQLITE_SQL;
import static java.util.Map.of;

public class LanguageSpecificationParserBuilder {
    private final LanguageSpecificationBuilderInput input;

    private static final Map<DatabaseType, Map<DBLanguage, Class<? extends DBLanguageParser>>> PARSERS = new HashMap<>();
    private static final Map<DatabaseType, Map<DBLanguage, DBLanguageDialectIdentifier>> DIALECTS = new HashMap<>();
    static {
        SQLLanguage sql = SQLLanguage.INSTANCE;
        PSQLLanguage psql = PSQLLanguage.INSTANCE;

        PARSERS.put(ORACLE, of(
                sql, OracleSQLParser.class,
                psql, OraclePLSQLParser.class));

        PARSERS.put(MYSQL, of(sql, MysqlSQLParser.class));
        PARSERS.put(POSTGRES, of(sql, PostgresSQLParser.class));
        PARSERS.put(SQLITE, of(sql, SqliteSQLParser.class));
        PARSERS.put(ISO92, of(sql, Iso92SQLParser.class));

        DIALECTS.put(ORACLE, of(
                sql, ORACLE_SQL,
                psql, ORACLE_PLSQL));

        DIALECTS.put(MYSQL, of(sql, MYSQL_SQL));
        DIALECTS.put(POSTGRES, of(sql, POSTGRES_SQL));
        DIALECTS.put(SQLITE, of(sql, SQLITE_SQL));
        DIALECTS.put(ISO92, of(sql, ISO92_SQL));
    }

    public LanguageSpecificationParserBuilder(LanguageSpecificationBuilderInput input) {
        this.input = input;
    }

    @SneakyThrows
    public void build() {
        var parsers = PARSERS.get(input.database);
        if (parsers == null || !parsers.containsKey(input.language)) {
            throw new IllegalArgumentException("Unsupported parser definition: " + input.databaseId + " " + input.language);
        }
        var parser = parsers.get(input.language);

        var dialects = DIALECTS.get(input.database);
        var dialect = dialects.get(input.language);

        ElementTypeBundle.Builder.rebuilding = true;
        DBLanguageDialect languageDialect = input.language.getLanguageDialect(dialect);
        var constructor = parser.getConstructor(getDialectClass());
        DBLanguageParser languageParser = constructor.newInstance(languageDialect);
        ElementTypeBundle elementTypes = languageParser.getElementTypes();
        // TODO write element-type-definition if marked dirty
    }

    private Class<? extends DBLanguageDialect> getDialectClass() {
        if (input.language == SQLLanguage.INSTANCE) return SQLLanguageDialect.class;
        if (input.language == PSQLLanguage.INSTANCE) return PSQLLanguageDialect.class;
        return null;
    }
}
