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

import com.dbn.common.editor.structure.DBObjectStructureViewModel;
import com.dbn.common.ref.WeakRef;
import com.dbn.editor.data.DatasetEditor;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DatasetEditorStructureViewModel extends DBObjectStructureViewModel {
    private final Sorter[] sorters = new Sorter[] {new DatasetEditorStructureViewModelSorter()};
    private final WeakRef<DatasetEditor> datasetEditor;
    private StructureViewTreeElement root;

    public DatasetEditorStructureViewModel(DatasetEditor datasetEditor) {
        this.datasetEditor = WeakRef.of(datasetEditor);
    }

    @NotNull
    public DatasetEditor getDatasetEditor() {
        return WeakRef.ensure(datasetEditor);
    }

    @NotNull
    @Override
    public Sorter[] getSorters() {
        return sorters;
    }

    @Override
    @Nullable
    public Object getCurrentEditorElement() {
        return null;
    }

    @Override
    @NotNull
    public StructureViewTreeElement getRoot() {
        if (root == null) {
            //DBObjectBundle objectBundle = datasetEditor.getCache().getObjectBundle();
            DatasetEditor datasetEditor = getDatasetEditor();
            root = new DatasetEditorStructureViewElement(datasetEditor.getDataset(), datasetEditor);
        }
        return root;
    }
}
