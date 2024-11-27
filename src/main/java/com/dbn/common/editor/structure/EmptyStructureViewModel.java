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

package com.dbn.common.editor.structure;

import com.intellij.ide.structureView.FileEditorPositionListener;
import com.intellij.ide.structureView.ModelListener;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.Grouper;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class EmptyStructureViewModel implements StructureViewModel {
    public static final StructureViewModel INSTANCE = new EmptyStructureViewModel();

    private EmptyStructureViewModel() {}

    private static final ItemPresentation ROOT_ITEM_PRESENTATION = new ItemPresentation() {
        @Nullable
        @Override
        public String getPresentableText() {
            return null;
        }

        @Nullable
        @Override
        public String getLocationString() {
            return null;
        }

        @Nullable
        @Override
        public Icon getIcon(boolean unused) {
            return null;
        }
    };

    private static final StructureViewTreeElement ROOT_TREE_ELEMENT = new StructureViewTreeElement() {

        
        @Override
        public Object getValue() {
            return null;
        }

        @Override
        public void navigate(boolean requestFocus) {

        }

        @Override
        public boolean canNavigate() {
            return false;
        }

        @Override
        public boolean canNavigateToSource() {
            return false;
        }

        @NotNull
        @Override
        public ItemPresentation getPresentation() {
            return ROOT_ITEM_PRESENTATION;
        }

        @NotNull
        @Override
        public TreeElement[] getChildren() {
            return new TreeElement[0];
        }
    };


    @Nullable
    @Override
    public Object getCurrentEditorElement() {
        return null;
    }

    @Override
    public void addEditorPositionListener(@NotNull FileEditorPositionListener listener) {

    }

    @Override
    public void removeEditorPositionListener(@NotNull FileEditorPositionListener listener) {

    }

    @Override
    public void addModelListener(@NotNull ModelListener modelListener) {

    }

    @Override
    public void removeModelListener(@NotNull ModelListener modelListener) {

    }

    @NotNull
    @Override
    public StructureViewTreeElement getRoot() {
        return ROOT_TREE_ELEMENT;
    }

    @NotNull
    @Override
    public Grouper[] getGroupers() {
        return new Grouper[0];
    }

    @NotNull
    @Override
    public Sorter[] getSorters() {
        return new Sorter[0];
    }

    @NotNull
    @Override
    public Filter[] getFilters() {
        return new Filter[0];
    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean shouldEnterElement(Object element) {
        return false;
    }
}
