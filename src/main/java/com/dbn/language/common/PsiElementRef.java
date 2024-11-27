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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiElementRef<T extends PsiElement> extends WeakRef<T> {
    private PsiElementRef(T psiElement) {
        super(psiElement);
    }

    @Contract("null -> null;!null -> !null;")
    public static <T extends PsiElement> PsiElementRef<T> of(@Nullable T psiElement) {
        return psiElement == null ? null : new PsiElementRef<>(psiElement);
    }

    @Nullable
    public static <T extends PsiElement> T get(@Nullable PsiElementRef<T> ref) {
        return ref == null ? null : ref.get();
    }

    @Nullable
    @Override
    public T get() {
        return super.get();
    }

    @Override
    @NotNull
    public T ensure() {
        return Failsafe.nn(get());
    }

    @Override
    public String toString() {
        T element = get();
        return element == null ? "null" : element.toString();
    }
}
