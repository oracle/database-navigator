package com.dbn.code.common.style.presets.iteration;

import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.Nullable;

public class IterationChopDownIfNotSinglePreset extends IterationAbstractPreset {
    public IterationChopDownIfNotSinglePreset() {
        super("chop_down_if_not_single", "Chop down unless single element");
    }

    @Override
    @Nullable
    public Wrap getWrap(BasePsiElement psiElement, CodeStyleSettings settings) {
        BasePsiElement parentPsiElement = getParentPsiElement(psiElement);
        if (parentPsiElement != null) {
            IterationElementType iterationElementType = (IterationElementType) parentPsiElement.elementType;
            ElementType elementType = psiElement.elementType;

            boolean shouldWrap = PsiUtil.getChildCount(parentPsiElement) > 1;
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

            boolean shouldWrap = PsiUtil.getChildCount(parentPsiElement) > 1;
            return getSpacing(iterationElementType, elementType, shouldWrap);
        }
        return null;
    }
}