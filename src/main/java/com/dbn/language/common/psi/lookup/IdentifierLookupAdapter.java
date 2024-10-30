package com.dbn.language.common.psi.lookup;

import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.IdentifierElementType;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.element.util.IdentifierCategory;
import com.dbn.language.common.element.util.IdentifierType;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.LeafPsiElement;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public class IdentifierLookupAdapter extends PsiLookupAdapter {
    private final IdentifierType identifierType;
    private final IdentifierCategory identifierCategory;
    private final DBObjectType objectType;
    private final CharSequence identifierName;
    private final ElementTypeAttribute attribute;
    private final LeafPsiElement lookupIssuer;

    public IdentifierLookupAdapter(
            @Nullable LeafPsiElement lookupIssuer,
            @Nullable IdentifierType identifierType,
            @Nullable IdentifierCategory identifierCategory,
            @Nullable DBObjectType objectType,
            CharSequence identifierName) {

        this(lookupIssuer, identifierType, identifierCategory, objectType, identifierName, null);
    }

    public IdentifierLookupAdapter(
            @Nullable LeafPsiElement lookupIssuer,
            @Nullable IdentifierType identifierType,
            @Nullable IdentifierCategory identifierCategory,
            @Nullable DBObjectType objectType,
            CharSequence identifierName,
            ElementTypeAttribute attribute) {

        this.lookupIssuer = lookupIssuer;
        this.identifierType = identifierType;
        this.objectType = objectType;
        this.identifierName = identifierName;
        this.identifierCategory = identifierCategory;
        this.attribute = attribute;
    }


    @Override
    public boolean matches(BasePsiElement basePsiElement) {
        if (basePsiElement instanceof IdentifierPsiElement && basePsiElement != lookupIssuer) {
            IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) basePsiElement;
            return
                matchesResolveState(identifierPsiElement) &&
                matchesType(identifierPsiElement) &&
                matchesObjectType(identifierPsiElement) &&
                matchesCategory(identifierPsiElement) &&
                matchesAttribute(identifierPsiElement) &&
                matchesName(identifierPsiElement);
        }
        return false;
    }


    private boolean matchesResolveState(IdentifierPsiElement identifierPsiElement) {
        return !isAssertResolved() || identifierPsiElement.isResolved() || !identifierPsiElement.isQualifiedIdentifierMember();
    }

    private boolean matchesType(IdentifierPsiElement identifierPsiElement) {
        return identifierType == null ||identifierType == identifierPsiElement.elementType.identifierType;
    }

    public boolean matchesObjectType(IdentifierPsiElement identifierPsiElement) {
        return objectType == null || identifierPsiElement.isObjectOfType(objectType);
    }

    public boolean matchesName(IdentifierPsiElement identifierPsiElement) {
        return identifierName == null || identifierPsiElement.textMatches(identifierName);

    }

    private boolean matchesCategory(IdentifierPsiElement identifierPsiElement) {
        if (identifierCategory == null) return true;
        IdentifierElementType elementType = identifierPsiElement.elementType;
        IdentifierCategory category = elementType.getIdentifierCategory();
        switch (identifierCategory) {
            case ALL: return true;
            case DEFINITION: return category == IdentifierCategory.DEFINITION || identifierPsiElement.isReferenceable();
            case REFERENCE: return category == IdentifierCategory.REFERENCE;
        }
        return false;
    }

    private boolean matchesAttribute(IdentifierPsiElement identifierPsiElement) {
        return attribute == null || identifierPsiElement.elementType.is(attribute);
    }

    @Override
    public boolean accepts(BasePsiElement element) {
        ElementType elementType = element.elementType;
        if (elementType instanceof TokenElementType) {
            TokenElementType tokenElementType = (TokenElementType) elementType;
            return tokenElementType.isIdentifier();
        }
        return true;
    }

    public String toString() {
        return "IdentifierLookupAdapter{" +
                "identifierType=" + identifierType +
                ", identifierCategory=" + identifierCategory +
                ", objectType=" + objectType +
                ", identifierName='" + identifierName + '\'' +
                ", attribute=" + attribute +
                '}';
    }
}
