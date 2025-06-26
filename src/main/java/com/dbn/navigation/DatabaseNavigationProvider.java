/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.navigation;

import com.dbn.common.util.Languages;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObjectPsiCache;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.codeInsight.javadoc.JavaDocUtil;
import com.intellij.navigation.DirectNavigationProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DatabaseNavigationProvider implements DirectNavigationProvider {
    @Override
    public @Nullable PsiElement getNavigationElement(@NotNull PsiElement element) {
        if (element.getLanguage() != Languages.getJavaLanguage()) return null;

        PsiFile priFile = element.getContainingFile();
        VirtualFile virtualFile = priFile.getVirtualFile();

        if (!(virtualFile instanceof DBSourceCodeVirtualFile)) return null;


        DBSourceCodeVirtualFile sourceCodeVirtualFile = (DBSourceCodeVirtualFile) virtualFile;
        DBSchema schema = sourceCodeVirtualFile.getSchema();
        if (schema == null) return null;

        PsiManager psiManager = PsiManager.getInstance(element.getProject());
        PsiElement referenceTarget = JavaDocUtil.findReferenceTarget(psiManager, element.getText(), element);
        if (!(referenceTarget instanceof PsiClass)) return null;

        PsiClass psiClass = (PsiClass) referenceTarget;
        String className = psiClass.getQualifiedName();
        if (className == null) return null;

        DBJavaClass javaClass = schema.getJavaClass(className.replace(".", "/"));
        return DBObjectPsiCache.asPsiElement(javaClass);


    }
}
