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

import com.dbn.common.util.Naming;
import com.dbn.object.type.DBObjectType;
import com.intellij.ide.util.treeView.smartTree.Group;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PSQLStructureViewModelGroup implements Group {
    private final DBObjectType objectType;
    private final List<TreeElement> children = new ArrayList<>();


    PSQLStructureViewModelGroup(DBObjectType objectType) {
        this.objectType = objectType;
    }

    public void addChild(TreeElement treeElement) {
        children.add(treeElement);
    }

    @Override
    @NotNull
    public ItemPresentation getPresentation() {
        return itemPresentation;
    }

    @Override
    @NotNull
    public Collection<TreeElement> getChildren() {
        return children;
    }


    private final ItemPresentation itemPresentation = new ItemPresentation(){
        @Override
        public String getPresentableText() {
            return Naming.capitalize(objectType.getListName());
        }

        @Override
        public String getLocationString() {
            return null;
        }

        @Override
        public Icon getIcon(boolean open) {
            return null;//objectType.getListIcon();
        }
    };
}