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
package com.dbn.language.editor.action;

import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.dialect.DBLanguageDialectCache;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Documents.touchDocument;
import static com.dbn.common.util.Documents.whenDocumentsCommitted;
import static com.dbn.common.util.Editors.updateEditorNotifications;
import static com.dbn.nls.NlsResources.txt;

public class DialectSelectAction extends ProjectAction {
    private final DBLanguageDialect dialect;

    public DialectSelectAction(@Nullable DBLanguageDialect dialect) {
        super(dialect == null
                ? txt("app.codeEditor.action.AutoDetectDialect")
                : dialect.getDisplayName());
        this.dialect = dialect;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        Editor editor = Lookups.getEditor(e);
        if (editor == null) return;

        PsiFile psiFile = PsiUtil.getPsiFile(project, editor.getDocument());
        if (!(psiFile instanceof DBLanguagePsiFile databasePsiFile)) return;

        DBLanguageDialectCache.setSelectedDialect(databasePsiFile, dialect);
        touchDocument(editor, true);
        whenDocumentsCommitted(project,
                () -> updateEditorNotifications(project, databasePsiFile.getVirtualFile()));
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        e.getPresentation().setEnabled(virtualFile != null);
    }
}
