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

package com.dbn.language.common;


import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ref.WeakRef;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode
public class PsiFileRef<T extends PsiFile>{
    private WeakRef<T> psiFileRef;

    private PsiFileRef(T psiFile) {
        this.psiFileRef = WeakRef.of(psiFile);
    }

    @Nullable
    public T get() {
        T psiFile = psiFileRef.get();
        if (psiFile != null && !psiFile.isValid()) {
            Project project = psiFile.getProject();
            VirtualFile virtualFile = psiFile.getVirtualFile();

            PsiFile newPsiFile = PsiUtil.getPsiFile(project, virtualFile);
            if (newPsiFile != null &&
                    newPsiFile != psiFile &&
                    newPsiFile.getClass() == psiFile.getClass() &&
                    newPsiFile.isValid()) {

                psiFile = (T) newPsiFile;
                psiFileRef = WeakRef.of(psiFile);
            } else {
                psiFile = null;
            }
        }
        return psiFile;
    }

    public static <T extends PsiFile> PsiFileRef<T> of(@NotNull T psiFile) {
        return new PsiFileRef<>(psiFile);
    }

    @Nullable
    public static <T extends PsiFile> T from(@Nullable PsiFileRef<T> psiFileRef) {
        return psiFileRef == null ? null : psiFileRef.get();
    }

    @NotNull
    public T ensure() {
        return Failsafe.nn(get());
    }
}
