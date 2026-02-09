/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.language.common.element.impl;

import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.ElementLookupContext;
import com.dbn.language.common.element.cache.ElementTypeLookupCache;
import com.dbn.language.common.element.cache.ElementTypeLookupCacheIndexed;
import com.dbn.language.common.element.cache.SequenceElementTypeLookupCache;
import com.dbn.language.common.element.parser.BranchCheck;
import com.dbn.language.common.element.parser.impl.SequenceElementTypeParser;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.language.common.psi.SequencePsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.Linked.linkElements;
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

    void setElements(Collection<? extends ElementTypeBase> elements) {
        children = new ElementTypeRef[elements.size()];

        int index = 0;
        for (ElementTypeBase element : elements) {
            children[index] = new ElementTypeRef(element);
            index++;
        }
        linkElements(children);
        initLookupCache();
    }

    @SuppressWarnings("unchecked")
    private void initLookupCache() {
        ElementTypeLookupCacheIndexed cache = (ElementTypeLookupCacheIndexed) this.cache;
        for (ElementTypeRef child : children) {
            ElementTypeBase elementType = child.elementType;
            ElementTypeLookupCache<?> elementTypeCache = elementType.cache;
            cache.allPossibleTokens.addAll(elementTypeCache.getAllPossibleTokens());
        }

        ElementTypeLookupCache<?> elementTypeCache = children[0].elementType.cache;
        cache.firstPossibleLeafs.addAll(elementTypeCache.getFirstPossibleLeafs());
        cache.firstRequiredLeafs.addAll(elementTypeCache.getFirstRequiredLeafs());
        cache.firstPossibleTokens.addAll(elementTypeCache.getFirstPossibleTokens());
        cache.firstRequiredTokens.addAll(elementTypeCache.getFirstRequiredTokens());

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
            String[] tokens = tokenIds.split(",");
            children = new ElementTypeRef[tokens.length];
            for (int i=0; i<tokens.length; i++) {
                String tokenTypeId = tokens[i].trim();

                TokenElementType tokenElementType = new TokenElementType(bundle, this, tokenTypeId);
                children[i] = new ElementTypeRef(tokenElementType);
            }
        } else {
            List<Element> children = def.getChildren();
            this.children = new ElementTypeRef[children.size()];

            for (int i = 0; i < children.size(); i++) {
                Element child = children.get(i);
                String type = child.getName();
                ElementTypeBase elementType = bundle.resolveElementDefinition(child, type, this);
                boolean optional = getBooleanAttribute(child, "optional");
                double version = Double.parseDouble(Commons.nvl(stringAttribute(child, "version"), "0"));

                Set<BranchCheck> branchChecks = parseBranchChecks(stringAttribute(child, "branch-check"));
                this.children[i] = new ElementTypeRef(elementType, optional, version, branchChecks);

                if (stringAttribute(child, "exit") != null) exitIndex = i;
            }
        }

        linkElements(children);

        if (children.length == 1 && !(this instanceof NamedElementType) && !(this instanceof BlockElementType)) {
            // TODO log and / or cleanup
        }
    }

    @Override
    public boolean isLeaf() {
        return false;
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
                child = child.next;
            }
        }
        return false;
    }

    public Set<TokenType> getFirstPossibleTokensFromIndex(ElementLookupContext context, int index) {
        if (children[index].optional) {
            Set<TokenType> tokenTypes = new HashSet<>();
            for (int i=index; i< children.length; i++) {
                ElementTypeLookupCache<?> lookupCache = children[i].elementType.cache;
                lookupCache.captureFirstPossibleTokens(context.reset(), tokenTypes);
                if (!children[i].optional) break;
            }
            return tokenTypes;
        } else {
            ElementTypeLookupCache<?> lookupCache = children[index].elementType.cache;
            return lookupCache.captureFirstPossibleTokens(context.reset());
        }
    }

    public int indexOf(ElementType elementType, int fromIndex) {
        if (wrapping != null && elementType instanceof TokenElementType tokenElementType) {
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
                child = child.next;
            }
        }
        return -1;
    }

    public int indexOf(ElementType elementType) {
        return indexOf(elementType, 0);
    }

    @Override
    public void collectAnonymousLeafs(Set<LeafElementType> bucket) {
        super.collectAnonymousLeafs(bucket);
        if (basic) {
            for (ElementTypeRef child : children) {
                bucket.add(cast(child.elementType));
            }
        }
    }

    public void initialize() {
        if (initialized) return;
        initialized = true;

        // rebuild children before this
        for (ElementTypeRef child : children) {
            child.elementType.initialize();
        }
    }
}
