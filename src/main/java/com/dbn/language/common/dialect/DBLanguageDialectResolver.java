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
import com.dbn.language.common.DBLanguageDialectIdentifier;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.diagnostics.data.ParserDiagnosticsUtil.countErrorRegions;
import static com.dbn.diagnostics.data.ParserDiagnosticsUtil.countWarnings;

/**
 * Predicts a dialect by combining reserved-word evidence with parser error
 * region counts from the parsed host language and its injected chameleon
 * languages.
 *
 * Candidates are eliminated when another dialect has no more error regions
 * and no more warnings, with fewer of at least one. Distinctive reserved words resolve
 * the remaining candidates.
 */
@UtilityClass
public final class DBLanguageDialectResolver {
    private static final int MIN_MATCHED_RESERVED_WORDS = 2;

    /**
     * Returns the sole remaining candidate after comparing error regions and
     * warnings, or the one with the strongest distinctive reserved-word evidence.
     * Returns {@code null} when the remaining candidates are tied or the token
     * evidence is too weak to distinguish them.
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
            int warningCount = Read.call(parsedFile, f -> countWarnings(f));

            Set<String> words = Read.call(parsedFile, f -> matchedReservedWords(f, dialect, project, text));
            state.add(dialect.getIdentifier(), errorRegionCount, warningCount, words);
        }

        DBLanguageDialectIdentifier dialectId = state.resolve();
        return dialectId == null ? null : language.getLanguageDialect(dialectId);
    }

    private static final class ResolutionState {
        private final Map<DBLanguageDialectIdentifier, Integer> errorCounts = new EnumMap<>(DBLanguageDialectIdentifier.class);
        private final Map<DBLanguageDialectIdentifier, Integer> warningCounts = new EnumMap<>(DBLanguageDialectIdentifier.class);
        private final Map<DBLanguageDialectIdentifier, Set<String>> reservedWords = new EnumMap<>(DBLanguageDialectIdentifier.class);
        private final Map<String, Integer> coverage = new HashMap<>();

        private void add(
                @NotNull DBLanguageDialectIdentifier dialectId,
                int errorCount,
                int warningCount,
                @NotNull Set<String> words) {
            errorCounts.put(dialectId, errorCount);
            warningCounts.put(dialectId, warningCount);
            reservedWords.put(dialectId, words);
            for (String word : words) {
                coverage.merge(word, 1, Integer::sum);
            }
        }

        @Nullable
        private DBLanguageDialectIdentifier resolve() {
            List<DBLanguageDialectIdentifier> candidates = new ArrayList<>(errorCounts.keySet());
            candidates.removeIf(dialectId -> errorCounts.keySet().stream().anyMatch(otherId -> hasFewerIssues(otherId, dialectId)));
            if (candidates.isEmpty()) return null;
            if (candidates.size() == 1) return candidates.get(0);

            candidates.sort(Comparator.comparingInt((DBLanguageDialectIdentifier dialectId) -> score(reservedWords.get(dialectId), coverage)).reversed());
            DBLanguageDialectIdentifier bestDialectId = candidates.get(0);
            int bestScore = score(reservedWords.get(bestDialectId), coverage);
            if (bestScore < MIN_MATCHED_RESERVED_WORDS) return null;
            if (bestScore == score(reservedWords.get(candidates.get(1)), coverage)) return null;

            return bestDialectId;
        }

        /** Both diagnostics must be no worse, with at least one strictly better. */
        private boolean hasFewerIssues(@NotNull DBLanguageDialectIdentifier dialectId, @NotNull DBLanguageDialectIdentifier otherId) {
            int errors = errorCounts.get(dialectId);
            int warnings = warningCounts.get(dialectId);
            int otherErrors = errorCounts.get(otherId);
            int otherWarnings = warningCounts.get(otherId);
            return errors <= otherErrors && warnings <= otherWarnings &&
                    (errors < otherErrors || warnings < otherWarnings);
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
