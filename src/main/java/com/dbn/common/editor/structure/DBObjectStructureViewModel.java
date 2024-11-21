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

import com.dbn.common.ui.util.Listeners;
import com.intellij.ide.structureView.FileEditorPositionListener;
import com.intellij.ide.structureView.ModelListener;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.Grouper;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import org.jetbrains.annotations.NotNull;

public abstract class DBObjectStructureViewModel implements StructureViewModel, StructureViewModel.ElementInfoProvider {
    protected Listeners<FileEditorPositionListener> fileEditorPositionListeners = Listeners.create(this);
    protected Listeners<ModelListener> modelListeners = Listeners.create(this);

    @Override
    public void addEditorPositionListener(@NotNull FileEditorPositionListener listener) {
        fileEditorPositionListeners.add(listener);
    }

    @Override
    public void removeEditorPositionListener(@NotNull FileEditorPositionListener listener) {
        fileEditorPositionListeners.remove(listener);
    }

    @Override
    public void addModelListener(@NotNull ModelListener modelListener) {
        modelListeners.add(modelListener);
    }

    @Override
    public void removeModelListener(@NotNull ModelListener modelListener) {
        modelListeners.remove(modelListener);
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean shouldEnterElement(Object o) {
        return false;
    }

    @Override
    @NotNull
    public Grouper[] getGroupers() {
        return Grouper.EMPTY_ARRAY;
    }

    @Override
    @NotNull
    public Sorter[] getSorters() {
        return new Sorter[0];
    }

    @Override
    @NotNull
    public Filter[] getFilters() {
        return new Filter[0];
    }

    public void rebuild() {
        modelListeners.notify(l -> l.onModelChanged());
        fileEditorPositionListeners.notify(l -> l.onCurrentElementChanged());
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        // model seems to be able to derive "is leaf" logic from the absence of children
        // (but only if model is extending ElementInfoProvider)
        return false;
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }
}
