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

import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.NamedElementTypeLookupCache;
import com.dbn.language.common.element.parser.impl.NamedElementTypeParser;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.language.common.psi.NamedPsiElement;
import com.dbn.language.common.psi.RootPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static com.dbn.common.util.Strings.cachedUpperCase;

@Getter
public final class NamedElementType extends SequenceElementType {
    public final Set<ElementTypeBase> parents;
    private boolean definitionLoaded;
    private boolean truncateOnExecution;

    public NamedElementType(ElementTypeBundle bundle, String id) {
        super(bundle, null, id);
        parents = new HashSet<>();
    }

    @Override
    public NamedElementTypeLookupCache createLookupCache() {
        return new NamedElementTypeLookupCache(this);
    }

    @NotNull
    @Override
    public NamedElementTypeParser createParser() {
        return new NamedElementTypeParser(this);
    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return is(ElementTypeAttribute.ROOT) ? new RootPsiElement(astNode, this) :
               is(ElementTypeAttribute.EXECUTABLE) ? new ExecutablePsiElement(astNode, this) :
                                new NamedPsiElement(astNode, this);
    }

    @Override
    public void loadDefinition(Element def) throws ElementTypeDefinitionException {
        super.loadDefinition(def);
        String description = ElementTypeBundle.determineMandatoryAttribute(def, "description", "Invalid definition of complex element '" + getId() + "'.");
        setDescription(description);
        truncateOnExecution = getBooleanAttribute(def, "truncate-on-execution");

        definitionLoaded = true;
    }

    public void update(NamedElementType elementType) {
        setDescription(elementType.getDescription());
        children = elementType.children;
        definitionLoaded = true;
    }

    @NotNull
    @Override
    public String getName() {
        return cachedUpperCase(getId());
    }

    public void addParent(ElementTypeBase parent) {
        parents.add(parent);
    }
}
