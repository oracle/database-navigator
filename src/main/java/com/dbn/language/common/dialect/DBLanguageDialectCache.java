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

import com.dbn.common.cache.LatentCache;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Read;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Modality;
import com.dbn.connection.ConnectionHandler;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Commons.coalesce;
import static com.dbn.common.util.Documents.touchDocument;
import static com.dbn.common.util.Editors.updateEditorNotifications;

/**
 * Caches dialect suggestions for editor files and refreshes their
 * notifications when background evaluation completes.
 */
@UtilityClass
public final class DBLanguageDialectCache {
    private static final LatentCache<DBLanguagePsiFile, DBLanguageDialect> suggestedDialects = new LatentCache<>() {
        @Override
        protected DBLanguageDialect load(@NotNull DBLanguagePsiFile psiFile) {
            return coalesce(
                    () -> DBLanguageDialectTokenResolver.resolve(psiFile),
                    () -> DBLanguageDialectParserResolver.resolve(psiFile),
                    () -> currentDialect(psiFile));
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
                    touchDocument(editors[0], true);
                } else {
                    updateEditorNotifications(psiFile.getProject(), psiFile.getVirtualFile());
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
}
