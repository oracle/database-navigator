/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.code.refactoring;

import com.dbn.language.common.DBLanguage;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandlerDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class MoveDatabaseFileHandler extends MoveHandlerDelegate {
    @Override
    public boolean canMove(PsiElement[] elements, @Nullable PsiElement targetContainer, @Nullable PsiReference reference) {
        return super.canMove(elements, targetContainer, reference);
    }

    @Override
    public boolean canMove(DataContext dataContext) {
        return super.canMove(dataContext);
    }

    @Override
    public boolean isValidTarget(@Nullable PsiElement targetElement, PsiElement[] sources) {
        return super.isValidTarget(targetElement, sources);
    }

    @Override
    public void doMove(Project project, PsiElement[] elements, @Nullable PsiElement targetContainer, @Nullable MoveCallback callback) {
        super.doMove(project, elements, targetContainer, callback);
    }

    @Override
    public PsiElement adjustTargetForMove(DataContext dataContext, PsiElement targetContainer) {
        return super.adjustTargetForMove(dataContext, targetContainer);
    }

    @Override
    public PsiElement [] adjustForMove(Project project, PsiElement[] sourceElements, PsiElement targetElement) {
        return super.adjustForMove(project, sourceElements, targetElement);
    }

    @Override
    public boolean tryToMove(PsiElement element, Project project, DataContext dataContext, @Nullable PsiReference reference, Editor editor) {
        return super.tryToMove(element, project, dataContext, reference, editor);
    }

    @Override
    public void collectFilesOrDirsFromContext(DataContext dataContext, Set<PsiElement> filesOrDirs) {
        super.collectFilesOrDirsFromContext(dataContext, filesOrDirs);
    }

    @Override
    public boolean isMoveRedundant(PsiElement source, PsiElement target) {
        return super.isMoveRedundant(source, target);
    }

    @Override
    public @Nullable String getActionName(PsiElement [] elements) {
        return super.getActionName(elements);
    }

    @Override
    public boolean supportsLanguage(@NotNull Language language) {
        return language instanceof DBLanguage;
    }
}
