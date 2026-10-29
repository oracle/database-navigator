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
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.util.Commons.coalesce;
import static com.dbn.common.util.Documents.touchDocument;
import static com.dbn.common.util.Editors.updateEditorNotifications;
import static com.dbn.language.common.dialect.DBLanguageDialectResolver.resolve;

/**
 * Caches detected dialects and explicit per-file selections, refreshing
 * editor notifications when background evaluation completes.
 */
@UtilityClass
public final class DBLanguageDialectCache {
    private static final long PREDICTION_INTERVAL = TimeUnit.SECONDS.toMillis(20);
    private static final Key<PredictionStamp> PREDICTION_STAMP = Key.create("DBNavigator.DialectPredictionStamp");

    private static final Map<VirtualFile, DBLanguageDialect> selectedDialects = ContainerUtil.createConcurrentWeakMap();

    private static final LatentCache<DBLanguagePsiFile, DBLanguageDialect> suggestedDialects = new LatentCache<>() {
        @Override
        protected DBLanguageDialect load(@NotNull DBLanguagePsiFile psiFile) {
            // An inconclusive prediction keeps the last dialect until stronger evidence is available.
            return coalesce(
                    () -> resolve(psiFile),
                    () -> peek(psiFile),
                    () -> getCurrentDialect(psiFile));
        }

        @Override
        @NotNull
        protected Object key(@NotNull DBLanguagePsiFile psiFile) {
            return psiFile.getVirtualFile();
        }

        @Override
        protected long stamp(@NotNull DBLanguagePsiFile psiFile) {
            VirtualFile file = psiFile.getVirtualFile();
            long version = psiFile.getModificationStamp();
            long now = System.currentTimeMillis();
            PredictionStamp stamp = file.getUserData(PREDICTION_STAMP);
            // Keep the last version visible during rapid edits and expose a changed version only after the cooldown.
            if (stamp == null || (stamp.version() != version && now - stamp.time() >= PREDICTION_INTERVAL)) {
                stamp = new PredictionStamp(version, now);
                file.putUserData(PREDICTION_STAMP, stamp);
            }
            return stamp.version();
        }

        @Override
        protected void notify(@NotNull DBLanguagePsiFile psiFile, @Nullable DBLanguageDialect dialect) {
            Dispatch.run(Modality.nonModal(), () -> {
                Document document = Documents.getDocument(psiFile.getVirtualFile());
                if (document == null) return;

                Editor[] editors = Documents.getEditors(document);
                if (editors.length == 0) return;

                if (getSelectedDialect(psiFile) != null) {
                    updateEditorNotifications(psiFile.getProject(), psiFile.getVirtualFile());
                    return;
                }

                ConnectionHandler connection = psiFile.getConnection();
                DBLanguageDialect currentDialect = getCurrentDialect(psiFile);
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
        DBLanguageDialect selectedDialect = getSelectedDialect(psiFile);
        if (selectedDialect != null) return selectedDialect;
        return suggestedDialects.get(original(psiFile));
    }

    @Nullable
    public static DBLanguageDialect getDetectedDialect(@NotNull DBLanguagePsiFile psiFile) {
        return suggestedDialects.get(original(psiFile));
    }

    @Nullable
    public static DBLanguageDialect getSelectedDialect(@NotNull DBLanguagePsiFile psiFile) {
        return selectedDialects.get(original(psiFile).getVirtualFile());
    }

    public static void setSelectedDialect(
            @NotNull DBLanguagePsiFile psiFile,
            @Nullable DBLanguageDialect dialect) {
        VirtualFile file = original(psiFile).getVirtualFile();
        if (dialect == null) {
            selectedDialects.remove(file);
        } else {
            selectedDialects.put(file, dialect);
        }
    }

    public static void keepDialect(@NotNull DBLanguagePsiFile psiFile) {
        DBLanguageDialect dialect = getSelectedDialect(psiFile);
        if (dialect == null) {
            dialect = getCurrentDialect(psiFile);
        }
        if (dialect != null) {
            setSelectedDialect(psiFile, dialect);
        }
    }

    public static boolean isPending(@NotNull DBLanguagePsiFile psiFile) {
        if (getSelectedDialect(psiFile) != null) return false;
        return suggestedDialects.isPending(original(psiFile));
    }

    private static DBLanguagePsiFile original(@NotNull DBLanguagePsiFile psiFile) {
        PsiFile originalFile = psiFile.getOriginalFile();
        return originalFile instanceof DBLanguagePsiFile databasePsiFile ? databasePsiFile : psiFile;
    }

    @Nullable
    public static DBLanguageDialect getCurrentDialect(@NotNull DBLanguagePsiFile psiFile) {
        return Read.call(psiFile, file -> {
            DBLanguage<?> language = file.getDBLanguage();
            if (language == null) return null;

            ConnectionHandler connection = file.getConnection();
            return connection == null
                    ? language.getMainLanguageDialect()
                    : connection.getLanguageDialect(language);
        });
    }

    private record PredictionStamp(long version, long time) {}
}
