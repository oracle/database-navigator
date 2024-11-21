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

package com.dbn.common.project;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class Modules {

    @Nullable
    public static VirtualFile getModuleContentRoot(@Nullable Module module, VirtualFile file) {
        if (module == null) return null;

        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        VirtualFile[] contentRoots = rootManager.getContentRoots();

        while (file != null) {
            file = file.getParent();
            for (VirtualFile contentRoot : contentRoots) {
                if (contentRoot.equals(file)) {
                    return contentRoot;
                }
            }
        }
        return null;
    }
}
