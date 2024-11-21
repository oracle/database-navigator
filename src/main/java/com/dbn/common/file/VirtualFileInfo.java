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

import com.dbn.common.file.util.VirtualFiles;
import com.dbn.common.project.Modules;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;

import javax.swing.Icon;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class VirtualFileInfo {
    private final VirtualFile file;
    private final VirtualFile moduleRoot;
    private final Module module;

    public VirtualFileInfo(VirtualFile file, Project project) {
        this.file = file;
        this.module = ModuleUtil.findModuleForFile(file, project);
        this.moduleRoot = Modules.getModuleContentRoot(module, file);
    }

    public String getPath() {
        return file.getPath();
    }

    public Icon getIcon() {
        return VirtualFiles.getIcon(file);
    }

    public static List<VirtualFileInfo> fromFiles(List<VirtualFile> files, Project project) {
        return files.stream().map(f -> new VirtualFileInfo(f, project)).collect(Collectors.toList());
    }
}
