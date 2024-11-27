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

package com.dbn.editor.data.structure;

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Safe;
import com.dbn.language.psql.structure.PSQLStructureViewElement;
import com.intellij.ide.util.treeView.smartTree.ActionPresentation;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Comparator;

public class DatasetEditorStructureViewModelSorter implements Sorter {

    @Override
    public Comparator getComparator() {
        return COMPARATOR;    
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    @NotNull
    public ActionPresentation getPresentation() {
        return ACTION_PRESENTATION;
    }

    @Override
    @NotNull
    public String getName() {
        return "Sort by Name";
    }

    private static final ActionPresentation ACTION_PRESENTATION = new ActionPresentation() {
        @Override
        @NotNull
        public String getText() {
            return "Sort by Name";
        }

        @Override
        public String getDescription() {
            return "Sort elements alphabetically by name";
        }

        @Override
        public Icon getIcon() {
            return Icons.ACTION_SORT_ALPHA;
        }
    };

    private static final Comparator COMPARATOR = new Comparator() {
        @Override
        public int compare(Object object1, Object object2) {
            if (object1 instanceof DatasetEditorStructureViewElement && object2 instanceof DatasetEditorStructureViewElement) {
                DatasetEditorStructureViewElement structureViewElement1 = (DatasetEditorStructureViewElement) object1;
                DatasetEditorStructureViewElement structureViewElement2 = (DatasetEditorStructureViewElement) object2;
                BrowserTreeNode treeNode1 = structureViewElement1.getValue();
                BrowserTreeNode treeNode2 = structureViewElement2.getValue();
                return Safe.compare(
                        treeNode1.getName(),
                        treeNode2.getName());
            } else {
                return object1 instanceof PSQLStructureViewElement ? 1 : -1;
            }
        }
    };
}
