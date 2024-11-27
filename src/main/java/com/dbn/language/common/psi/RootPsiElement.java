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

package com.dbn.language.common.psi;

import com.dbn.common.util.Cloneable;
import com.dbn.language.common.element.impl.NamedElementType;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

public class RootPsiElement extends NamedPsiElement implements ExecutableBundlePsiElement, Cloneable<RootPsiElement> {

    public RootPsiElement(ASTNode astNode, NamedElementType elementType) {
        super(astNode, elementType);
    }

    @Override
    public List<ExecutablePsiElement> getExecutablePsiElements() {
        List<ExecutablePsiElement> bucket = new ArrayList<>();
        collectExecutablePsiElements(bucket, this);
        return bucket;
    }

    private static void collectExecutablePsiElements(List<ExecutablePsiElement> bucket, PsiElement element) {
        PsiElement child = element.getFirstChild();
        while (child != null) {
            if (child instanceof ExecutablePsiElement) {
                ExecutablePsiElement executablePsiElement = (ExecutablePsiElement) child;
                bucket.add(executablePsiElement);
            } else {
                collectExecutablePsiElements(bucket, child);
            }
            child = child.getNextSibling();
        }
    }

    @Override
    public boolean hasErrors() {
        return false;
    }

    @Override
    public RootPsiElement clone() {
        return (RootPsiElement) super.clone();
    }

    /*********************************************************
     *                    ItemPresentation                   *
     *********************************************************/
    @Override
    public String getPresentableText() {
        return elementType.getDescription();
    }

    @Override
    @Nullable
    public Icon getIcon(boolean open) {
        return super.getIcon(open);
    }

    @Override
    @Nullable
    public TextAttributesKey getTextAttributesKey() {
        return null;
    }
}
