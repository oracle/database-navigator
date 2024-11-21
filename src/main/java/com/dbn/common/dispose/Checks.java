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

package com.dbn.common.dispose;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public final class Checks {

    public static boolean allValid(Object ... objects) {
        for (Object object : objects) {
            if (isNotValid(object)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotValid(Object object) {
        return !isValid(object);
    }

    public static boolean isValid(Object object) {
        if (object == null) {
            return false;
        }

        if (object instanceof StatefulDisposable) {
            StatefulDisposable disposable = (StatefulDisposable) object;
            return !disposable.isDisposed();
        }

        if (object instanceof Project) {
            Project project = (Project) object;
            return !project.isDisposed();
        }

        if (object instanceof Editor) {
            Editor editor = (Editor) object;
            return !editor.isDisposed();
        }

        if (object instanceof FileEditor) {
            FileEditor editor = (FileEditor) object;
            return editor.isValid();
        }

        if (object instanceof VirtualFile) {
            VirtualFile virtualFile = (VirtualFile) object;
            return virtualFile.isValid();
        }

        if (object instanceof PsiElement) {
            PsiElement psiElement = (PsiElement) object;
            return psiElement.isValid();
        }

        return true;
    }

    @Nullable
    public static <T> T invalidToNull(@Nullable T object) {
        return isValid(object) ? object : null;
    }

    public static boolean isTrue(@Nullable Boolean bool) {
        return bool != null && bool;
    }
}
