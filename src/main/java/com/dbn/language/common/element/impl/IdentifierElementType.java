package com.dbn.language.common.element.impl;

import com.dbn.code.common.style.formatting.FormattingDefinition;
import com.dbn.code.common.style.formatting.SpacingDefinition;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.IdentifierElementTypeLookupCache;
import com.dbn.language.common.element.parser.impl.IdentifierElementTypeParser;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.element.util.ElementTypeDefinition;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.language.common.element.util.IdentifierCategory;
import com.dbn.language.common.element.util.IdentifierType;
import com.dbn.language.common.psi.IdentifierDefPsiElement;
import com.dbn.language.common.psi.IdentifierRefPsiElement;
import com.dbn.language.common.resolve.UnderlyingObjectResolver;
import com.dbn.object.type.DBObjectType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
public final class IdentifierElementType extends LeafElementType {
    public static final FormattingDefinition FORMATTING = new FormattingDefinition(null, null, SpacingDefinition.ONE_SPACE, null);

    public IdentifierType identifierType;
    private IdentifierCategory identifierCategory;
    private DBObjectType objectType;
    private String underlyingObjectResolverId;
    private boolean referenceable; // is referenceable ()
    private boolean localReference; // is local reference


    public IdentifierElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
        tokenType = bundle.getTokenTypeBundle().getIdentifier();
    }

    @Override
    public IdentifierElementTypeLookupCache createLookupCache() {
        return new IdentifierElementTypeLookupCache(this);
    }

    @NotNull
    @Override
    public IdentifierElementTypeParser createParser() {
        return new IdentifierElementTypeParser(this);
    }

    @Override
    protected void loadDefinition(Element def) throws ElementTypeDefinitionException {
        super.loadDefinition(def);
        String objectTypeName = ElementTypeBundle.determineMandatoryAttribute(def, "type", "Incomplete definition " + getId() + ". ");
        objectType = ElementTypeBundle.resolveObjectType(objectTypeName); 

        String type = def.getName();
        identifierType =
            ElementTypeDefinition.OBJECT_DEF.is(type) || ElementTypeDefinition.OBJECT_REF.is(type) ? IdentifierType.OBJECT :
            ElementTypeDefinition.ALIAS_DEF.is(type) || ElementTypeDefinition.ALIAS_REF.is(type) ? IdentifierType.ALIAS :
            ElementTypeDefinition.VARIABLE_DEF.is(type) || ElementTypeDefinition.VARIABLE_REF.is(type) ? IdentifierType.VARIABLE : IdentifierType.UNKNOWN;

        identifierCategory =
                    ElementTypeDefinition.OBJECT_REF.is(type) ||
                    ElementTypeDefinition.ALIAS_REF.is(type) ||
                    ElementTypeDefinition.VARIABLE_REF.is(type) ? IdentifierCategory.REFERENCE :
                    ElementTypeDefinition.OBJECT_DEF.is(type) ||
                    ElementTypeDefinition.ALIAS_DEF.is(type) ||
                    ElementTypeDefinition.VARIABLE_DEF.is(type) ? IdentifierCategory.DEFINITION : IdentifierCategory.UNKNOWN;

        referenceable = getBooleanAttribute(def, "referenceable");
        localReference = getBooleanAttribute(def, "local");

        underlyingObjectResolverId = stringAttribute(def, "underlying-object-resolver");

        if (isDefinition()) {
            setDefaultFormatting(FORMATTING);
        }

/*
        setDefaultFormatting();
        if (is(ElementTypeAttribute.STATEMENT)) {
            formatting = FormattingDefinitionFactory.mergeDefinitions(formatting, FormattingDefinition.STATEMENT_ATTRIBUTES_INDENTED);
        }
*/

    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return isDefinition() ?
                new IdentifierDefPsiElement(astNode, this) :
                new IdentifierRefPsiElement(astNode, this);
    }

    @NotNull
    @Override
    public String getName() {
        String prefix =
                isObject() ? (isReference() ? "object-ref " : "object-def ") :
                isAlias() ? (isReference() ? "alias-ref " : "alias-def ") :
                isVariable() ? (isReference() ? "variable-ref " : "variable-def ") : "unknown";
        return prefix + getObjectTypeName() + " (" + getId() + ")";
    }

    public String toString() {
        return getObjectTypeName() + " (" + getId() + ")";
    }

    /*********************************************************
     *                Identifier type accessors               *
     *********************************************************/

    public boolean isObject() {
        return identifierType == IdentifierType.OBJECT;
    }

    public boolean isAlias() {
        return identifierType == IdentifierType.ALIAS;
    }
    
    public boolean isVariable() {
        return identifierType == IdentifierType.VARIABLE;
    }

    public boolean isReference() {
        return identifierCategory == IdentifierCategory.REFERENCE;
    }

    public boolean isDefinition() {
        return identifierCategory == IdentifierCategory.DEFINITION;
    }

    public String getObjectTypeName() {
        return objectType.getName();
    }

    public String getQualifiedObjectTypeName() {
        return getObjectTypeName() + " " + identifierType.lowerCaseName();

    }

    public boolean isObjectOfType(DBObjectType type) {
        return objectType.matches(type);
    }

    public boolean isSubject() {
        return is(ElementTypeAttribute.SUBJECT);
    }

    @Override
    public boolean isSameAs(LeafElementType elementType) {
        if (elementType instanceof IdentifierElementType) {
            IdentifierElementType identifierElementType = (IdentifierElementType) elementType;
            return  identifierElementType.objectType.matches(objectType) &&
                    identifierElementType.identifierType == identifierType &&
                    identifierElementType.identifierCategory == identifierCategory;
        }
        return false;
    }

    @Override
    public boolean isIdentifier() {
        return true;
    }

    public UnderlyingObjectResolver getUnderlyingObjectResolver() {
        return underlyingObjectResolverId == null ? null : UnderlyingObjectResolver.get(underlyingObjectResolverId);
    }
}
