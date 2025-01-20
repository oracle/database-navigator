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

package com.dbn.common.file.ui;

import com.dbn.common.file.VirtualFileInfo;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.JList;

public class FileListCellRenderer extends ColoredListCellRenderer<VirtualFileInfo> {
    @Override
    protected void customize(@NotNull JList<? extends VirtualFileInfo> list, VirtualFileInfo value, int index, boolean selected, boolean hasFocus) {
        Module module = value.getModule();
        if (module == null) {
            append(value.getPath(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        } else {
            VirtualFile contentRoot = value.getModuleRoot();
            VirtualFile parent = contentRoot == null ? null : contentRoot.getParent();
            int relativePathIndex = parent == null ? 0 : parent.getPath().length();
            String relativePath = value.getPath().substring(relativePathIndex);
            append('[' + module.getName() + ']', SimpleTextAttributes.REGULAR_ATTRIBUTES);
            append(relativePath, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        setIcon(value.getIcon());
    }
}
