/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.common.ui.misc;

import com.dbn.common.file.VirtualFilePresentable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class ContentRootSelector extends DBNComboBox<VirtualFilePresentable> {
    public void setContentRoots(VirtualFile[] contentRoots) {
        initComboBox(this, VirtualFilePresentable.fromFiles(contentRoots));
    }

    public void setContentRoots(List<VirtualFile> contentRoots) {
        initComboBox(this, VirtualFilePresentable.fromFiles(contentRoots));
    }

    public void setSelectedPath(@Nullable String path) {
        VirtualFilePresentable selection = null;
        for (int i = 0; i < getItemCount(); i++) {
            VirtualFilePresentable item = getItemAt(i);
            if (item != null && item.getName().equals(path)) {
                selection = item;
                break;
            }
        }

        if (selection == null && path != null && getItemCount() > 0) {
            selection = getItemAt(0);
        }
        setSelection(this, selection);
    }

    @Nullable
    public String getSelectedPath() {
        VirtualFilePresentable selection = getSelection(this);
        return selection == null ? null : selection.getName();
    }
}
