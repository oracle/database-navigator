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
import com.dbn.language.common.element.cache.OneOfElementTypeCache;
import com.dbn.language.common.element.extension.OneOfElementTypeExtension;
import com.dbn.language.common.element.parser.BranchCheck;
import com.dbn.language.common.element.parser.impl.OneOfElementTypeParser;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.language.common.psi.SequencePsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;

import java.util.List;
import java.util.Set;

import static com.dbn.common.Linked.linkElements;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.language.common.element.util.ElementTypeAttribute.SYNTHETIC;

@Slf4j
public class OneOfElementType extends ElementTypeBase {
    public ElementTypeRef[] children;
    public boolean basic;
    public boolean unbounded;

    public OneOfElementTypeExtension extension;

    public OneOfElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
    }

    public OneOfElementType(ElementTypeBase parent, String id) {
        super(parent.bundle, parent, id);
    }

    @Override
    protected void loadDefinition(Element def) throws ElementTypeDefinitionException {
        super.loadDefinition(def);
        unbounded = getBooleanAttribute(def, "unbounded");
        String tokenIds = stringAttribute(def, "tokens");
        if (Strings.isNotEmptyOrSpaces(tokenIds)) {
            basic = true;
            String[] tokens = tokenIds.split(",");
            this.children = new ElementTypeRef[tokens.length];

            for (int i=0; i<tokens.length; i++) {
                String tokenTypeId = tokens[i].trim();

                TokenElementType tokenElementType = new TokenElementType(this, tokenTypeId);
                tokenElementType.set(SYNTHETIC, false);
                children[i] = new ElementTypeRef(tokenElementType);
            }
        } else {
            List<Element> children = def.getChildren();
            this.children = new ElementTypeRef[children.size()];
            String languageId = getLanguageDialect().getID();
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
                if ("one-of".equals(type)) {
                    log.warn("DBN - [{}] nested one-of element (one-of = {})", languageId, getId());
                }
                ElementTypeBase elementType = bundle.resolveElementDefinition(child, type, this);
                double version = Double.parseDouble(Commons.nvl(stringAttribute(child, "version"), "0"));
                Set<BranchCheck> branchChecks = parseBranchChecks(stringAttribute(child, "branch-check"));

                this.children[i] = new ElementTypeRef(elementType, false, version, branchChecks);
            }
        }

        if (children == null || children.length == 0) {
            // TODO assert at least 2 children
            throw new ElementTypeDefinitionException("[" + getLanguageDialect().getID() + "] Invalid one-of definition (id=" + getId() + "). Element should contain at least 2 elements.");
        }
        linkElements(children);
    }

    @Override
    public void loadExtension(Element def) {
        if (!"one-of-extension".equals(def.getName())) {
            super.loadExtension(def);
            return;
        }

        extension = new OneOfElementTypeExtension(this, def);
    }

    @Override
    protected OneOfElementTypeCache createLookupCache() {
        return new OneOfElementTypeCache(this);
    }

    @Override
    protected OneOfElementTypeParser createParser() {
        return new OneOfElementTypeParser(this);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public String getName() {
        return "one-of (" + getId() + ")";
    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return new SequencePsiElement<>(astNode, this);
    }

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
}
