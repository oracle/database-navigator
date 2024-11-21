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

import com.dbn.code.common.style.formatting.FormattingAttributes;
import com.dbn.code.common.style.formatting.FormattingProviderPsiElement;
import com.dbn.code.common.style.formatting.IndentDefinition;
import com.dbn.code.common.style.formatting.SpacingDefinition;
import com.dbn.common.icon.Icons;
import com.dbn.language.common.element.ChameleonElementType;
import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

public class ChameleonPsiElement extends ASTDelegatePsiElement implements ExecutableBundlePsiElement, FormattingProviderPsiElement {
    public static final FormattingAttributes FORMATTING_ATTRIBUTES = new FormattingAttributes(null, IndentDefinition.ABSOLUTE_NONE, SpacingDefinition.MIN_ONE_LINE, null);

    public final ASTNode node;
    public final ChameleonElementType elementType;

    public ChameleonPsiElement(@NotNull ASTNode node, ChameleonElementType elementType) {
        this.node = node;
        this.elementType = elementType;
    }


    @Override
    public PsiElement getParent() {
        ASTNode parentNode = node.getTreeParent();
        return parentNode == null ? null : parentNode.getPsi();
    }

    @Override
    @NotNull
    public ASTNode getNode() {
        return node;
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
    public String toString() {
        return elementType.getName();
    }

    @Override
    public Icon getIcon(int flags) {
        return Icons.FILE_BLOCK_PSQL; // todo make this dynamic
    }

    @Override
    public FormattingAttributes getFormattingAttributes() {
        return FORMATTING_ATTRIBUTES;
    }

    @Override
    public FormattingAttributes getFormattingAttributesRecursive(boolean left) {
        return FORMATTING_ATTRIBUTES;
    }
}
