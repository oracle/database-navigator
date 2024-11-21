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

package com.dbn.language.psql.structure;

import com.dbn.common.icon.Icons;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.NamedPsiElement;
import com.dbn.object.type.DBObjectType;
import com.intellij.ide.util.treeView.smartTree.ActionPresentation;
import com.intellij.ide.util.treeView.smartTree.ActionPresentationData;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class PSQLStructureViewModelFilter implements Filter {
    private final ActionPresentation actionPresentation = new ActionPresentationData("Top Level Elements", "", Icons.TOP_LEVEL_FILTER);

    @Override
    @NotNull
    public ActionPresentation getPresentation() {
        return actionPresentation;
    }

    @Override
    @NotNull
    public String getName() {
        return "Top Level";
    }

    @Override
    public boolean isVisible(TreeElement treeNode) {
        PSQLStructureViewElement structureViewElement = (PSQLStructureViewElement) treeNode;
        PsiElement psiElement = structureViewElement.getPsiElement();
        if (psiElement instanceof NamedPsiElement) {
            NamedPsiElement namedPsiElement = (NamedPsiElement) psiElement;
            boolean isObject = namedPsiElement.is(ElementTypeAttribute.OBJECT_DEFINITION) ||
                    namedPsiElement.is(ElementTypeAttribute.OBJECT_DECLARATION) ||
                    namedPsiElement.is(ElementTypeAttribute.OBJECT_SPECIFICATION);

            if (isObject) {
                BasePsiElement subject = namedPsiElement.findFirstPsiElement(ElementTypeAttribute.SUBJECT);
                if (subject instanceof IdentifierPsiElement) {
                    IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) subject;
                    DBObjectType objectType = identifierPsiElement.getObjectType();
                    if (objectType.matches(DBObjectType.METHOD) || objectType.matches(DBObjectType.PROGRAM)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isReverted() {
        return false;
    }
}
