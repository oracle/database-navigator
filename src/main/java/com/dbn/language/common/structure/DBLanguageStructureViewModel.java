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

package com.dbn.language.common.structure;

import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.sql.structure.SQLStructureViewElement;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.Grouper;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public abstract class DBLanguageStructureViewModel extends TextEditorBasedStructureViewModel implements StructureViewModel.ElementInfoProvider {

    public DBLanguageStructureViewModel(Editor editor, PsiFile psiFile) {
        super(editor, psiFile);
    }

    @Override
    @NotNull
    protected Class[] getSuitableClasses() {
        return new Class[] {BasePsiElement.class};
    }

    @Override
    @NotNull
    public Grouper[] getGroupers() {
        return Grouper.EMPTY_ARRAY;
    }

    @Override
    @NotNull
    public Sorter[] getSorters() {
        return Sorter.EMPTY_ARRAY;
    }

    @Override
    @NotNull
    public Filter[] getFilters() {
        return Filter.EMPTY_ARRAY;
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        if (element instanceof SQLStructureViewElement) {
            SQLStructureViewElement sqlElement = (SQLStructureViewElement) element;
            if (sqlElement.getPsiElement() instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) sqlElement.getPsiElement();
                BasePsiElement childStructureElement = basePsiElement.findFirstPsiElement(ElementTypeAttribute.STRUCTURE);
                if (childStructureElement == null) {
                    return true;
                }
            }
        }
        return false;
    }
}
