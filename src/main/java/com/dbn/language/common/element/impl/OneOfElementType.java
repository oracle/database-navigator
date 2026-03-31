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
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.ElementTypeCache;
import com.dbn.language.common.element.cache.ElementTypeIndexedCache;
import com.dbn.language.common.element.cache.OneOfElementTypeCache;
import com.dbn.language.common.element.parser.BranchCheck;
import com.dbn.language.common.element.parser.impl.OneOfElementTypeParser;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.language.common.psi.SequencePsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.dbn.common.Linked.linkElements;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.language.common.TokenTypeCategory.CHARACTER;
import static com.dbn.language.common.TokenTypeCategory.IDENTIFIER;
import static com.dbn.language.common.element.impl.OneOfElementTypeBuilder.rebuildAmbiguousPaths;

@Slf4j
public class OneOfElementType extends ElementTypeBase {
    public ElementTypeRef[] children;
    public boolean basic;
    public boolean sortable;
    public boolean ambiguous;

    public OneOfElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
    }

    public OneOfElementType(ElementTypeBase parent, String id) {
        super(parent.bundle, parent, id);
    }

    void setElements(Collection<? extends ElementTypeBase> elements) {
        children = new ElementTypeRef[elements.size()];

        int index = 0;
        for (ElementTypeBase element : elements) {
            element.parent = this;
            children[index] = new ElementTypeRef(element);
            index++;
        }
        linkElements(children);
        initLookupCache();
    }

    @SuppressWarnings("unchecked")
    private void initLookupCache() {
        ElementTypeIndexedCache cache = (ElementTypeIndexedCache) this.cache;
        for (ElementTypeRef child : children) {
            ElementTypeBase elementType = child.elementType;
            ElementTypeCache<?> elementTypeCache = elementType.cache;
            cache.firstPossibleLeafs.addAll(elementTypeCache.getFirstPossibleLeafs());
            cache.firstRequiredLeafs.addAll(elementTypeCache.getFirstRequiredLeafs());
            cache.allPossibleTokens.addAll(elementTypeCache.getAllPossibleTokens());
            cache.firstPossibleTokens.addAll(elementTypeCache.getFirstPossibleTokens());
            cache.firstRequiredTokens.addAll(elementTypeCache.getFirstRequiredTokens());
        }
    }

    @Override
    protected void loadDefinition(Element def) throws ElementTypeDefinitionException {
        super.loadDefinition(def);
        String tokenIds = stringAttribute(def, "tokens");
        if (Strings.isNotEmptyOrSpaces(tokenIds)) {
            basic = true;
            String[] tokens = tokenIds.split(",");
            this.children = new ElementTypeRef[tokens.length];

            for (int i=0; i<tokens.length; i++) {
                String tokenTypeId = tokens[i].trim();

                TokenElementType tokenElementType = new TokenElementType(this, tokenTypeId);
                children[i] = new ElementTypeRef(tokenElementType);
            }
            sortable = false;
        } else {
            List<Element> children = def.getChildren();
            this.children = new ElementTypeRef[children.size()];
            String languageId = getLanguage().getID();
            if (this.children.length == 0) {
                log.warn("DBN - [{}] empty one-of element (one-of = {})", languageId, getId());
            } else if  (this.children.length == 1) {
                log.warn("DBN - [{}] single-child one-of element (one-of = {})", languageId, getId());
            }

            for (int i=0; i<children.size(); i++) {
                Element child = children.get(i);
                if (isMarkedOptional(child)) {
                    // not supported - prevent false expectations
                    log.warn("DBN - [{}] one-of element cannot be optional (one-of = {})", languageId, getId());
                }

                String type = child.getName();
                ElementTypeBase elementType = bundle.resolveElementDefinition(child, type, this);
                double version = Double.parseDouble(Commons.nvl(stringAttribute(child, "version"), "0"));
                Set<BranchCheck> branchChecks = parseBranchChecks(stringAttribute(child, "branch-check"));

                this.children[i] = new ElementTypeRef(elementType, false, version, branchChecks);
            }
            sortable = getBooleanAttribute(def, "sortable");
            ambiguous = getBooleanAttribute(def, "ambiguous");
        }

        if (children == null || children.length == 0) {
            // TODO assert at least 2 children
            throw new ElementTypeDefinitionException("[" + getLanguageDialect().getID() + "] Invalid one-of definition (id=" + getId() + "). Element should contain at least 2 elements.");
        }
        linkElements(children);
    }

    @Override
    protected OneOfElementTypeCache createLookupCache() {
        return new OneOfElementTypeCache(this);
    }

    @NotNull
    @Override
    protected OneOfElementTypeParser createParser() {
        return new OneOfElementTypeParser(this);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @NotNull
    @Override
    public String getName() {
        return "one-of (" + getId() + ")";
    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return new SequencePsiElement<>(astNode, this);
    }

    public void sortChildren() {
        if (!sortable) return;

        Arrays.sort(children, ONE_OF_COMPARATOR);
        linkElements(children);
    }

    private static final Comparator<ElementTypeRef> ONE_OF_COMPARATOR = (o1, o2) -> {
        int i1 = o1.elementType.cache.startsWith(IDENTIFIER) ? 1 :
                 o1.elementType.cache.startsWith(CHARACTER) ? 2 : 3;

        int i2 = o2.elementType.cache.startsWith(IDENTIFIER) ? 1 :
                 o2.elementType.cache.startsWith(CHARACTER) ? 2 : 3;

        return i2-i1;
    };

    public ElementTypeRef getFirstChild() {
        return children[0];
    }

    @Override
    public void collectAnonymousLeafs(Set<LeafElementType> bucket) {
        super.collectAnonymousLeafs(bucket);
        if (!basic) return;

        for (ElementTypeRef child : children) {
            bucket.add((LeafElementType) child.elementType);
        }
    }

    private void initChildren() {
        for (ElementTypeRef child : children) {
            child.elementType.initialize();
        }
    }

    public void initialize() {
        if (initialized) return;
        initialized = true;

        // initialize children before this
        initChildren();
        sortChildren();

        rebuildAmbiguousPaths(this);
    }
}
