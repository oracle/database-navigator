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

package com.dbn.navigation.psi;

import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public interface ReadonlyPsiElementStub extends NamedPsiElementStub {

    @Override
    default PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        throw notSupported();
    }

    @Override
    default PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
        throw notSupported();
    }

    @Override
    default PsiElement addBefore(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        throw notSupported();
    }

    @Override
    default PsiElement addAfter(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        throw notSupported();
    }

    @Override
    default void checkAdd(@NotNull PsiElement element) throws IncorrectOperationException {
        throw notSupported();
    }

    @Override
    default PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        throw notSupported();
    }

    @Override
    default PsiElement addRangeBefore(@NotNull PsiElement first, @NotNull PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        throw notSupported();
    }

    @Override
    default PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        throw notSupported();
    }

    @Override
    default void delete() throws IncorrectOperationException {
        throw notSupported();
    }

    @Override
    default void checkDelete() throws IncorrectOperationException {
        throw notSupported();
    }

    @Override
    default void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        throw notSupported();
    }

    @Override
    default PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        throw notSupported();
    }


    static @NotNull IncorrectOperationException notSupported() {
        return new IncorrectOperationException("Operation not supported");
    }
}
