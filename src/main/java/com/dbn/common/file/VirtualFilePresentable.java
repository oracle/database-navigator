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

package com.dbn.common.file;

import com.dbn.common.ui.Presentable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VirtualFilePresentable implements Presentable {
    private final VirtualFileRef file;

    public VirtualFilePresentable(VirtualFile file) {
        this.file = VirtualFileRef.of(file);
    }

    public static List<VirtualFilePresentable> fromFiles(VirtualFile[] files) {
        return Arrays.stream(files).map(f -> new VirtualFilePresentable(f)).collect(Collectors.toList());
    }

    public static List<VirtualFilePresentable> fromFiles(List<VirtualFile> files) {
        return files.stream().map(f -> new VirtualFilePresentable(f)).collect(Collectors.toList());
    }

    @Nullable
    public VirtualFile getFile() {
        return VirtualFileRef.get(file);
    }

    @Override
    public @NotNull String getName() {
        VirtualFile file = getFile();
        return file == null ? "UNDEFINED" : file.getPath();
    }

    @Override
    public @Nullable Icon getIcon() {
        VirtualFile file = getFile();
        if (file == null) return null;

        return file.isDirectory() ? AllIcons.Nodes.Folder : file.getFileType().getIcon();
    }
}
