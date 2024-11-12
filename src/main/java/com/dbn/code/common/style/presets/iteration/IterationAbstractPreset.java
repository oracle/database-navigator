package com.dbn.code.common.style.presets.iteration;

import com.dbn.code.common.style.presets.CodeStylePresetImpl;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;

public abstract class IterationAbstractPreset extends CodeStylePresetImpl {
    IterationAbstractPreset(String id, String name) {
        super(id, name);
    }

    @Override
    public boolean accepts(BasePsiElement psiElement) {
        return !psiElement.elementType.is(ElementTypeAttribute.STATEMENT) &&
                getParentElementType(psiElement) instanceof IterationElementType;
    }

    protected Wrap getWrap(ElementType elementType, IterationElementType iterationElementType, boolean shouldWrap) {
        if (shouldWrap) {
            if (elementType instanceof TokenElementType) {
                TokenElementType tokenElementType = (TokenElementType) elementType;
                return iterationElementType.isSeparator(tokenElementType) ? null : WRAP_ALWAYS;
            } else {
                return WRAP_ALWAYS;
            }

        } else {
            return WRAP_NONE;
        }
    }

    protected Spacing getSpacing(IterationElementType iterationElementType, ElementType elementType, boolean shouldWrap) {
        if (elementType instanceof TokenElementType) {
            TokenElementType tokenElementType = (TokenElementType) elementType;
            if (iterationElementType.isSeparator(tokenElementType)) {
                return  tokenElementType.isCharacter() ?
                            SPACING_NO_SPACE :
                            SPACING_ONE_SPACE;
            }
        }
        return shouldWrap ? SPACING_LINE_BREAK : SPACING_ONE_SPACE;
    }
}
