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

import com.dbn.language.common.TokenTypeCategory;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.dbn.language.common.TokenTypeCategory.DATATYPE;
import static com.dbn.language.common.TokenTypeCategory.EXCEPTION;
import static com.dbn.language.common.TokenTypeCategory.FUNCTION;
import static com.dbn.language.common.TokenTypeCategory.KEYWORD;
import static com.dbn.language.common.TokenTypeCategory.PARAMETER;
import static java.util.Collections.emptyList;

public class LanguageSpecificationLexerBuilder {
    private final LanguageSpecificationBuilderInput input;
    private final Map<TokenTypeCategory, List<TokenDefinition>> tokenDefinitions = new HashMap<>();

    public LanguageSpecificationLexerBuilder(LanguageSpecificationBuilderInput input) {
        this.input = input;
    }

    public void build() {
        updateParserLexerDefinition();
        updateParserTokensDefinition();
        updateHighlighterLexerDefinition();
    }

    @SneakyThrows
    private void updateParserLexerDefinition() {
        File file = input.getParserLexerFile();
        Path filePath = file.toPath();

        System.out.println("Reading " + filePath);

        List<String> content = Files.readAllLines(filePath);
        content = updateParserLexerDefinition(KEYWORD, content);
        content = updateParserLexerDefinition(FUNCTION, content);
        content = updateParserLexerDefinition(DATATYPE, content);
        content = updateParserLexerDefinition(PARAMETER, content);
        content = updateParserLexerDefinition(EXCEPTION, content);

        System.out.println("Writing " + filePath);
        Files.write(filePath, content);
    }

    @SneakyThrows
    private void updateParserTokensDefinition() {
        File file = input.getParserTokensFile();
        Path filePath = file.toPath();

        System.out.println("Reading " + filePath);

        List<String> content = Files.readAllLines(filePath);
        content = updateParserTokensDefinition(KEYWORD, content);
        content = updateParserTokensDefinition(FUNCTION, content);
        content = updateParserTokensDefinition(DATATYPE, content);
        content = updateParserTokensDefinition(PARAMETER, content);
        content = updateParserTokensDefinition(EXCEPTION, content);

        System.out.println("Writing " + filePath);
        Files.write(filePath, content);
    }

    @SneakyThrows
    private void updateHighlighterLexerDefinition() {
        File file = input.getHighlighterLexerBaseFile();
        Path filePath = file.toPath();

        System.out.println("Reading " + filePath);

        List<String> content = Files.readAllLines(filePath);
        content = updateHighlighterLexerDefinition(KEYWORD, content);
        content = updateHighlighterLexerDefinition(FUNCTION, content);
        content = updateHighlighterLexerDefinition(DATATYPE, content);
        content = updateHighlighterLexerDefinition(PARAMETER, content);
        content = updateHighlighterLexerDefinition(EXCEPTION, content);

        System.out.println("Reading " + filePath);
        Files.write(filePath, content);
    }



    @SneakyThrows
    private List<String> updateParserLexerDefinition(TokenTypeCategory category, List<String> content) {
        System.out.println("Updating " + category + " parser lexer definition");
        List<TokenDefinition> tokens = getTokenDefinitions(category);
        List<String> lexerEntries = createLexerEntries(tokens);
        String categoryIdentifier = getCategoryIdentifier(category).toUpperCase();
        return replaceBlock(content, "// MARKER_BEGIN_" + categoryIdentifier, "// MARKER_END_" + categoryIdentifier, lexerEntries);
    }

    @SneakyThrows
    private List<String> updateParserTokensDefinition(TokenTypeCategory category, List<String> content) {
        System.out.println("Updating " + category + " parser tokens definition");
        List<TokenDefinition> tokens = getTokenDefinitions(category);
        List<String> tokenEntries = createTokenEntries(tokens);
        String categoryIdentifier = getCategoryIdentifier(category).toUpperCase();
        return replaceBlock(content, "<!-- MARKER_BEGIN_" + categoryIdentifier + " -->", "<!-- MARKER_END_" + categoryIdentifier + " -->", tokenEntries);
    }

    private List<String> updateHighlighterLexerDefinition(TokenTypeCategory category, List<String> content) {
        System.out.println("Updating " + category + " highlighter lexer definition");
        List<TokenDefinition> tokens = getTokenDefinitions(category);
        String tokenString = tokens.stream().map(tokenDefinition ->  tokenDefinition.toLexerToken()).collect(Collectors.joining("|"));

        String lineBegin = input.languageFid.toUpperCase() + "_" + category;
        return replaceLine(content, lineBegin, lineBegin + " = " + tokenString);
    }

    private List<TokenDefinition> getTokenDefinitions(TokenTypeCategory category) {
        return tokenDefinitions.computeIfAbsent(category, k -> loadTokenDefinitions(category));
    }

    @SneakyThrows
    private List<TokenDefinition> loadTokenDefinitions(TokenTypeCategory category) {
        String categoryIdentifier = getCategoryIdentifier(category);

        String filePath = "/language/" + input.databaseId + "/" + input.databaseId + "_" + input.languageFid + "_" + categoryIdentifier + ".txt";
        URL fileUrl = LanguageSpecificationBuilder.class.getResource(filePath);
        if (fileUrl == null) return emptyList();

        String tokens = Files.readString(Path.of(fileUrl.getPath()));
        String[] tokenEntries = tokens.split("\n");
        AtomicInteger index = new AtomicInteger(0);
        return Arrays.stream(tokenEntries).
                map(String::trim).
                filter(s -> !s.isEmpty()).
                map(i -> new TokenDefinition(category, i, index.getAndIncrement())).
                toList();
    }

    @NonNls
    private String getCategoryIdentifier(TokenTypeCategory category) {
        return category == KEYWORD ? "keywords" :
                category == FUNCTION ? "functions" :
                category == PARAMETER ? "parameters" :
                category == DATATYPE ? "datatypes" :
                category == EXCEPTION ? "exceptions" :
                "undefined";
    }

    private List<String> createLexerEntries(List<TokenDefinition> tokens) {
        return tokens.stream().map(t -> t.toParserLexerDefinition()).toList();
    }

    private List<String> createTokenEntries(List<TokenDefinition> tokens) {
        return tokens.stream().map(t -> t.toParserTokenDefinition()).toList();
    }

    private static List<String> replaceBlock(List<String> lines, @NonNls String beginMarker, @NonNls String endMarker, List<String> replacement) {
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


    static class TokenDefinition {
        private final TokenTypeCategory category;
        private final String id;
        private final int index;
        private final boolean reserved;

        public TokenDefinition(TokenTypeCategory category, String identifier, int index) {
            String[] split = identifier.split("\\|");
            this.category = category;
            this.id = split[0];
            this.index = index;
            this.reserved = split.length > 1 && split[1].equals("reserved");
        }

        @NonNls
        public String toParserLexerDefinition() {
            String idToken = toLexerToken();

            return switch (category) {
                case KEYWORD -> idToken + " {return tt.ktt("+ index + ");}";
                case FUNCTION -> idToken + " {return tt.ftt("+ index + ");}";
                case PARAMETER -> idToken + " {return tt.ptt("+ index + ");}";
                case DATATYPE -> idToken + " {return tt.dtt("+ index + ");}";
                case EXCEPTION -> idToken + " {return tt.ett("+ index + ");}";
                default -> throw new UnsupportedOperationException("Unexpected value: " + category);
            };
        }

        @NonNls
        public String toLexerToken() {
            String idToken = "\"" + id.toLowerCase().replace(" ", "\"{ws}\"") + "\"";
            return idToken.replace("_n\"", "_\"{digit}+");
        }

        @NonNls
        public String toParserTokenDefinition() {
            String idToken = id.replace(" ", "_");
            String identifier = switch (category) {
                case KEYWORD -> "KW_" + idToken;
                case FUNCTION -> "FN_" + idToken;
                case PARAMETER -> "PRM_" + idToken;
                case DATATYPE -> "DT_" + idToken;
                case EXCEPTION -> "EX_" + idToken;
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
