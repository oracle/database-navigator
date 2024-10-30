package com.dbn.language.common.psi;

import com.dbn.common.util.Strings;
import com.dbn.language.common.element.impl.ExecVariableElementType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dbn.object.type.DBObjectType;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.function.Consumer;

public class ExecVariablePsiElement extends LeafPsiElement<ExecVariableElementType> {
    public ExecVariablePsiElement(ASTNode astNode, ExecVariableElementType elementType) {
        super(astNode, elementType);
    }

    @Override
    @Nullable
    public BasePsiElement findPsiElement(PsiLookupAdapter lookupAdapter, int scopeCrossCount) {return null;}

    @Override
    public void collectPsiElements(PsiLookupAdapter lookupAdapter, int scopeCrossCount, @NotNull Consumer<BasePsiElement> consumer) {}


    @Override
    public void collectExecVariablePsiElements(@NotNull Consumer<ExecVariablePsiElement> consumer) { consumer.accept(this);}

    @Override
    public void collectSubjectPsiElements(@NotNull Consumer<IdentifierPsiElement> consumer) {}

    @Override
    public NamedPsiElement findNamedPsiElement(String id) {return null;}

    @Override
    public BasePsiElement findPsiElementBySubject(ElementTypeAttribute attribute, CharSequence subjectName, DBObjectType subjectType) {return null;}


    /*********************************************************
     *                       PsiReference                    *
     *********************************************************/
    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return false;
    }

    @Override
    public boolean isSoft() {
        return false;
    }

    /*********************************************************
     *                       ItemPresentation                *
     *********************************************************/
    @Override
    public String getPresentableText() {
        return elementType.tokenType.getValue();
    }

    @Override
    @Nullable
    public Icon getIcon(boolean open) {
        return null;
    }

    @Override
    @Nullable
    public TextAttributesKey getTextAttributesKey() {
        return null;
    }

    @Override
    public boolean hasErrors() {
        return false;
    }

    @Override
    public boolean matches(BasePsiElement basePsiElement, MatchType matchType) {
        if (basePsiElement instanceof ExecVariablePsiElement) {
            ExecVariablePsiElement execVariablePsiElement = (ExecVariablePsiElement) basePsiElement;
            return matchType == MatchType.SOFT || Strings.equalsIgnoreCase(execVariablePsiElement.getChars(), getChars());
        }
        return false;
    }
}
