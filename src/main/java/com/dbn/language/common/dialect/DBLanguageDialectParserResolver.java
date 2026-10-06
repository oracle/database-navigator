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
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.diagnostics.data.ParserDiagnosticsUtil.countIssues;

/**
 * Finds a more suitable parser dialect by comparing parser issue counts
 * (errors plus unparsed-token warnings) for every dialect supported by the
 * file's base language.
 */
@UtilityClass
public final class DBLanguageDialectParserResolver {
    /** Returns the lowest-issue dialect, or {@code null} when the best score is tied. */
    @Nullable
    public static DBLanguageDialect resolve(@NotNull DBLanguagePsiFile psiFile) {
        DBLanguage<?> language = psiFile.getDBLanguage();
        if (language == null) return null;

        String text = Read.call(psiFile, DBLanguagePsiFile::getText);
        Project project = psiFile.getProject();
        String fileName = psiFile.getName();

        DBLanguageDialect suggestedDialect = null;
        int suggestedIssueCount = Integer.MAX_VALUE;
        boolean ambiguous = false;

        for (DBLanguageDialect dialect : language.getLanguageDialects()) {
            DBLanguagePsiFile parsedFile = DBLanguagePsiFile.createFromText(
                    project, fileName, dialect, text, null, null);
            if (parsedFile == null) continue;

            int issueCount = Read.call(parsedFile, f -> countIssues(f));

            if (issueCount == 0) {
                return dialect;
            }

            if (issueCount < suggestedIssueCount) {
                suggestedDialect = dialect;
                suggestedIssueCount = issueCount;
                ambiguous = false;
            } else if (issueCount == suggestedIssueCount) {
                ambiguous = true;
            }
        }

        return ambiguous ? null : suggestedDialect;
    }
}
