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

package com.dbn.vfs;

import com.dbn.common.icon.OverlaidIcons;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.ide.FileIconPatcher;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class DBFileIconPatcher implements FileIconPatcher {
    @Override
    public Icon patchIcon(Icon baseIcon, VirtualFile file, int flags, @Nullable Project project) {
        if (file instanceof DBEditableObjectVirtualFile) {
            DBEditableObjectVirtualFile objectFile = (DBEditableObjectVirtualFile) file;
            if (!objectFile.isModified()) return baseIcon;

            return OverlaidIcons.addModifiedOverlay(baseIcon);
        }
        return baseIcon;
    }
}
