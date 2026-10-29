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
package com.dbn.language.common.dialect;

import com.dbn.common.thread.Read;
import com.dbn.common.util.Strings;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.TokenType;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.toLowerCase;

/**
 * Predicts a dialect from reserved words recognized by its lexer.
 *
 * This is intentionally independent from {@link DBLanguageDialectParserResolver}.
 * It is a lexical heuristic and must not replace the parser-error based
 * resolver without a separate decision about how the two signals should be
 * combined.
 */
@UtilityClass
public final class DBLanguageDialectTokenResolver {
    private static final int MIN_MATCHED_RESERVED_WORDS = 2;

    /**
     * Returns the dialect with the most dialect-specific reserved words, or
     * {@code null} when there is not enough evidence or the result is tied.
     *
     * Reserved words are counted once per distinct word. Words recognized by
     * more than one dialect are ignored so common SQL vocabulary cannot create
     * a false prediction.
     */
    @Nullable
    public static DBLanguageDialect resolve(@NotNull DBLanguagePsiFile psiFile) {
        DBLanguage<?> language = psiFile.getDBLanguage();
        if (language == null) return null;

        String text = Read.call(psiFile, f -> f.getText());
        if (Strings.isEmpty(text)) return null;

        Project project = psiFile.getProject();
        Map<DBLanguageDialect, Set<String>> matches = new HashMap<>();
        Map<String, Integer> coverage = new HashMap<>();

        for (DBLanguageDialect dialect : language.getLanguageDialects()) {
            Set<String> words = matchedReservedWords(dialect, project, text);
            matches.put(dialect, words);
            for (String word : words) {
                coverage.merge(word, 1, Integer::sum);
            }
        }

        DBLanguageDialect bestDialect = null;
        int bestScore = 0;
        int secondBestScore = 0;
        for (Map.Entry<DBLanguageDialect, Set<String>> entry : matches.entrySet()) {
            int score = score(entry.getValue(), coverage);
            if (score > bestScore) {
                secondBestScore = bestScore;
                bestScore = score;
                bestDialect = entry.getKey();
            } else if (score >= secondBestScore) {
                secondBestScore = score;
            }
        }

        if (bestScore < MIN_MATCHED_RESERVED_WORDS || bestScore == secondBestScore) {
             return null;
        }
        return bestDialect;
    }

    @NotNull
    private static Set<String> matchedReservedWords(
            @NotNull DBLanguageDialect dialect,
            @NotNull Project project,
            @NotNull String text) {
        Set<String> words = new HashSet<>();
        Lexer lexer = dialect.getParserDefinition().createLexer(project);
        lexer.start(text);
        while (lexer.getTokenType() != null) {
            IElementType elementType = lexer.getTokenType();
            if (elementType instanceof TokenType tokenType && tokenType.isReservedWord()) {
                String tokenText = lexer.getTokenText();
                if (isNotEmptyOrSpaces(tokenText)) {
                    words.add(toLowerCase(tokenText));
                }
            }
            lexer.advance();
        }
        return words;
    }

    private static int score(@NotNull Set<String> words, @NotNull Map<String, Integer> coverage) {
        int score = 0;
        for (String word : words) {
            if (coverage.getOrDefault(word, 0) == 1) {
                score++;
            }
        }
        return score;
    }
}
