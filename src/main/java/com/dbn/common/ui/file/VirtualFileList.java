/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.ui.file;

import com.dbn.common.ui.list.MutableObjectList;
import com.dbn.common.util.FileChoosers;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

public class VirtualFileList extends MutableObjectList<VirtualFile> {
    public VirtualFileList(List<VirtualFile> files) {
        super(new VirtualFileListModel(files));
        setCellRenderer(new VirtualFileListCellRenderer());
        setVisibleRowCount(5);
    }

    public void insertRows() {
        FileChooser.chooseFiles(FileChoosers.multipleFiles(), null, /* parent= */ null,
                (List<VirtualFile> selected) -> {
                    VirtualFileListModel model = getModel();
                    model.addAll(selected);
                });
    }

    public VirtualFileListModel getModel() {
        return (VirtualFileListModel) super.getModel();
    }

    public List<VirtualFile> getFiles() {
        return getElements();
    }
}


