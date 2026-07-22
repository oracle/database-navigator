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

import com.dbn.common.data.Data;
import com.dbn.common.util.Strings;
import com.dbn.connection.DatabaseType;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.sql.SQLLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static com.dbn.connection.DatabaseType.ISO92;
import static com.dbn.connection.DatabaseType.MYSQL;
import static com.dbn.connection.DatabaseType.ORACLE;
import static com.dbn.connection.DatabaseType.POSTGRES;
import static com.dbn.connection.DatabaseType.SQLITE;

@NonNls
public class LanguageSpecificationBuilderInput {
    private static final String CONFIG_FILE_PATH = "modules/dbn-dev/language-builder.properties";
    private static final String PARSER_EXT_BUILDER_PROPERTY = "parserExtBuilder";

    public DatabaseType database;
    public DBLanguage language;

    public String databaseId; // database path & file identifier
    public String languagePid; // language path identifier
    public String languageFid; // language file identifier
    private final Properties properties = new Properties();

    public static final Map<String, DatabaseType> DATABASE_OPTIONS = new LinkedHashMap<>();
    public static final Map<String, DBLanguage> LANGUAGE_OPTIONS = new LinkedHashMap<>();
    public static final Map<String, Artifact> ARTIFACT_OPTIONS = new LinkedHashMap<>();
    public static final Map<String, Action> LEXER_ACTION_OPTIONS = new LinkedHashMap<>();
    public static final Map<String, Action> PARSER_ACTION_OPTIONS = new LinkedHashMap<>();
    static {
        DATABASE_OPTIONS.put("o", ORACLE);
        DATABASE_OPTIONS.put("m", MYSQL);
        DATABASE_OPTIONS.put("p", POSTGRES);
        DATABASE_OPTIONS.put("l", SQLITE);
        DATABASE_OPTIONS.put("i", ISO92);

        LANGUAGE_OPTIONS.put("s", SQLLanguage.INSTANCE);
        LANGUAGE_OPTIONS.put("p", PSQLLanguage.INSTANCE);

        ARTIFACT_OPTIONS.put("l", Artifact.LEXER);
        ARTIFACT_OPTIONS.put("p", Artifact.PARSER);
        ARTIFACT_OPTIONS.put("a", Artifact.ALL);

        LEXER_ACTION_OPTIONS.put("d", Action.UPDATE_DEFINITION);
        LEXER_ACTION_OPTIONS.put("c", Action.BUILD_CLASS);
        LEXER_ACTION_OPTIONS.put("a", Action.ALL);

        PARSER_ACTION_OPTIONS.put("d", Action.UPDATE_DEFINITION);
        PARSER_ACTION_OPTIONS.put("e", Action.BUILD_EXTENSION);
        PARSER_ACTION_OPTIONS.put("a", Action.ALL);
    }

    public LanguageSpecificationBuilderInput() {
        loadProperties();
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

    public File getParserElementsFile() {
        return new File(getProjectPath(), getDefinitionFilePath() + getDefinitionFilePrefix() + "_parser_elements.xml");
    }

    public File getParserElementsExtensionFile() {
        return new File(getProjectPath(), getDefinitionFilePath() + getDefinitionFilePrefix() + "_parser_elements_ext.xml");
    }

    public File getTokenRegistryFile(String categoryIdentifier) {
        return new File(
                getProjectPath(),
                "modules/dbn-dev/src/main/resources/language/" + databaseId + "/" +
                        getDefinitionFilePrefix() + "_" + categoryIdentifier + ".txt");
    }

    public File getHighlighterLexerBaseFile() {
        String commonLexerPath = "src/main/java/com/dbn/language/common/lexer/";
        File file = new File(getProjectPath(), commonLexerPath + "shared_elements_" + databaseId + "_" + languageFid + ".flext");
        if (file.exists()) return file;

        return getHighlighterLexerFile();
    }

    public File getHighlighterLexerFile() {
        return new File(getProjectPath(), getDefinitionFilePath() + getDefinitionFilePrefix() + "_highlighter.flex");
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

    public String getRequiredProperty(String name) {
        String value = getProperty(name);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required configuration property: " + name +
                    " (provide -D" + name + "=<path> or set it in " + CONFIG_FILE_PATH + ")");
        }
        return value;
    }

    public boolean isParserExtBuilderEnabled() {
        return getBooleanProperty(PARSER_EXT_BUILDER_PROPERTY, true);
    }

    private boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = getProperty(name);
        if (Strings.isEmpty(value)) return defaultValue;
        return Data.asBoolean(value);
    }

    private String getProperty(String name) {
        String value = System.getProperty(name);
        return value == null || value.isBlank() ? properties.getProperty(name) : value;
    }

    private void loadProperties() {
        File configFile = new File(getProjectPath(), CONFIG_FILE_PATH);
        if (!configFile.exists()) return;

        try (FileInputStream inputStream = new FileInputStream(configFile)) {
            properties.load(inputStream);
            System.out.println("Loaded configuration: " + configFile.getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Could not load configuration: " + configFile.getAbsolutePath(), e);
        }
    }

    public enum Artifact {
        LEXER,
        PARSER,
        ALL;

        public Map<String, Action> getActionOptions() {
            if (this == ALL) return Map.of();
            return this == LEXER ? LEXER_ACTION_OPTIONS : PARSER_ACTION_OPTIONS;
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public enum Action {
        UPDATE_DEFINITION,
        BUILD_CLASS,
        BUILD_EXTENSION,
        ALL;

        @Override
        public String toString() {
            return name().toLowerCase().replace('_', ' ');
        }
    }
}
