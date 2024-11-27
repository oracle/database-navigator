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

package com.dbn.code.common.style.presets.iteration;

import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.NamedPsiElement;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.Nullable;

public class IterationChopDownIfLongStatementPreset extends IterationAbstractPreset {
    public IterationChopDownIfLongStatementPreset() {
        super("chop_down_if_statement_long", "Chop down if statement long");
    }

    @Override
    @Nullable
    public Wrap getWrap(BasePsiElement psiElement, CodeStyleSettings settings) {
        BasePsiElement<?> parentPsiElement = getParentPsiElement(psiElement);
        if (parentPsiElement == null) return null;

        IterationElementType iterationElementType = (IterationElementType) parentPsiElement.elementType;
        ElementType elementType = psiElement.elementType;

        NamedPsiElement namedPsiElement = parentPsiElement.findEnclosingElement(ElementTypeAttribute.EXECUTABLE);
        if (namedPsiElement == null) return null;

        boolean shouldWrap = namedPsiElement.approximateLength() > settings.getRightMargin(psiElement.getLanguage());
        return getWrap(elementType, iterationElementType, shouldWrap);
    }

    @Override
    @Nullable
    public Spacing getSpacing(BasePsiElement psiElement, CodeStyleSettings settings) {
        BasePsiElement<?> parentPsiElement = getParentPsiElement(psiElement);
        if (parentPsiElement == null) return null;

        IterationElementType iterationElementType = (IterationElementType) parentPsiElement.elementType;
        ElementType elementType = psiElement.elementType;

        NamedPsiElement namedPsiElement = parentPsiElement.findEnclosingElement(ElementTypeAttribute.EXECUTABLE);
        if (namedPsiElement == null) return null;

        boolean shouldWrap = namedPsiElement.approximateLength() > settings.getRightMargin(psiElement.getLanguage());
        return getSpacing(iterationElementType, elementType, shouldWrap);
    }
}