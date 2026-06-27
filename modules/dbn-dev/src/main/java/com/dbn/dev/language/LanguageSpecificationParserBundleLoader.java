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

package com.dbn.dev.language;

import com.dbn.connection.DatabaseType;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguageDialectIdentifier;
import com.dbn.language.common.DBLanguageParser;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.impl.OneOfElementTypeBuilder;
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
import org.jdom.Document;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.dbn.connection.DatabaseType.ISO92;
import static com.dbn.connection.DatabaseType.MYSQL;
import static com.dbn.connection.DatabaseType.ORACLE;
import static com.dbn.connection.DatabaseType.POSTGRES;
import static com.dbn.connection.DatabaseType.SQLITE;
import static com.dbn.dev.language.LanguageSpecificationXmlUtil.fileToDocument;
import static com.dbn.language.common.DBLanguageDialectIdentifier.ISO92_SQL;
import static com.dbn.language.common.DBLanguageDialectIdentifier.MYSQL_SQL;
import static com.dbn.language.common.DBLanguageDialectIdentifier.ORACLE_PLSQL;
import static com.dbn.language.common.DBLanguageDialectIdentifier.ORACLE_SQL;
import static com.dbn.language.common.DBLanguageDialectIdentifier.POSTGRES_SQL;
import static com.dbn.language.common.DBLanguageDialectIdentifier.SQLITE_SQL;
import static java.util.Map.of;

class LanguageSpecificationParserBundleLoader {
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

    LanguageSpecificationParserBundleLoader(LanguageSpecificationBuilderInput input) {
        this.input = input;
    }

    @SneakyThrows
    @SuppressWarnings("removal")
    ElementTypeBundle load(Consumer<ElementTypeBundle.Builder> builderCallback) {
        return load(builderCallback, true, true);
    }

    /**
     * Legacy ambiguous-path toggle. Trie-based parser extensions supersede this path.
     */
    @Deprecated(forRemoval = true)
    @SneakyThrows
    @SuppressWarnings("removal")
    ElementTypeBundle load(BiConsumer<ElementTypeBundle, ElementTypeBundle.Builder> builderCallback, boolean rebuilding, boolean legacyAmbiguousPathRebuildEnabled) {
        AtomicReference<ElementTypeBundle.Builder> builder = new AtomicReference<>();
        ElementTypeBundle bundle = load(builder::set, rebuilding, legacyAmbiguousPathRebuildEnabled);
        builderCallback.accept(bundle, builder.get());
        return bundle;
    }

    /**
     * Legacy ambiguous-path toggle. Trie-based parser extensions supersede this path.
     */
    @Deprecated(forRemoval = true)
    @SneakyThrows
    @SuppressWarnings("removal")
    ElementTypeBundle load(Consumer<ElementTypeBundle.Builder> builderCallback, boolean rebuilding, boolean legacyAmbiguousPathRebuildEnabled) {
        var parsers = PARSERS.get(input.database);
        if (parsers == null || !parsers.containsKey(input.language)) {
            throw new IllegalArgumentException("Unsupported parser definition: " + input.databaseId + " " + input.language);
        }

        var parser = parsers.get(input.language);
        var dialects = DIALECTS.get(input.database);
        var dialect = dialects.get(input.language);

        boolean previousRebuilding = ElementTypeBundle.Builder.rebuilding;
        boolean previousLegacyAmbiguousPathRebuildEnabled = OneOfElementTypeBuilder.legacyAmbiguousPathRebuildEnabled;
        try {
            ElementTypeBundle.Builder.rebuilding = rebuilding;
            OneOfElementTypeBuilder.legacyAmbiguousPathRebuildEnabled = legacyAmbiguousPathRebuildEnabled;
            DBLanguageDialect languageDialect = input.language.getLanguageDialect(dialect);
            var constructor = parser.getConstructor(getDialectClass());
            DBLanguageParser languageParser = constructor.newInstance(languageDialect);
            File definitionFile = getParserElementsFile();

            Document definitionDocument = fileToDocument(definitionFile);
            System.out.println("Building element type bundle: " + languageDialect.getID());
            return new ElementTypeBundle(languageDialect, languageParser.getTokenTypes(), definitionDocument, null, builderCallback);
        } finally {
            System.out.println("Element type bundle loading finished");
            ElementTypeBundle.Builder.rebuilding = previousRebuilding;
            OneOfElementTypeBuilder.legacyAmbiguousPathRebuildEnabled = previousLegacyAmbiguousPathRebuildEnabled;
        }
    }

    private File getParserElementsFile() {
        File file = input.getParserElementsFile();
        if (!file.exists()) {
            throw new IllegalArgumentException("Parser elements definition does not exist: " + file.getAbsolutePath());
        }
        return file;
    }

    private Class<? extends DBLanguageDialect> getDialectClass() {
        if (input.language == SQLLanguage.INSTANCE) return SQLLanguageDialect.class;
        if (input.language == PSQLLanguage.INSTANCE) return PSQLLanguageDialect.class;
        return null;
    }
}
