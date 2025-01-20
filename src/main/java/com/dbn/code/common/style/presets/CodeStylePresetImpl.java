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

package com.dbn.code.common.style.presets;

import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.psi.BasePsiElement;
import com.intellij.psi.PsiElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@EqualsAndHashCode
public abstract class CodeStylePresetImpl implements CodeStylePreset {
    private final String id;
    private final String name;

    protected CodeStylePresetImpl(@NonNls String id, String name) {
        this.id = id;
        this.name = name;
        //CodeStylePresetsRegister.registerWrapPreset(this);
    }

    public String toString() {
        return name;
    }

    @Nullable
    protected static BasePsiElement getParentPsiElement(@NotNull PsiElement psiElement) {
        PsiElement parentPsiElement = psiElement.getParent();
        if (parentPsiElement instanceof BasePsiElement) {
            return (BasePsiElement) parentPsiElement;
        }
        return null;
    }

    protected static ElementType getParentElementType(PsiElement psiElement) {
        BasePsiElement parentPsiElement = getParentPsiElement(psiElement);
        if (parentPsiElement != null) {
            return parentPsiElement.elementType;
        }
        return null;
    }
}
