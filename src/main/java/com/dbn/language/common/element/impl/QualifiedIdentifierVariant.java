package com.dbn.language.common.element.impl;

import com.dbn.language.common.psi.LeafPsiElement;
import com.dbn.language.common.psi.QualifiedIdentifierPsiElement;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Unsafe.cast;

public class QualifiedIdentifierVariant implements Comparable{
    private LeafElementType[] leafs;
    /**@deprecated*/
    private boolean incomplete;
    private int matchedTokens;

    public QualifiedIdentifierVariant(LeafElementType[] leafs, boolean partial) {
        this.leafs = leafs;
        this.incomplete = partial;
    }

    public QualifiedIdentifierVariant(LeafElementType[] leafs, int matchedTokens) {
        this.leafs = leafs;
        this.matchedTokens = matchedTokens;
        this.incomplete = matchedTokens != leafs.length;
    }

    public QualifiedIdentifierVariant replace(LeafElementType[] leafs, int matchedTokens) {
        this.leafs = leafs;
        this.matchedTokens = matchedTokens;
        this.incomplete = matchedTokens != leafs.length;
        return this;
    }

    public LeafElementType[] getLeafs() {
        return leafs;
    }

    public LeafElementType getLeaf(int index) {
        if (index > -1 && index < leafs.length) {
            return leafs[index];
        }
        return null; 
    }

    public int getIndexOf(LeafElementType leafElementType) {
        int index = 0;
        for (LeafElementType leaf : leafs) {
            if (leaf == leafElementType) {
                return index;
            } else {
                index++;
            }
        }
        return -1;
    }

    public int length() {
        return leafs.length;
    }

    public int getMatchedTokens() {
        return matchedTokens;
    }

    public boolean isIncomplete() {
        return incomplete;
    }

    public boolean containsNonIdentifierTokens() {
        for (int i=0; i<matchedTokens; i++) {
            if (!leafs[i].tokenType.isIdentifier()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        QualifiedIdentifierVariant variant = (QualifiedIdentifierVariant) o;
        if (variant.incomplete != incomplete) {
            return variant.incomplete ? -1 : 1;
        }

        if (variant.leafs.length == leafs.length) {
            return leafs[0].getId().compareTo(variant.leafs[0].getId());
        }
        return variant.leafs.length - leafs.length;  
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        for (LeafElementType leaf : leafs) {
            if (leaf instanceof IdentifierElementType) {
                IdentifierElementType identifierElementType = (IdentifierElementType) leaf;
                buffer.append(identifierElementType.getObjectTypeName());
            } else if (leaf instanceof TokenElementType) {
                TokenElementType tokenElementType = (TokenElementType) leaf;
                buffer.append(tokenElementType.tokenType.getValue());
            }
            if (leaf != leafs[leafs.length-1]) {
                buffer.append('.');
            }
        }
        return buffer.toString();
    }

    public boolean matchesPsiElement(QualifiedIdentifierPsiElement psiElement) {
        TokenElementType separatorToken = psiElement.elementType.getSeparatorToken();
        PsiElement child = psiElement.getFirstChild();
        int index = 0;
        while (child != null) {
            if (child instanceof LeafPsiElement) {
                LeafPsiElement leafPsiElement = cast(child);

                if (leafPsiElement.elementType == separatorToken){
                    index++;
                } else {
                    if (leafs.length == index) {
                        // variant is too short for given QualifiedIdentifier
                        return false;
                    }

                    PsiElement reference = leafPsiElement.resolve();
                    LeafElementType leafElementType = cast(leafPsiElement.elementType);
                    if (reference == null) {
                        if (!(leafElementType.isIdentifier() && leafs[index].isIdentifier()) ||
                                !leafElementType.isSameAs(leafs[index])) {
                            if(child.getNextSibling() != null) {
                                return false;
                            }
                        }

                    } else {
                        // element is resolved. Must entirely match variant leaf
                        if (!leafElementType.isSameAs(leafs[index])) {
                             if(child.getNextSibling() != null) {
                                 return false;
                             }
                        }
                    }
                }
            }
            child = child.getNextSibling();
        }
        return true;
    }
}
