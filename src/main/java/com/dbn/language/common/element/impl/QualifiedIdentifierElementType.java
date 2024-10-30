package com.dbn.language.common.element.impl;

import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.QualifiedIdentifierElementTypeLookupCache;
import com.dbn.language.common.element.parser.impl.QualifiedIdentifierElementTypeParser;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.language.common.psi.QualifiedIdentifierPsiElement;
import com.dbn.object.type.DBObjectType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.stringAttribute;

public final class QualifiedIdentifierElementType extends ElementTypeBase {
    private final TokenElementType separatorToken;
    private final List<LeafElementType[]> variants = new ArrayList<>();
    private final Set<DBObjectType> objectTypeCache = EnumSet.noneOf(DBObjectType.class);
    private int maxLength;

    public QualifiedIdentifierElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
        List<Element> children = def.getChildren();
        for (Element child : children) {
            List<LeafElementType[]> childVariants = createVariants(child);
            for (LeafElementType[] childVariant : childVariants) {
                for (LeafElementType leafElementType : childVariant) {
                    if (leafElementType instanceof IdentifierElementType) {
                        IdentifierElementType identifierElementType = (IdentifierElementType) leafElementType;
                        objectTypeCache.add(identifierElementType.getObjectType());
                    }
                }
            }
            variants.addAll(childVariants);
        }
        String separatorId = stringAttribute(def, "separator");
        separatorToken = new TokenElementType(bundle, this, separatorId, TokenElementType.SEPARATOR);
    }

    @Override
    public QualifiedIdentifierElementTypeLookupCache createLookupCache() {
        return new QualifiedIdentifierElementTypeLookupCache(this);
    }

    @NotNull
    @Override
    public QualifiedIdentifierElementTypeParser createParser() {
        return new QualifiedIdentifierElementTypeParser(this);
    }

    public List<LeafElementType[]> getVariants() {
        return variants;
    }

    private List<LeafElementType[]> createVariants(Element element) throws ElementTypeDefinitionException {
        List<LeafElementType[]> variants = new ArrayList<>();

        List<Element> children = element.getChildren();
        LeafElementType[] leafElementTypes = new LeafElementType[children.size()];
        boolean[] optional = new boolean[children.size()];
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            String type = child.getName();
            leafElementTypes[i] = (LeafElementType) bundle.resolveElementDefinition(child, type, this);
            optional[i] = getBooleanAttribute(child, "optional");
            leafElementTypes[i].optional = optional[i];
        }
        variants.add(leafElementTypes);
        if (maxLength < leafElementTypes.length) maxLength = leafElementTypes.length;

        int lastIndex = leafElementTypes.length - 1;
        int leftIndex = 0;
        int rightIndex = lastIndex;

        while (optional[rightIndex]) {
            LeafElementType[] variant = new LeafElementType[rightIndex];
            System.arraycopy(leafElementTypes, 0, variant, 0, variant.length);
            variants.add(variant);
            rightIndex--;
        }

        while (optional[leftIndex]) {
            rightIndex = lastIndex;
            LeafElementType[] variant = new LeafElementType[lastIndex - leftIndex];
            System.arraycopy(leafElementTypes, leftIndex +1, variant, 0, variant.length);
            variants.add(variant);

            while (optional[rightIndex]) {
                variant = new LeafElementType[rightIndex - leftIndex -1];
                System.arraycopy(leafElementTypes, leftIndex +1, variant, 0, variant.length);
                variants.add(variant);
                rightIndex--;
            }
            leftIndex++;
        }

        return variants;
    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        /*SortedSet<QualifiedIdentifierVariant> parseVariants = getParseVariants();
        if (parseVariants != null) {
            astNode.putUserData(PARSE_VARIANTS_KEY, parseVariants);
        }*/
        return new QualifiedIdentifierPsiElement(astNode, this);
    }

    @NotNull
    @Override
    public String getName() {
        return "identifier sequence (" + getId() + ")";
    }

    /*private SortedSet<QualifiedIdentifierVariant> parseVariants;
    private SortedSet<QualifiedIdentifierVariant> getParseVariants() {
        return parseVariants;
    }
    private void setParseVariants(SortedSet<QualifiedIdentifierVariant> parseVariants) {
        this.parseVariants = parseVariants;
    }*/

    @Override
    public boolean isLeaf() {
        return false;
    }

    public TokenElementType getSeparatorToken() {
        return separatorToken;
    }

    public boolean containsObjectType(DBObjectType objectType) {
        return objectTypeCache.contains(objectType);
    }

}
