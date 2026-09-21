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
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.psi.ChameleonPsiElement;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.tree.IElementType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.util.Commons.coalesce;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.diagnostics.data.ParserDiagnosticsUtil.countErrorRegions;
import static com.dbn.diagnostics.data.ParserDiagnosticsUtil.countWarnings;

/**
 * Predicts a dialect by combining reserved-word evidence with parser error
 * region counts from the parsed host language and its injected chameleon
 * languages.
 *
 * Reserved-word evidence is the primary signal. Parser error-region counts
 * are used as a fallback when the token evidence is insufficient or
 * ambiguous, with warnings used only to break an error-region tie.
 */
@UtilityClass
public final class DBLanguageDialectResolver {
    private static final int MIN_MATCHED_RESERVED_WORDS = 2;

    /**
     * Returns the dialect with the strongest reserved-word evidence, using
     * parser error-region counts and then warnings to resolve token ties, or
     * {@code null} when the result is ambiguous.
     */
    @Nullable
    public static DBLanguageDialect resolve(@NotNull DBLanguagePsiFile psiFile) {
        DBLanguage<?> language = psiFile.getDBLanguage();
        if (language == null) return null;

        String text = Read.call(psiFile, DBLanguagePsiFile::getText);
        Project project = psiFile.getProject();
        String fileName = psiFile.getName();

        ResolutionState state = new ResolutionState();

        for (DBLanguageDialect dialect : language.getLanguageDialects()) {
            DBLanguagePsiFile parsedFile = DBLanguagePsiFile.createFromText(
                    project, fileName, dialect, text, null, null);
            if (parsedFile == null) continue;

            int errorRegionCount = Read.call(parsedFile, f -> countErrorRegions(f));

            Set<String> words = Read.call(parsedFile, f -> matchedReservedWords(f, dialect, project, text));
            state.add(dialect, parsedFile, errorRegionCount, words);
        }

        return state.resolve();
    }

    private static final class ResolutionState {
        private final Map<DBLanguageDialect, Integer> errorRegionCounts = new HashMap<>();
        private final Map<DBLanguageDialect, DBLanguagePsiFile> parsedFiles = new HashMap<>();
        private final Map<DBLanguageDialect, Set<String>> reservedWords = new HashMap<>();
        private final Map<String, Integer> coverage = new HashMap<>();

        private DBLanguageDialect bestTokenDialect;
        private int bestTokenScore;
        private int secondBestTokenScore;

        private final Set<DBLanguageDialect> bestErrorRegionDialects = new HashSet<>();
        private int lowestErrorRegionCount = Integer.MAX_VALUE;

        private DBLanguageDialect bestWarningDialect;
        private int lowestWarningCount = Integer.MAX_VALUE;
        private boolean ambiguousWarnings;

        private void add(
                @NotNull DBLanguageDialect dialect,
                @NotNull DBLanguagePsiFile parsedFile,
                int errorRegionCount,
                @NotNull Set<String> words) {
            parsedFiles.put(dialect, parsedFile);
            errorRegionCounts.put(dialect, errorRegionCount);
            reservedWords.put(dialect, words);
            for (String word : words) {
                coverage.merge(word, 1, Integer::sum);
            }
        }

        @Nullable
        private DBLanguageDialect resolve() {
            return coalesce(
                    this::resolveTokenDialect,
                    this::resolveErrorRegionDialect,
                    this::resolveWarningDialect);
        }

        @Nullable
        private DBLanguageDialect resolveTokenDialect() {
            for (Map.Entry<DBLanguageDialect, Set<String>> entry : reservedWords.entrySet()) {
                DBLanguageDialect dialect = entry.getKey();
                int tokenScore = score(entry.getValue(), coverage);
                if (tokenScore > bestTokenScore) {
                    secondBestTokenScore = bestTokenScore;
                    bestTokenScore = tokenScore;
                    bestTokenDialect = dialect;
                } else if (tokenScore >= secondBestTokenScore) {
                    secondBestTokenScore = tokenScore;
                }
            }
            return bestTokenScore >= MIN_MATCHED_RESERVED_WORDS && bestTokenScore > secondBestTokenScore ? bestTokenDialect : null;
        }

        @Nullable
        private DBLanguageDialect resolveErrorRegionDialect() {
            for (Map.Entry<DBLanguageDialect, Integer> entry : errorRegionCounts.entrySet()) {
                int errorRegionCount = entry.getValue();
                if (errorRegionCount < lowestErrorRegionCount) {
                    lowestErrorRegionCount = errorRegionCount;
                    bestErrorRegionDialects.clear();
                    bestErrorRegionDialects.add(entry.getKey());
                } else if (errorRegionCount == lowestErrorRegionCount) {
                    bestErrorRegionDialects.add(entry.getKey());
                }
            }
            return bestErrorRegionDialects.size() == 1
                    ? bestErrorRegionDialects.iterator().next()
                    : null;
        }

        @Nullable
        private DBLanguageDialect resolveWarningDialect() {
            if (bestErrorRegionDialects.isEmpty()) return null;

            for (DBLanguageDialect dialect : bestErrorRegionDialects) {
                DBLanguagePsiFile parsedFile = parsedFiles.get(dialect);
                int warningCount = Read.call(parsedFile, f -> countWarnings(f));
                if (warningCount < lowestWarningCount) {
                    lowestWarningCount = warningCount;
                    bestWarningDialect = dialect;
                    ambiguousWarnings = false;
                } else if (warningCount == lowestWarningCount) {
                    ambiguousWarnings = true;
                }
            }
            return ambiguousWarnings ? null : bestWarningDialect;
        }
    }

    @NotNull
    private static Set<String> matchedReservedWords(
            @NotNull DBLanguagePsiFile parsedFile,
            @NotNull DBLanguageDialect hostDialect,
            @NotNull Project project,
            @NotNull String text) {
        List<ChameleonPsiElement> chameleons = new ArrayList<>();
        parsedFile.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof ChameleonPsiElement chameleon) {
                    chameleons.add(chameleon);
                    return;
                }
                super.visitElement(element);
            }
        });
        chameleons.sort(Comparator.comparingInt(PsiElement::getTextOffset));

        Set<String> words = new HashSet<>();
        int offset = 0;
        for (ChameleonPsiElement chameleon : chameleons) {
            TextRange range = chameleon.getTextRange();
            if (range == null) continue;

            int start = range.getStartOffset();
            int end = range.getEndOffset();
            if (start < offset || start < 0 || end > text.length() || end < start) continue;

            tokenize(hostDialect, project, text.substring(offset, start), words);
            tokenize(chameleon.elementType.getLanguageDialect(), project, chameleon.getText(), words);
            offset = end;
        }
        tokenize(hostDialect, project, text.substring(offset), words);
        return words;
    }

    private static void tokenize(
            @NotNull DBLanguageDialect dialect,
            @NotNull Project project,
            @NotNull String text,
            @NotNull Set<String> words) {
        if (!isNotEmptyOrSpaces(text)) return;

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
    }

    private static int score(@Nullable Set<String> words, @NotNull Map<String, Integer> coverage) {
        if (words == null) return 0;

        int score = 0;
        for (String word : words) {
            if (coverage.getOrDefault(word, 0) == 1) {
                score++;
            }
        }
        return score;
    }
}
