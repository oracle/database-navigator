package com.dbn.code.common.lookup;

import com.dbn.code.common.completion.CodeCompletionContext;
import com.dbn.language.common.element.util.IdentifierType;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.object.type.DBObjectType;

import javax.swing.Icon;

public class IdentifierLookupItemBuilder extends LookupItemBuilder {
    private IdentifierPsiElement identifierPsiElement;
    public IdentifierLookupItemBuilder(IdentifierPsiElement identifierPsiElement) {
        this.identifierPsiElement = identifierPsiElement;
    }

    @Override
    public String getTextHint() {
        IdentifierType identifierType = identifierPsiElement.elementType.identifierType;
        DBObjectType objectType = identifierPsiElement.elementType.getObjectType();
        String objectTypeName = objectType == DBObjectType.ANY ? "object" : objectType.getName();
        String identifierTypeName =
                identifierType == IdentifierType.ALIAS  ? " alias" :
                identifierType == IdentifierType.VARIABLE ? " variable" :
                        "";
        return objectTypeName + identifierTypeName + (identifierPsiElement.isDefinition() ? " def" : " ref");
    }

    @Override
    public boolean isBold() {
        return false;
    }

    @Override
    public CharSequence getText(CodeCompletionContext completionContext) {
        return identifierPsiElement.getChars();
    }

    @Override
    public Icon getIcon() {
        DBObjectType objectType = identifierPsiElement.getObjectType();
        return objectType.getIcon();
    }
}