package com.dbn.language.common.psi;

import com.dbn.common.Pair;
import com.dbn.common.ref.WeakRefCache;
import com.dbn.language.common.element.impl.IdentifierElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierVariant;
import com.dbn.language.common.element.parser.ParseResultType;
import com.dbn.object.common.DBObject;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;

public class QualifiedIdentifierPsiElement extends SequencePsiElement<QualifiedIdentifierElementType> {
    private static final WeakRefCache<QualifiedIdentifierPsiElement, Pair<Integer, List<QualifiedIdentifierVariant>>> parseVariants = WeakRefCache.weakKey();

    public QualifiedIdentifierPsiElement(ASTNode astNode, QualifiedIdentifierElementType elementType) {
        super(astNode, elementType);
    }

    public List<QualifiedIdentifierVariant> getParseVariants() {
        return parseVariants.compute(this, (k, v) -> {
            if (v == null || v.first() != k.getElementsCount()) {
                List<QualifiedIdentifierVariant> variants = k.buildParseVariants();
                v = Pair.of(k.getElementsCount(), variants);
            }
            return v;
        }).second();
    }

    public int getIndexOf(LeafPsiElement leafPsiElement) {
        int index = 0;
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child == leafPsiElement){
                return index;
            }
            if (child instanceof IdentifierPsiElement) {
                index++;
            }
            child = child.getNextSibling();
        }
        return -1;        
    }

    public int getIndexOf(IdentifierElementType identifierElementType) {
        int index = 0;
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof IdentifierPsiElement) {
                IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) child;
                if (identifierPsiElement.elementType == identifierElementType) {
                    return index;
                } else {
                    index++;
                }
            }
            child = child.getNextSibling();
        }
        return -1;
    }

    public IdentifierPsiElement getLeafAtIndex(int index) {
        if (index >= 0) {
            int idx = 0;
            PsiElement child = getFirstChild();
            while (child != null) {
                if (child instanceof IdentifierPsiElement) {
                    if (idx == index) {
                        return (IdentifierPsiElement) child;
                    } else {
                        idx++;
                    }
                }
                child = child.getNextSibling();
            }
        }
        return null;
    }

    private List<QualifiedIdentifierVariant> buildParseVariants() {
        List<QualifiedIdentifierVariant> parseVariants = new ArrayList<>();
        for (LeafElementType[] elementTypes : elementType.getVariants()) {

            ParseResultType resultType = ParseResultType.FULL_MATCH;
            for (int i=0; i< elementTypes.length; i++) {

                BasePsiElement leaf = getLeafAtIndex(i);
                // if no mach -> consider as partial if not first element
                if (leaf == null) {
                    resultType = i==0 ? ParseResultType.NO_MATCH : ParseResultType.PARTIAL_MATCH;
                    break;
                }

                LeafElementType leafElementType = (LeafElementType) leaf.elementType;
                if (!(leafElementType.isIdentifier() && elementTypes[i].isIdentifier() || leafElementType.isSameAs(elementTypes[i]))) {
                    resultType = i==0 ? ParseResultType.NO_MATCH : ParseResultType.PARTIAL_MATCH;
                    break;
                }

                BasePsiElement separator = leaf.getNextElement();
                if (separator == null) {
                    // if is NOT the last element and no separator found -> consider as partial mach
                    if (i < elementTypes.length -1) {
                        resultType = ParseResultType.PARTIAL_MATCH;
                        break;
                    }
                } else {
                    // if is the last element and still separator found -> not match;
                    if (i == elementTypes.length -1){
                        resultType = ParseResultType.NO_MATCH;
                        break;
                    }
                }
            }
            if (resultType != ParseResultType.NO_MATCH) {
                parseVariants.add(new QualifiedIdentifierVariant(elementTypes, resultType == ParseResultType.PARTIAL_MATCH));
            }
        }
        parseVariants.sort(null);
        return parseVariants;
    }

    private LeafPsiElement lookupParentElementFor(LeafPsiElement element) {
        int index = getIndexOf(element);
        if (index > 0) {
            return getLeafAtIndex(index - 1);
        }
        return null;
    }

    public DBObject lookupParentObjectFor(LeafPsiElement leafPsiElement) {
        LeafPsiElement parent = lookupParentElementFor(leafPsiElement);
        return parent == null ? null : parent.getUnderlyingObject();
    }

    public DBObject lookupParentObjectFor(LeafElementType leafElementType) {
       for (QualifiedIdentifierVariant parseVariant : getParseVariants()) {
           if (parseVariant.getLeafs().length != getElementsCount()) continue;

           int index = parseVariant.getIndexOf(leafElementType);
           if (index <= 0) continue;

           IdentifierPsiElement previousPsiElement = getLeafAtIndex(index-1);
           if (previousPsiElement == null) continue;

           DBObject parentObject = previousPsiElement.getUnderlyingObject();
           if (parentObject != null) return parentObject;
       }
        return null;
    }

    int getElementsCount() {
        int count = 0;
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof LeafPsiElement) {
                LeafPsiElement leafPsiElement = (LeafPsiElement) child;
                if (leafPsiElement.elementType != elementType.getSeparatorToken() ) {
                    count++;
                }
            }
            child = child.getNextSibling();
        }
        return count;
    }
}
