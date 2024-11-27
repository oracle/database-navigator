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

package com.dbn.code.common.style.presets.clause;

import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.NamedPsiElement;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.Nullable;

public class ClauseChopDownIfLongStatementPreset extends ClauseAbstractPreset {
    public ClauseChopDownIfLongStatementPreset() {
        super("chop_down_if_statement_long", "Chop down if statement long");
    }

    @Override
    @Nullable
    public Wrap getWrap(BasePsiElement psiElement, CodeStyleSettings settings) {
        NamedPsiElement namedPsiElement = getEnclosingStatementElement(psiElement);
        boolean shouldWrap = namedPsiElement != null && namedPsiElement.approximateLength() > settings.getRightMargin(psiElement.getLanguage());
        return shouldWrap ? WRAP_ALWAYS : WRAP_NONE;

    }

    @Override
    @Nullable
    public Spacing getSpacing(BasePsiElement psiElement, CodeStyleSettings settings) {
        NamedPsiElement namedPsiElement = getEnclosingStatementElement(psiElement);
        boolean shouldWrap = namedPsiElement!= null && namedPsiElement.approximateLength() > settings.getRightMargin(psiElement.getLanguage());
        return getSpacing(psiElement, shouldWrap);
    }

    @Nullable
    private NamedPsiElement getEnclosingStatementElement(BasePsiElement psiElement) {
        BasePsiElement<?> parentPsiElement = getParentPsiElement(psiElement);
        if (parentPsiElement != null) {
            DBLanguagePsiFile psiFile = parentPsiElement.getFile();
            NamedPsiElement namedPsiElement = parentPsiElement.findEnclosingElement(ElementTypeAttribute.STATEMENT);
            if (namedPsiElement == null) {
                PsiElement childPsiElement = psiFile.getFirstChild();
                while (childPsiElement != null) {
                    if (childPsiElement instanceof NamedPsiElement) {
                        return (NamedPsiElement) childPsiElement;
                    }
                    childPsiElement = childPsiElement.getNextSibling();
                }
            } else {
                return namedPsiElement;
            }
        }
        return null;
    }

}