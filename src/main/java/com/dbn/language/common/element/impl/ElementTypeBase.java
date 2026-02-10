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

import com.dbn.code.common.style.formatting.FormattingDefinition;
import com.dbn.code.common.style.formatting.FormattingDefinitionFactory;
import com.dbn.code.common.style.formatting.IndentDefinition;
import com.dbn.code.common.style.formatting.SpacingDefinition;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Strings;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.TokenPairTemplate;
import com.dbn.language.common.element.cache.ElementTypeLookupCache;
import com.dbn.language.common.element.parser.Branch;
import com.dbn.language.common.element.parser.BranchCheck;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.path.LanguageNodeBase;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.element.util.ElementTypeAttributeHolder;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.object.type.DBObjectType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.ICompositeElementType;
import com.intellij.psi.tree.IElementType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;

import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.language.common.element.util.ElementTypeAttribute.SCOPE_DEMARCATION;
import static com.dbn.language.common.element.util.ElementTypeAttribute.SCOPE_ISOLATION;
import static com.dbn.language.common.element.util.ElementTypeAttribute.STATEMENT;
import static com.dbn.language.common.element.util.ElementTypeAttribute.WRAPPING_TOKEN;

@Slf4j
@Getter
@Setter
public abstract class ElementTypeBase extends IElementType implements ElementType, ICompositeElementType {
    private static final FormattingDefinition STATEMENT_FORMATTING = new FormattingDefinition(null, IndentDefinition.NORMAL, SpacingDefinition.MIN_LINE_BREAK, null);

    private final int hashCode;

    public final String id;
    public String description;
    public Icon icon;
    public Branch branch;
    public FormattingDefinition formatting;

    public ElementTypeLookupCache<?> cache = createLookupCache();
    public final ElementTypeParser parser = createParser();
    public final ElementTypeBundle bundle;
    public ElementTypeBase parent;
    public DBObjectType virtualObjectType;
    public WrappingDefinition wrapping;
    private ElementTypeAttributeHolder attributes;

    public boolean scopeDemarcation;
    public boolean scopeIsolation;
    public boolean surrogate;
    protected transient boolean initialized;


    @Override
    public @NotNull ASTNode createCompositeNode() {
        return new BasicCompositeElement(this);
    }

    ElementTypeBase(@NotNull ElementTypeBundle bundle, ElementTypeBase parent, String id) {
        super(id, bundle.getLanguageDialect(), false);
        this.id = id.intern();
        this.hashCode = System.identityHashCode(this);
        this.bundle = bundle;
        this.parent = parent;
    }

    ElementTypeBase(@NotNull ElementTypeBundle bundle, ElementTypeBase parent, String id, @NotNull Element def) throws ElementTypeDefinitionException {
        super(id, bundle.getLanguageDialect(), false);
        String defId = stringAttribute(def, "id");
        this.hashCode = System.identityHashCode(this);
        if (!Objects.equals(id, defId)) {
            defId = id;
            def.setAttribute("id", defId);
            bundle.markIdsDirty();
        }
        this.id = defId.intern();
        this.bundle = bundle;
        this.parent = parent;
        if (Strings.isNotEmpty(stringAttribute(def,"exit")) && !(parent instanceof SequenceElementType)) {
            log.warn('[' + getLanguageDialect().getID() + "] Invalid element attribute 'exit'. (id=" + this.id + "). Attribute is only allowed for direct child of sequence element");
        }
        loadDefinition(def);
    }

    Set<BranchCheck> parseBranchChecks(String definitions) {
        Set<BranchCheck> branches = null;
        if (definitions != null) {
            branches = new HashSet<>();
            StringTokenizer tokenizer = new StringTokenizer(definitions, " ");
            while (tokenizer.hasMoreTokens()) {
                String branchDef = tokenizer.nextToken().trim();
                branches.add(new BranchCheck(branchDef));
            }
        }
        return branches;
    }

    public void initialize() {
        this.initialized = true;
    }

    public String nextChildId() {
        return ElementTypeIdCache.nextId(this);
    }

    public boolean isWrappingBegin(LeafElementType elementType) {
        return wrapping != null && wrapping.beginElementType == elementType;
    }

    @Override
    public boolean isWrappingBegin(TokenType tokenType) {
        return wrapping != null && wrapping.beginElementType.tokenType == tokenType;
    }

    public boolean isWrappingEnd(LeafElementType elementType) {
        return wrapping != null && wrapping.endElementType == elementType;
    }

    @Override
    public boolean isWrappingEnd(TokenType tokenType) {
        return wrapping != null && wrapping.endElementType.tokenType == tokenType;
    }

    protected abstract ElementTypeLookupCache<?> createLookupCache();

    @NotNull
    protected abstract ElementTypeParser createParser();

    @Override
    public void setDefaultFormatting(FormattingDefinition defaultFormatting) {
        formatting = FormattingDefinitionFactory.mergeDefinitions(formatting, defaultFormatting);
    }

    protected void loadDefinition(Element def) throws ElementTypeDefinitionException {
        String attributesString = stringAttribute(def, "attributes");
        if (Strings.isNotEmptyOrSpaces(attributesString)) {
            attributes =  new ElementTypeAttributeHolder(attributesString);
        }

        String objectTypeName = stringAttribute(def, "virtual-object");
        if (objectTypeName != null) {
            virtualObjectType = ElementTypeBundle.resolveObjectType(objectTypeName);
        }
        formatting = FormattingDefinitionFactory.loadDefinition(def);
        if (is(STATEMENT)) {
            setDefaultFormatting(STATEMENT_FORMATTING);
        }

        String iconKey = stringAttribute(def, "icon");
        if (iconKey != null)  icon = Icons.getIcon(iconKey);

        String branchDef = stringAttribute(def, "branch");
        if (branchDef != null) {
            branch = new Branch(branchDef);
        }

        loadWrappingAttributes(def);
    }

    private void loadWrappingAttributes(Element def) {
        String optionalWrapping = stringAttribute(def, "optional-wrapping");
        TokenElementType beginTokenElement = null;
        TokenElementType endTokenElement = null;
        if (Strings.isEmpty(optionalWrapping)) {
            String beginTokenId = stringAttribute(def, "wrapping-begin-token");
            String endTokenId = stringAttribute(def, "wrapping-end-token");

            if (Strings.isNotEmpty(beginTokenId) && Strings.isNotEmpty(endTokenId)) {
                beginTokenElement = new TokenElementType(this, beginTokenId);
                endTokenElement = new TokenElementType(this, endTokenId);
            }
        } else {
            TokenPairTemplate template = TokenPairTemplate.valueOf(optionalWrapping);
            String beginTokenId = template.getBeginToken();
            String endTokenId = template.getEndToken();
            beginTokenElement = new TokenElementType(this, beginTokenId);
            endTokenElement = new TokenElementType(this, endTokenId);

            if (template.isBlock()) {
                beginTokenElement.setDefaultFormatting(FormattingDefinition.LINE_BREAK_AFTER);
                endTokenElement.setDefaultFormatting(FormattingDefinition.LINE_BREAK_BEFORE);
                setDefaultFormatting(FormattingDefinition.LINE_BREAK_BEFORE);
            }
        }

        if (beginTokenElement != null) {
            beginTokenElement.set(WRAPPING_TOKEN, true);
            endTokenElement.set(WRAPPING_TOKEN, true);
            wrapping = new WrappingDefinition(beginTokenElement, endTokenElement);
        }

        scopeDemarcation = is(SCOPE_DEMARCATION) || is(STATEMENT);
        scopeIsolation = is(SCOPE_ISOLATION);
    }

    @Override
    public boolean is(ElementTypeAttribute attribute) {
        return attributes != null && attributes.is(attribute);
    }

    @Override
    public boolean set(ElementTypeAttribute attribute, boolean value) {
        if (attributes == null) {
            attributes =  new ElementTypeAttributeHolder();
        }
        return attributes.set(attribute, value);
    }

    @Override
    @NotNull
    public DBLanguage getLanguage() {
        return getLanguageDialect().getBaseLanguage();
    }

    @Override
    public DBLanguageDialect getLanguageDialect() {
        return (DBLanguageDialect) super.getLanguage();
    }

    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public int getIndexInParent(LanguageNodeBase node) {
        LanguageNodeBase parentNode = (LanguageNodeBase) node.parent;
        if (parentNode != null && parentNode.element instanceof SequenceElementType sequenceElementType) {
            return sequenceElementType.indexOf(this);
        }
        return 0;
    }


    /*********************************************************
     *                  Virtual Object                       *
     *********************************************************/
    @Override
    public boolean isVirtualObject() {
        return virtualObjectType != null;
    }

    protected boolean getBooleanAttribute(Element element, @NonNls String attributeName) {
        String attributeValue = stringAttribute(element, attributeName);
        if (Strings.isNotEmpty(attributeValue)) {
            if (Objects.equals(attributeValue, "true")) return true;
            if (Objects.equals(attributeValue, "false")) return false;
            log.warn('[' + getLanguageDialect().getID() + "] Invalid element boolean attribute '" + attributeName + "' (id=" + this.id + "). Expected 'true' or 'false'");
        }
        return false;
    }

    protected boolean isMarkedOptional(Element element) {
        return getBooleanAttribute(element, "optional");
    }

    @Override
    public TokenType getTokenType() {
        return null;
    }

    public void collectAnonymousLeafs(Set<LeafElementType> bucket) {
        if (wrapping == null) return;

        bucket.add(wrapping.beginElementType);
        bucket.add(wrapping.beginElementType);
    }

    public void changeParent(ElementTypeBase oldParent, ElementTypeBase newParent) {
        this.parent = newParent;
    }
}
