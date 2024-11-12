package com.dbn.language.common.element.impl;

import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.ElementLookupContext;
import com.dbn.language.common.element.cache.ElementTypeLookupCache;
import com.dbn.language.common.element.cache.SequenceElementTypeLookupCache;
import com.dbn.language.common.element.parser.BranchCheck;
import com.dbn.language.common.element.parser.impl.SequenceElementTypeParser;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.language.common.psi.SequencePsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Unsafe.cast;

public class SequenceElementType extends ElementTypeBase {
    public ElementTypeRef[] children;
    private int exitIndex;
    private boolean basic;

    public ElementTypeRef getFirstChild() {
        // TODO check parser definitions (empty sequence blocks)
        return children.length == 0 ? null : children[0];
    }

    public ElementTypeRef getChild(int index) {
        return children[index];
    }

    public SequenceElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id) {
        super(bundle, parent, id, (String) null);
    }

    public SequenceElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
    }

    @Override
    public SequenceElementTypeLookupCache createLookupCache() {
        return new SequenceElementTypeLookupCache<>(this);
    }

    @NotNull
    @Override
    public SequenceElementTypeParser createParser() {
        return new SequenceElementTypeParser<>(this);
    }

    @Override
    protected void loadDefinition(Element def) throws ElementTypeDefinitionException {
        super.loadDefinition(def);
        String tokenIds = stringAttribute(def, "tokens");
        if (Strings.isNotEmptyOrSpaces(tokenIds)) {
            basic = true;
            String id = getId();
            String[] tokens = tokenIds.split(",");
            children = new ElementTypeRef[tokens.length];
            for (int i=0; i<tokens.length; i++) {
                String tokenTypeId = tokens[i].trim();
                ElementTypeRef previous = i == 0 ? null : children[i-1];

                TokenElementType tokenElementType = new TokenElementType(bundle, this, tokenTypeId, id);
                children[i] = new ElementTypeRef(previous, this, tokenElementType, false, 0, null);
            }
        } else {
            List<Element> children = def.getChildren();
            this.children = new ElementTypeRef[children.size()];

            ElementTypeRef previous = null;
            for (int i = 0; i < children.size(); i++) {
                Element child = children.get(i);
                String type = child.getName();
                ElementTypeBase elementType = bundle.resolveElementDefinition(child, type, this);
                boolean optional = getBooleanAttribute(child, "optional");
                double version = Double.parseDouble(Commons.nvl(stringAttribute(child, "version"), "0"));

                Set<BranchCheck> branchChecks = parseBranchChecks(stringAttribute(child, "branch-check"));
                this.children[i] = new ElementTypeRef(previous, this, elementType, optional, version, branchChecks);
                previous = this.children[i];

                if (stringAttribute(child, "exit") != null) exitIndex = i;
            }
        }

        if (children.length == 1 && !(this instanceof NamedElementType) && !(this instanceof BlockElementType)) {
            // TODO log and / or cleanup
        }
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public int getChildCount() {
        return children.length;
    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return new SequencePsiElement(astNode, this);
    }

    public boolean isExitIndex(int index) {
        return index <= exitIndex;
    }

    @NotNull
    @Override
    public String getName() {
        return "sequence (" + getId() + ")";
    }

    /*********************************************************
     *                Cached lookup helpers                  *
     *********************************************************/
    public boolean containsLandmarkTokenFromIndex(TokenType tokenType, int index) {
        if (index < children.length) {
            ElementTypeRef child = children[index];
            while (child != null) {
                if (child.elementType.cache.couldStartWithToken(tokenType)) return true;
                child = child.getNext();
            }
        }
        return false;
    }

    public Set<TokenType> getFirstPossibleTokensFromIndex(ElementLookupContext context, int index) {
        if (children[index].optional) {
            Set<TokenType> tokenTypes = new HashSet<>();
            for (int i=index; i< children.length; i++) {
                ElementTypeLookupCache lookupCache = children[i].elementType.cache;
                lookupCache.captureFirstPossibleTokens(context.reset(), tokenTypes);
                if (!children[i].optional) break;
            }
            return tokenTypes;
        } else {
            ElementTypeLookupCache lookupCache = children[index].elementType.cache;
            return lookupCache.captureFirstPossibleTokens(context.reset());
        }
    }

    public boolean isPossibleTokenFromIndex(TokenType tokenType, int index) {
        if (index < children.length) {
            for (int i= index; i< children.length; i++) {
                if (children[i].elementType.cache.couldStartWithToken(tokenType)){
                    return true;
                }
                if (!children[i].optional) {
                    return false;
                }
            }
        }
        return false;
    }

    public int indexOf(LeafElementType leafElementType) {
        if (wrapping != null && leafElementType instanceof TokenElementType) {
            TokenElementType tokenElementType = (TokenElementType) leafElementType;
            if (wrapping.endElementType.tokenType == tokenElementType.tokenType) {
                return children.length-1;
            }
        }
        ElementTypeRef child = children[0];
        while (child != null) {
            if (child.elementType == leafElementType || child.elementType.cache.containsLeaf(leafElementType)) {
                return child.getIndex();
            }
            child = child.getNext();
        }

        return -1;
    }

    public int indexOf(ElementType elementType, int fromIndex) {
        if (wrapping != null && elementType instanceof TokenElementType) {
            TokenElementType tokenElementType = (TokenElementType) elementType;
            if (wrapping.endElementType.tokenType == tokenElementType.tokenType) {
                return children.length-1;
            }
        }

        if (fromIndex < children.length) {
            ElementTypeRef child = children[fromIndex];
            while (child != null) {
                if (child.elementType == elementType) {
                    return child.getIndex();
                }
                child = child.getNext();
            }
        }
        return -1;
    }

    public int indexOf(ElementType elementType) {
        return indexOf(elementType, 0);
    }

    @Override
    public void collectLeafElements(Set<LeafElementType> bucket) {
        super.collectLeafElements(bucket);
        if (basic) {
            for (ElementTypeRef child : children) {
                bucket.add(cast(child.elementType));
            }
        }
    }
}
