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
import com.dbn.language.psql.PSQLFile;
import com.dbn.object.type.DBObjectType;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.smartTree.ActionPresentation;
import com.intellij.ide.util.treeView.smartTree.ActionPresentationData;
import com.intellij.ide.util.treeView.smartTree.Group;
import com.intellij.ide.util.treeView.smartTree.Grouper;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class PSQLStructureViewModelGrouper implements Grouper {
    private final ActionPresentation actionPresentation = new ActionPresentationData("Group by Object Type", "", Icons.ACTION_GROUP);

    private static final Collection<Group> EMPTY_GROUPS = new ArrayList<>(0);

    @NotNull
    @Override
    public Collection<Group> group(@NotNull AbstractTreeNode<?> abstractTreeNode, @NotNull Collection<TreeElement> treeElements) {
        Map<DBObjectType, Group> groups = null;
        if (abstractTreeNode.getValue() instanceof PSQLStructureViewElement) {
            PSQLStructureViewElement structureViewElement = (PSQLStructureViewElement) abstractTreeNode.getValue();
            Object value = structureViewElement.getValue();
            if (value instanceof BasePsiElement || value instanceof PSQLFile) {

                for (TreeElement treeElement : treeElements) {
                    if (treeElement instanceof PSQLStructureViewElement) {
                        PSQLStructureViewElement element = (PSQLStructureViewElement) treeElement;
                        if (element.getValue() instanceof BasePsiElement) {
                            BasePsiElement basePsiElement = (BasePsiElement) element.getValue();
                            if (!basePsiElement.elementType.is(ElementTypeAttribute.ROOT)) {
                                BasePsiElement subjectPsiElement = basePsiElement.findFirstPsiElement(ElementTypeAttribute.SUBJECT);
                                if (subjectPsiElement instanceof IdentifierPsiElement) {
                                    IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) subjectPsiElement;
                                    DBObjectType objectType = identifierPsiElement.getObjectType();
                                    switch (objectType) {
                                        case PACKAGE_PROCEDURE: objectType = DBObjectType.PROCEDURE; break;
                                        case PACKAGE_FUNCTION: objectType = DBObjectType.FUNCTION; break;
                                        case TYPE_PROCEDURE: objectType = DBObjectType.PROCEDURE; break;
                                        case TYPE_FUNCTION: objectType = DBObjectType.FUNCTION; break;
                                    }

                                    if (groups == null) groups = new EnumMap<>(DBObjectType.class);
                                    PSQLStructureViewModelGroup group = (PSQLStructureViewModelGroup) groups.get(objectType);
                                    if (group == null) {
                                        group = new PSQLStructureViewModelGroup(objectType);
                                        groups.put(objectType, group);
                                    }
                                    group.addChild(treeElement);
                                }
                            }
                        }
                    }
                }
            }
        }

        return groups == null ? EMPTY_GROUPS : groups.values();
    }

    @Override
    @NotNull
    public ActionPresentation getPresentation() {
        return actionPresentation;
    }

    @Override
    @NotNull
    public String getName() {
        return "Object Type";
    }
}
