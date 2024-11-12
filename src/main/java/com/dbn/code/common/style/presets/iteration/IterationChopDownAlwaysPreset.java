package com.dbn.code.common.style.presets.iteration;

import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.psi.BasePsiElement;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.Nullable;

public class IterationChopDownAlwaysPreset extends IterationAbstractPreset {
    public IterationChopDownAlwaysPreset() {
        super("chop_down", "Chop down");
    }

    @Override
    public boolean accepts(BasePsiElement psiElement) {
        return getParentElementType(psiElement) instanceof IterationElementType;
    }

    @Override
    @Nullable
    public Wrap getWrap(BasePsiElement psiElement, CodeStyleSettings settings) {
        BasePsiElement parentPsiElement = getParentPsiElement(psiElement);
        if (parentPsiElement != null) {
            IterationElementType iterationElementType = (IterationElementType) parentPsiElement.elementType;
            ElementType elementType = psiElement.elementType;
            return getWrap(elementType, iterationElementType, true);
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
            return getSpacing(iterationElementType, elementType, true);
        }
        return null;
    }
}