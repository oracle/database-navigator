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

package com.dbn.language.common;

import com.dbn.vfs.DBVirtualFile;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting.FORCE_HIGHLIGHTING;
import static com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting.SKIP_HIGHLIGHTING;

public class DefaultHighlightingSettingProvider extends com.intellij.codeInsight.daemon.impl.analysis.DefaultHighlightingSettingProvider {
    @Override
    public @Nullable FileHighlightingSetting getDefaultSetting(@NotNull Project project, @NotNull VirtualFile file) {
        if (file instanceof DBSourceCodeVirtualFile) return FORCE_HIGHLIGHTING;
        if (file instanceof DBConsoleVirtualFile) return FORCE_HIGHLIGHTING;
        if (file instanceof DBVirtualFile) return SKIP_HIGHLIGHTING;

        return null;
    }
}
