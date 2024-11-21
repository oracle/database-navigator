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

import com.dbn.language.common.element.ElementType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class BasicCompositeElement extends CompositeElement {

    public BasicCompositeElement(@NotNull IElementType type) {
        super(type);

        // initialise the psi wrapper when node is created
        // (unjustified number of threads invoking createPsiNoLock all over again)
        getPsi();
    }

    @Override
    protected PsiElement createPsiNoLock() {
        IElementType elementType = getElementType();
        if (elementType instanceof ElementType) {
            ElementType et = (ElementType) elementType;
            return et.createPsiElement(this);
        }
        return super.createPsiNoLock();
    }
}
