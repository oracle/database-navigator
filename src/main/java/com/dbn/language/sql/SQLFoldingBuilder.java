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

package com.dbn.language.sql;

import com.dbn.common.consumer.SetCollector;
import com.dbn.common.util.Naming;
import com.dbn.language.common.DBLanguageFoldingBuilder;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.ChameleonPsiElement;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.TokenPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.lang.folding.LanguageFolding;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SQLFoldingBuilder extends DBLanguageFoldingBuilder {

    @Override
    protected void createFoldingDescriptors(@NotNull PsiElement psiElement, Document document, List<FoldingDescriptor> descriptors, int nestingIndex) {
        PsiElement child = psiElement.getFirstChild();
        while (child != null) {
            FoldingContext context = new FoldingContext(descriptors, document, nestingIndex);
            if (child instanceof PsiComment) {
                PsiComment psiComment = (PsiComment) child;
                createCommentFolding(context, psiComment);
            }
            else if (child instanceof ExecutablePsiElement) {
                ExecutablePsiElement executablePsiElement = (ExecutablePsiElement) child;
                TextRange textRange = executablePsiElement.getTextRange();
                if (textRange.getLength() > 10) {
                    ASTNode childNode = executablePsiElement.getNode();
                    FoldingDescriptor descriptor = new FoldingDescriptor(childNode, textRange);
                    context.addDescriptor(descriptor);
                    createFoldingDescriptors(executablePsiElement, document, descriptors, 1);
                }
            } else if (child instanceof ChameleonPsiElement) {
                ChameleonPsiElement chameleonPsiElement = (ChameleonPsiElement) child;
                FoldingDescriptor descriptor = new FoldingDescriptor(
                        chameleonPsiElement.node,
                        chameleonPsiElement.getTextRange());
                context.addDescriptor(descriptor);

                FoldingBuilder foldingBuilder = LanguageFolding.INSTANCE.forLanguage(chameleonPsiElement.getLanguage());
                FoldingDescriptor[] nestedDescriptors = foldingBuilder.buildFoldRegions(chameleonPsiElement.node, document);
                descriptors.addAll(Arrays.asList(nestedDescriptors));

            } else if (child instanceof TokenPsiElement) {
                TokenPsiElement tokenPsiElement = (TokenPsiElement) child;
                createLiteralFolding(context, tokenPsiElement);
            } else if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                createAttributeFolding(context, basePsiElement);

                if (context.nestingIndex < 2) {
                    createFoldingDescriptors(basePsiElement, document, descriptors, 1);
                }
            }

            child = child.getNextSibling();
        }
    }

    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        PsiElement psiElement = node.getPsi();
        if (psiElement instanceof PsiComment) {
            return "/*...*/";
        }
        if (psiElement instanceof BasePsiElement) {
            BasePsiElement basePsiElement = (BasePsiElement) psiElement;
            SetCollector<IdentifierPsiElement> subjects = SetCollector.linked();

            basePsiElement.collectSubjectPsiElements(subjects);
            StringBuilder buffer = new StringBuilder(" ");
            buffer.append(basePsiElement.getSpecificElementType().getDescription());
            if (subjects.isNotEmpty()) {
                buffer.append(" (");
                buffer.append(Naming.createNamesList(subjects.elements(), 3));
                buffer.append(")");
            }
            return buffer.toString();
        }
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }

}
