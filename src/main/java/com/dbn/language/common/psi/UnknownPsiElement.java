package com.dbn.language.common.psi;

import com.dbn.code.common.style.formatting.FormattingAttributes;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dbn.object.type.DBObjectType;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public class UnknownPsiElement extends BasePsiElement<ElementTypeBase> {
    public UnknownPsiElement(ASTNode astNode, ElementTypeBase elementType) {
        super(astNode, elementType);
    }

    @Override
    public FormattingAttributes getFormattingAttributes() {
        return FormattingAttributes.NO_ATTRIBUTES;
    }

    @Override
    public int approximateLength() {
        return getTextLength();
    }

    @Nullable
    @Override public BasePsiElement findPsiElement(PsiLookupAdapter lookupAdapter, int scopeCrossCount) {return null;}
    @Override public void collectPsiElements(PsiLookupAdapter lookupAdapter, int scopeCrossCount, @NotNull Consumer<BasePsiElement> consumer) {}


    @Override public void collectExecVariablePsiElements(@NotNull Consumer<ExecVariablePsiElement> consumer) {}
    @Override public void collectSubjectPsiElements(@NotNull Consumer<IdentifierPsiElement> consumer) {}
    @Override public NamedPsiElement findNamedPsiElement(String id) {return null;}
    @Override public BasePsiElement findFirstPsiElement(ElementTypeAttribute attribute) {return null;}
    @Override public BasePsiElement findFirstPsiElement(Class<? extends ElementType> clazz) { return null; }

    @Override public BasePsiElement findFirstLeafPsiElement() {return null;}
    @Override public BasePsiElement findPsiElementByAttribute(ElementTypeAttribute attribute) {return null;}
    @Override public BasePsiElement findPsiElementBySubject(ElementTypeAttribute attribute, CharSequence subjectName, DBObjectType subjectType) {return null;}

    @Override public boolean hasErrors() {
        return true;
    }

    @Override
    public boolean matches(BasePsiElement remote, MatchType matchType) {
        if (matchType == MatchType.SOFT) {
            return remote instanceof UnknownPsiElement;
        } else {
            return getTextLength() == remote.getTextLength() && Objects.equals(getText(), remote.getText());
        }
    }

    @Override
    public String toString() {
        return elementType.getId();

    }
}
