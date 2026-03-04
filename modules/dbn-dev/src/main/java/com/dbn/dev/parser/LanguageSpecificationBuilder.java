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

package com.dbn.dev.parser;

import com.dbn.connection.DatabaseType;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.sql.SQLLanguage;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.dbn.language.common.TokenTypeCategory.DATATYPE;
import static com.dbn.language.common.TokenTypeCategory.FUNCTION;
import static com.dbn.language.common.TokenTypeCategory.KEYWORD;
import static com.dbn.language.common.TokenTypeCategory.PARAMETER;

@NonNls
public class LanguageSpecificationBuilder {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static DatabaseType database;
    private static DBLanguage language;
    private static Operation operation;

    private static String databaseIdentifier;
    private static String languageIdentifier;
    private static final Map<TokenTypeCategory, List<TokenDefinition>> tokenDefinitions = new HashMap<>();

    private static final Map<String, DatabaseType> databaseOptions = new LinkedHashMap<>();
    private static final Map<String, DBLanguage> languageOptions = new LinkedHashMap<>();
    private static final Map<String, Operation> operationOptions = new LinkedHashMap<>();
    static {
        databaseOptions.put("o", DatabaseType.ORACLE);
        databaseOptions.put("m", DatabaseType.MYSQL);
        databaseOptions.put("p", DatabaseType.POSTGRES);
        databaseOptions.put("l", DatabaseType.SQLITE);

        languageOptions.put("s", SQLLanguage.INSTANCE);
        languageOptions.put("p", PSQLLanguage.INSTANCE);

        operationOptions.put("l", Operation.LEXER_DEFINITION);
        operationOptions.put("p", Operation.PARSER_DEFINITION);
    }

    public static void main(String[] args) {
        database = selectOption("database", databaseOptions);
        databaseIdentifier = database.name().toLowerCase();
        System.out.println("Selected database type: " + database);


        language = selectOption("language", languageOptions);
        languageIdentifier = language == SQLLanguage.INSTANCE ? "sql" : "psql";
        System.out.println("Selected language: " + language);

        operation = selectOption("operation", operationOptions);
        System.out.println("Selected operation: " + operation);

        if (operation == Operation.LEXER_DEFINITION) {
            updateParserLexerDefinition();
            updateParserTokensDefinition();
            updateHighlighterLexerDefinition();
        }
    }

    @SneakyThrows
    private static void updateParserLexerDefinition() {
        File file = getParserLexerFile();
        Path filePath = file.toPath();

        System.out.println("Reading " + filePath);

        List<String> content = Files.readAllLines(filePath);
        content = updateParserLexerDefinition(KEYWORD, content);
        content = updateParserLexerDefinition(FUNCTION, content);
        content = updateParserLexerDefinition(DATATYPE, content);
        content = updateParserLexerDefinition(PARAMETER, content);

        System.out.println("Writing " + filePath);
        Files.write(filePath, content);
    }

    @SneakyThrows
    private static void updateParserTokensDefinition() {
        File file = getParserTokensFile();
        Path filePath = file.toPath();

        System.out.println("Reading " + filePath);

        List<String> content = Files.readAllLines(filePath);
        content = updateParserTokensDefinition(KEYWORD, content);
        content = updateParserTokensDefinition(FUNCTION, content);
        content = updateParserTokensDefinition(DATATYPE, content);
        content = updateParserTokensDefinition(PARAMETER, content);

        System.out.println("Writing " + filePath);
        Files.write(filePath, content);
    }

    @SneakyThrows
    private static void updateHighlighterLexerDefinition() {
        File file = getHighlighterLexerFile();
        Path filePath = file.toPath();

        System.out.println("Reading " + filePath);

        List<String> content = Files.readAllLines(filePath);
        content = updateHighlighterLexerDefinition(KEYWORD, content);
        content = updateHighlighterLexerDefinition(FUNCTION, content);
        content = updateHighlighterLexerDefinition(DATATYPE, content);
        content = updateHighlighterLexerDefinition(PARAMETER, content);

        System.out.println("Reading " + filePath);
        Files.write(filePath, content);
    }



    @SneakyThrows
    private static List<String> updateParserLexerDefinition(TokenTypeCategory category, List<String> content) {
        System.out.println("Updating " + category + " parser lexer definition");
        List<TokenDefinition> tokens = getTokenDefinitions(category);
        List<String> lexerEntries = createLexerEntries(tokens);
        String categoryIdentifier = getCategoryIdentifier(category).toUpperCase();
        return replaceBlock(content, "// MARKER_BEGIN_" + categoryIdentifier, "// MARKER_END_" + categoryIdentifier, lexerEntries);
    }

    @SneakyThrows
    private static List<String> updateParserTokensDefinition(TokenTypeCategory category, List<String> content) {
        System.out.println("Updating " + category + " parser tokens definition");
        List<TokenDefinition> tokens = getTokenDefinitions(category);
        List<String> tokenEntries = createTokenEntries(tokens);
        String categoryIdentifier = getCategoryIdentifier(category).toUpperCase();
        return replaceBlock(content, "<!-- MARKER_BEGIN_" + categoryIdentifier + " -->", "<!-- MARKER_END_" + categoryIdentifier + " -->", tokenEntries);
    }

    private static List<String> updateHighlighterLexerDefinition(TokenTypeCategory category, List<String> content) {
        System.out.println("Updating " + category + " highlighter lexer definition");
        List<TokenDefinition> tokens = getTokenDefinitions(category);
        String tokenString = tokens.stream().map(tokenDefinition ->  tokenDefinition.toLexerToken()).collect(Collectors.joining("|"));

        String lineBegin = languageIdentifier.toUpperCase() + "_" + category;
        return replaceLine(content, lineBegin, lineBegin + " = " + tokenString);
    }

    private static File getParserLexerFile() {
        return new File(getProjectPath(), getDefinitionFilePath() + getDefinitionFilePrefix() + "_parser.flex");
    }

    private static File getParserTokensFile() {
        return new File(getProjectPath(), getDefinitionFilePath() + getDefinitionFilePrefix() + "_parser_tokens.xml");
    }

    private static String getDefinitionFilePath() {
        return "src/main/java/com/dbn/language/" + languageIdentifier + "/dialect/" + databaseIdentifier + "/";
    }

    private static String getDefinitionFilePrefix() {
        return databaseIdentifier + "_" + languageIdentifier;
    }

    private static File getHighlighterLexerFile() {
        String commonLexerPath = "src/main/java/com/dbn/language/common/lexer/";
        File file = new File(getProjectPath(), commonLexerPath + "shared_elements_" + databaseIdentifier + "_" + languageIdentifier + ".flext");
        if (file.exists()) return file;

        file = new File(getProjectPath(), getDefinitionFilePath() + getDefinitionFilePrefix() + "_highlighter.flex");
        return file;
    }

    private static @NotNull File getProjectPath() {
        return Paths.get("").toAbsolutePath().toFile();
    }

    private static List<TokenDefinition> getTokenDefinitions(TokenTypeCategory category) {
        return tokenDefinitions.computeIfAbsent(category, k -> loadTokenDefinitions(category));
    }

    @SneakyThrows
    private static List<TokenDefinition> loadTokenDefinitions(TokenTypeCategory category) {
        String categoryIdentifier = getCategoryIdentifier(category);

        String filePath = "/language/" + databaseIdentifier + "_" + languageIdentifier + "_" + categoryIdentifier + ".txt";
        URL fileUrl = LanguageSpecificationBuilder.class.getResource(filePath);
        String tokens = Files.readString(Path.of(fileUrl.getPath()));
        String[] tokenEntries = tokens.split("\n");
        AtomicInteger index = new AtomicInteger(0);
        return Arrays.stream(tokenEntries).map(i -> new TokenDefinition(category, i, index.getAndIncrement())).toList();
    }

    private static String getCategoryIdentifier(TokenTypeCategory category) {
        return category == KEYWORD ? "keywords" :
                category == FUNCTION ? "functions" :
                category == PARAMETER ? "parameters" :
                category == DATATYPE ? "datatypes" : "undefined";
    }

    private static List<String> createLexerEntries(List<TokenDefinition> tokens) {
        return tokens.stream().map(t -> t.toParserLexerDefinition()).toList();
    }

    private static List<String> createTokenEntries(List<TokenDefinition> tokens) {
        return tokens.stream().map(t -> t.toParserTokenDefinition()).toList();
    }

    private static String presentableOptions(Map<String, ?> options) {
        return options.keySet().stream().map(k -> k + " = " + options.get(k)).collect(Collectors.joining("\n"));
    }

    private static <T> T selectOption(String name, Map<String, T> options) {
        System.out.println("_______________________________________");
        System.out.print("Select " + name + "\n" + presentableOptions(options) + "\nx = EXIT\n");
        String s = SCANNER.next();

        T option = options.get(s.toLowerCase());
        if (option != null) return option;

        if (s.equalsIgnoreCase("x")) {
            System.out.print("Bye bye!");
            System.exit(0);
            return null;
        }

        System.out.println("Invalid option: " + s);
        return selectOption(name, options);
    }

    private static List<String> replaceBlock(List<String> lines, String beginMarker, String endMarker, List<String> replacement) {
        List<String> result = new ArrayList<>();

        boolean beginMatched = false;
        boolean endMatched = false;
        for (String line : lines) {
            if (!beginMatched) {
                result.add(line);
                if (line.trim().equals(beginMarker)) {
                    beginMatched = true;
                    result.addAll(replacement);
                }
                continue;
            }

            if (line.trim().equals(endMarker)) {
                endMatched = true;
                result.add(line);
                beginMatched = false;
            }
        }

        assert beginMatched;
        assert endMatched;
        return result;
    }

    private static List<String> replaceLine(List<String> lines, String lineBegin, String replacement) {
        List<String> result = new ArrayList<>();

        boolean matched = false;
        for (String line : lines) {
            if (line.trim().startsWith(lineBegin)) {
                matched = true;
                result.add(replacement);
            } else {
                result.add(line);
            }
        }

        assert matched;
        return result;
    }




    enum Operation {
        LEXER_DEFINITION,
        PARSER_DEFINITION
    }

    static class TokenDefinition {
        private final TokenTypeCategory category;
        private final String id;
        private final int index;
        private final boolean reserved;

        public TokenDefinition(TokenTypeCategory category, String identifier, int index) {
            String[] split = identifier.split(":");
            this.category = category;
            this.id = split[0];
            this.index = index;
            this.reserved = split.length > 1 && split[1].equals("Y");
        }

        public String toParserLexerDefinition() {
            String idToken = toLexerToken();

            return switch (category) {
                case KEYWORD -> idToken + " {return tt.ktt("+ index + ");}";
                case FUNCTION -> idToken + " {return tt.ftt("+ index + ");}";
                case PARAMETER -> idToken + " {return tt.ptt("+ index + ");}";
                case DATATYPE -> idToken + " {return tt.dtt("+ index + ");}";
                default -> throw new UnsupportedOperationException("Unexpected value: " + category);
            };
        }

        public String toLexerToken() {
            String idToken = "\"" + id.toLowerCase().replace(" ", "\"{ws}\"") + "\"";
            return idToken.replace("_n\"", "_\"{digit}+");
        }

        public String toParserTokenDefinition() {
            String idToken = id.replace(" ", "_");
            String identifier = switch (category) {
                case KEYWORD -> "KW_" + idToken;
                case FUNCTION -> "FN_" + idToken;
                case PARAMETER -> "PRM_" + idToken;
                case DATATYPE -> "DT_" + idToken;
                default -> throw new UnsupportedOperationException("Unexpected value: " + category);
            };

            return "        " +
                    "<token " +
                    "index=\"" + index + "\" " +
                    "id=\"" + identifier + "\" " +
                    "value=\"" + id.toLowerCase() + "\" " +
                    "category=\"" + category.name().toLowerCase() + "\"" +
                    (reserved ? " reserved=\"true\"" : "") +  "/>";
        }


        @Override
        public String toString() {
            return id;
        }

    }
}
