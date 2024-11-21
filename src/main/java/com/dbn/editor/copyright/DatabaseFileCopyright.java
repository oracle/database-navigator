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

package com.dbn.editor.copyright;

import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.psi.UpdateAnyFileCopyright;

public class DatabaseFileCopyright extends UpdateAnyFileCopyright {
    public DatabaseFileCopyright(Project project, Module module, VirtualFile root, CopyrightProfile options) {
        super(project, module, root, options);
    }

    @Override
    protected boolean accept() {
        return getFile() instanceof DBLanguagePsiFile;
    }

    @Override
    protected void scanFile() {
        super.scanFile();
    }
}
