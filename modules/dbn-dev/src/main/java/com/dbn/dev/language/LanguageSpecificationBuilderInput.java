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
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.sql.SQLLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@NonNls
public class LanguageSpecificationBuilderInput {
    public DatabaseType database;
    public DBLanguage language;

    public String databaseId; // database path & file identifier
    public String languagePid; // language path identifier
    public String languageFid; // language file identifier

    public static final Map<String, DatabaseType> DATABASE_OPTIONS = new LinkedHashMap<>();
    public static final Map<String, DBLanguage> LANGUAGE_OPTIONS = new LinkedHashMap<>();
    public static final Map<String, Operation> OPERATION_OPTIONS = new LinkedHashMap<>();
    static {
        DATABASE_OPTIONS.put("o", DatabaseType.ORACLE);
        DATABASE_OPTIONS.put("m", DatabaseType.MYSQL);
        DATABASE_OPTIONS.put("p", DatabaseType.POSTGRES);
        DATABASE_OPTIONS.put("l", DatabaseType.SQLITE);

        LANGUAGE_OPTIONS.put("s", SQLLanguage.INSTANCE);
        LANGUAGE_OPTIONS.put("p", PSQLLanguage.INSTANCE);

        OPERATION_OPTIONS.put("l", Operation.LEXER_DEFINITION);
        OPERATION_OPTIONS.put("p", Operation.PARSER_DEFINITION);
    }

    public void setDatabase(DatabaseType database) {
        this.database = database;
        this.databaseId = database.name().toLowerCase();
    }

    public void setLanguage(DBLanguage language) {
        this.language = language;
        this.languagePid = language == SQLLanguage.INSTANCE ? "sql" : "psql";
        this.languageFid = language == SQLLanguage.INSTANCE ? "sql" : database == DatabaseType.ORACLE ? "plsql" : "psql";
    }

    public File getParserLexerFile() {
        return new File(getProjectPath(), getDefinitionFilePath() + getDefinitionFilePrefix() + "_parser.flex");
    }

    public File getParserTokensFile() {
        return new File(getProjectPath(), getDefinitionFilePath() + getDefinitionFilePrefix() + "_parser_tokens.xml");
    }

    public File getHighlighterLexerFile() {
        String commonLexerPath = "src/main/java/com/dbn/language/common/lexer/";
        File file = new File(getProjectPath(), commonLexerPath + "shared_elements_" + databaseId + "_" + languageFid + ".flext");
        if (file.exists()) return file;

        file = new File(getProjectPath(), getDefinitionFilePath() + getDefinitionFilePrefix() + "_highlighter.flex");
        return file;
    }

    public String getDefinitionFilePath() {
        return "src/main/java/com/dbn/language/" + languagePid + "/dialect/" + databaseId + "/";
    }

    public String getDefinitionFilePrefix() {
        return databaseId + "_" + languageFid;
    }

    public @NotNull File getProjectPath() {
        return Paths.get("").toAbsolutePath().toFile();
    }

    public enum Operation {
        LEXER_DEFINITION,
        PARSER_DEFINITION
    }
}
