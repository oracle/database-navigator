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

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.action.Lookups;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguageFileType;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.dialect.DBLanguageDialectCache;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;

import static com.dbn.nls.NlsResources.txt;

@BackgroundUpdate
public class DialectSelectDropdownAction extends ComboBoxAction implements DumbAware {

    public DialectSelectDropdownAction() {
        super(txt("app.codeEditor.action.ScriptEditorDialect"));
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(
            @NotNull JComponent component,
            @NotNull DataContext dataContext) {
        Project project = Lookups.getProject(component);
        VirtualFile virtualFile = Lookups.getVirtualFile(component);
        return project == null || virtualFile == null
                ? new DefaultActionGroup()
                : new DialectSelectActionGroup(project, virtualFile);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Project project = Lookups.getProject(e);
        VirtualFile virtualFile = Lookups.getVirtualFile(e);

        boolean visible = project != null && virtualFile != null &&
                virtualFile.getFileType() instanceof DBLanguageFileType &&
                !(virtualFile instanceof DBConsoleVirtualFile) &&
                FileConnectionContextManager.getInstance(project).getConnection(virtualFile) == null;
        presentation.setVisible(visible);
        if (!visible) return;

        DBLanguagePsiFile psiFile = getPsiFile(project, virtualFile);
        if (psiFile == null) {
            presentation.setVisible(false);
            return;
        }

        DBLanguageDialect selectedDialect = DBLanguageDialectCache.getSelectedDialect(psiFile);
        if (selectedDialect == null) {
            DBLanguageDialect detectedDialect = DBLanguageDialectCache.getDetectedDialect(psiFile);
            presentation.setText(detectedDialect == null
                    ? txt("app.codeEditor.action.SqlDialect")
                    : txt("app.codeEditor.action.DetectedDialect", detectedDialect.getDisplayName()), false);
        } else {
            presentation.setText(selectedDialect.getDisplayName(), false);
        }
        presentation.setIcon((Icon) null);
    }

    private static DBLanguagePsiFile getPsiFile(
            @NotNull Project project,
            @NotNull VirtualFile virtualFile) {
        return PsiUtil.getPsiFile(project, virtualFile) instanceof DBLanguagePsiFile psiFile ? psiFile : null;
    }
}
