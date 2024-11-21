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

package com.dbn.object.common;

import com.dbn.common.ref.WeakRefCache;
import com.dbn.navigation.psi.DBObjectPsiDirectory;
import com.dbn.navigation.psi.DBObjectPsiFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Unsafe.cast;

@UtilityClass
public class DBObjectPsiCache {
    private static final WeakRefCache<DBObject, PsiFile> psiFiles = WeakRefCache.weakKey();
    private static final WeakRefCache<DBObject, PsiElement> psiElements = WeakRefCache.weakKey();
    private static final WeakRefCache<DBObject, PsiDirectory> psiDirectories = WeakRefCache.weakKey();

    public static void clear(DBObject object) {
        psiFiles.remove(object);
        psiElements.remove(object);
        psiDirectories.remove(object);
    }

    public static void map(DBObject object, PsiElement psiElement) {
        if (psiElement instanceof PsiDirectory) {
            psiDirectories.set(object, (PsiDirectory) psiElement);
            return;
        }

        if (psiElement instanceof PsiFile) {
            psiFiles.set(object, (PsiFile) psiElement);
            return;
        }

        psiElements.set(object, psiElement);
    }

    @Contract("null -> null;!null -> !null;")
    public static <T extends PsiElement> T asPsiElement(@Nullable DBObject object) {
        return object == null ? null : cast(psiElements.computeIfAbsent(object, o -> new DBObjectPsiElement(o.ref())));
    }

    @Contract("null -> null;!null -> !null;")
    public static PsiFile asPsiFile(@Nullable DBObject object) {
        return object == null ? null : psiFiles.computeIfAbsent(object, o -> new DBObjectPsiFile(o.ref()));
    }

    @Contract("null -> null;!null -> !null;")
    public static PsiDirectory asPsiDirectory(@Nullable DBObject object) {
        return object == null ? null : psiDirectories.computeIfAbsent(object, o -> new DBObjectPsiDirectory(o.ref()));
    }

}
