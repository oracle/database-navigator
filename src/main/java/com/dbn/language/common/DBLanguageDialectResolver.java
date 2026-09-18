/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dbn.language.common;

import com.dbn.common.cache.LatentCache;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Read;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Modality;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.diagnostics.data.ParserDiagnosticsUtil.countErrors;

/**
 * Finds a more suitable parser dialect by parsing the same source with every
 * dialect supported by the file's base language.
 */
@UtilityClass
public final class DBLanguageDialectResolver {
    private static final LatentCache<DBLanguagePsiFile, DBLanguageDialect> suggestedDialects = new LatentCache<>() {
        @Override
        protected DBLanguageDialect load(@NotNull DBLanguagePsiFile psiFile) {
            return resolve(psiFile);
        }

        @Override
        @NotNull
        protected Object key(@NotNull DBLanguagePsiFile psiFile) {
            return psiFile.getVirtualFile();
        }

        @Override
        protected long stamp(@NotNull DBLanguagePsiFile psiFile) {
            return psiFile.getModificationStamp();
        }

        @Override
        protected void notify(@NotNull DBLanguagePsiFile psiFile, @Nullable DBLanguageDialect dialect) {
            Dispatch.run(Modality.nonModal(), () -> {
                Document document = Documents.getDocument(psiFile.getVirtualFile());
                if (document == null) return;

                Editor[] editors = Documents.getEditors(document);
                if (editors.length == 0) return;

                ConnectionHandler connection = psiFile.getConnection();
                DBLanguageDialect currentDialect = currentDialect(psiFile);
                if (dialect != null && dialect != currentDialect && (connection == null || connection.isVirtual())) {
                    Documents.touchDocument(editors[0], true);
                } else {
                    Editors.updateEditorNotifications(psiFile.getProject(), psiFile.getVirtualFile());
                }
            });
        }
    };

    @Nullable
    public static DBLanguageDialect getSuggestedDialect(@NotNull DBLanguagePsiFile psiFile) {
        return suggestedDialects.get(original(psiFile));
    }

    public static boolean isPending(@NotNull DBLanguagePsiFile psiFile) {
        return suggestedDialects.isPending(original(psiFile));
    }

    private static DBLanguagePsiFile original(@NotNull DBLanguagePsiFile psiFile) {
        PsiFile originalFile = psiFile.getOriginalFile();
        return originalFile instanceof DBLanguagePsiFile databasePsiFile ? databasePsiFile : psiFile;
    }

    @Nullable
    private static DBLanguageDialect currentDialect(@NotNull DBLanguagePsiFile psiFile) {
        return Read.call(psiFile, file -> {
            DBLanguage<?> language = file.getDBLanguage();
            if (language == null) return null;

            ConnectionHandler connection = file.getConnection();
            return connection == null
                    ? language.getMainLanguageDialect()
                    : connection.getLanguageDialect(language);
        });
    }

    @Nullable
    public static DBLanguageDialect resolve(@NotNull DBLanguagePsiFile psiFile) {
        DBLanguage<?> language = Read.call(psiFile, f -> f.getDBLanguage());
        ConnectionHandler connection = Read.call(psiFile, DBLanguagePsiFile::getConnection);
        if (language == null) return null;

        DBLanguageDialect currentDialect = connection == null
                ? language.getMainLanguageDialect()
                : connection.getLanguageDialect(language);
        if (currentDialect == null) return null;

        String text = Read.call(psiFile, DBLanguagePsiFile::getText);
        SchemaId schema = Read.call(psiFile, DBLanguagePsiFile::getSchemaId);
        Project project = psiFile.getProject();
        String fileName = psiFile.getName();

        DBLanguageDialect suggestedDialect = null;
        int suggestedErrorCount = Integer.MAX_VALUE;
        int currentErrorCount = Integer.MAX_VALUE;

        for (DBLanguageDialect dialect : language.getLanguageDialects()) {
            DBLanguagePsiFile parsedFile = DBLanguagePsiFile.createFromText(
                    project, fileName, dialect, text, connection, schema);
            if (parsedFile == null) continue;

            int errorCount = Read.call(parsedFile, f -> countErrors(f));
            if (dialect == currentDialect) {
                currentErrorCount = errorCount;
            }

            if (errorCount == 0) {
                return dialect;
            }

            if (dialect != currentDialect && errorCount < suggestedErrorCount) {
                suggestedDialect = dialect;
                suggestedErrorCount = errorCount;
            }
        }

        return suggestedErrorCount < currentErrorCount ? suggestedDialect : currentDialect;
    }
}
