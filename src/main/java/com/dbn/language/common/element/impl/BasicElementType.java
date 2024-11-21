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
import com.dbn.language.common.element.cache.BasicElementTypeLookupCache;
import com.dbn.language.common.element.cache.ElementTypeLookupCache;
import com.dbn.language.common.element.parser.impl.BasicElementTypeParser;
import com.dbn.language.common.psi.UnknownPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class BasicElementType extends ElementTypeBase {

    protected BasicElementType(ElementTypeBundle bundle, String id, String description) {
        super(bundle, null, id, description);
    }

    @Override
    public ElementTypeLookupCache<?> createLookupCache() {
        return new BasicElementTypeLookupCache(this);
    }

    @NotNull
    @Override
    public BasicElementTypeParser createParser() {
        return new BasicElementTypeParser(this);
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @NotNull
    @Override
    public String getName() {
        return getId();
    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return new UnknownPsiElement(astNode, this);
    }

}
