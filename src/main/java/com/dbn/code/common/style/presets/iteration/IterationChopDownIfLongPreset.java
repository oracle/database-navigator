package com.dbn.code.common.style.presets.iteration;

import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.psi.BasePsiElement;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.Nullable;

public class IterationChopDownIfLongPreset extends IterationAbstractPreset {
    public IterationChopDownIfLongPreset() {
        super("chop_down_if_long", "Chop down if long");
    }

    @Override
    @Nullable
    public Wrap getWrap(BasePsiElement psiElement, CodeStyleSettings settings) {
        BasePsiElement parentPsiElement = getParentPsiElement(psiElement);
        if (parentPsiElement != null) {
            IterationElementType iterationElementType = (IterationElementType) parentPsiElement.elementType;
            ElementType elementType = psiElement.elementType;

            boolean shouldWrap = parentPsiElement.approximateLength() > settings.getRightMargin(psiElement.getLanguage());
            return getWrap(elementType, iterationElementType, shouldWrap);
        }
        return null;
    }

    @Override
    @Nullable
    public Spacing getSpacing(BasePsiElement psiElement, CodeStyleSettings settings) {
        BasePsiElement parentPsiElement = getParentPsiElement(psiElement);
        if (parentPsiElement != null) {
            IterationElementType iterationElementType = (IterationElementType) parentPsiElement.elementType;
            ElementType elementType = psiElement.elementType;

            boolean shouldWrap = parentPsiElement.approximateLength() > settings.getRightMargin(psiElement.getLanguage());
            return getSpacing(iterationElementType, elementType, shouldWrap);
        }
        return null;
    }

}