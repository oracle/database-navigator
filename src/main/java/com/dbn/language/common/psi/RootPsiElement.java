package com.dbn.language.common.psi;

import com.dbn.common.util.Cloneable;
import com.dbn.language.common.element.impl.NamedElementType;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

public class RootPsiElement extends NamedPsiElement implements ExecutableBundlePsiElement, Cloneable<RootPsiElement> {

    public RootPsiElement(ASTNode astNode, NamedElementType elementType) {
        super(astNode, elementType);
    }

    @Override
    public List<ExecutablePsiElement> getExecutablePsiElements() {
        List<ExecutablePsiElement> bucket = new ArrayList<>();
        collectExecutablePsiElements(bucket, this);
        return bucket;
    }

    private static void collectExecutablePsiElements(List<ExecutablePsiElement> bucket, PsiElement element) {
        PsiElement child = element.getFirstChild();
        while (child != null) {
            if (child instanceof ExecutablePsiElement) {
                ExecutablePsiElement executablePsiElement = (ExecutablePsiElement) child;
                bucket.add(executablePsiElement);
            } else {
                collectExecutablePsiElements(bucket, child);
            }
            child = child.getNextSibling();
        }
    }

    @Override
    public boolean hasErrors() {
        return false;
    }

    @Override
    public RootPsiElement clone() {
        return (RootPsiElement) super.clone();
    }

    /*********************************************************
     *                    ItemPresentation                   *
     *********************************************************/
    @Override
    public String getPresentableText() {
        return elementType.getDescription();
    }

    @Override
    @Nullable
    public Icon getIcon(boolean open) {
        return super.getIcon(open);
    }

    @Override
    @Nullable
    public TextAttributesKey getTextAttributesKey() {
        return null;
    }
}
