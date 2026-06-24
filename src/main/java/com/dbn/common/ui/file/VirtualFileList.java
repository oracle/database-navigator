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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.util.Lists.forEach;

public class VirtualFileList extends MutableObjectList<VirtualFile> {
    private final Set<String> userSelectedPaths = new HashSet<>();

    public VirtualFileList(List<VirtualFile> files) {
        super(new VirtualFileListModel(files));
        setCellRenderer(new VirtualFileListCellRenderer());
        setVisibleRowCount(5);
    }

    public void insertRows() {
        FileChooser.chooseFiles(FileChoosers.multipleFiles(), null, /* parent= */ null,
                (List<VirtualFile> selected) -> addFiles(selected));
    }

    private void addFiles(List<VirtualFile> selected) {
        VirtualFileListModel model = getModel();
        model.addAll(selected);
        forEach(selected, s -> userSelectedPaths.add(s.getPath()));
    }

    public VirtualFileListModel getModel() {
        return (VirtualFileListModel) super.getModel();
    }

    public List<VirtualFile> getFiles() {
        return getElements();
    }

    public boolean isUserSelectedPath(String path) {
        return userSelectedPaths.contains(path);
    }

    public void resetUserSelectedPaths() {
        userSelectedPaths.clear();
    }
}

