package com.dbn.code.common.style.presets;

import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.psi.BasePsiElement;
import com.intellij.psi.PsiElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@EqualsAndHashCode
public abstract class CodeStylePresetImpl implements CodeStylePreset {
    private final String id;
    private final String name;

    protected CodeStylePresetImpl(String id, String name) {
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
