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

import com.dbn.common.util.Commons;
import com.dbn.language.common.DBLanguageFileType;
import com.dbn.vfs.file.DBObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.lang.Language;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.FileViewProviderFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

public class DatabaseFileViewProviderFactory implements FileViewProviderFactory{

    @Override
    @NotNull
    public FileViewProvider createFileViewProvider(@NotNull VirtualFile file, Language language, @NotNull PsiManager manager, boolean eventSystemEnabled) {

        if (file instanceof DBObjectVirtualFile ||
                file instanceof DBSourceCodeVirtualFile ||
                ((file instanceof DBVirtualFile || file instanceof LightVirtualFile) && file.getFileType() instanceof DBLanguageFileType)) {

            if (file instanceof DBVirtualFile) {
                DBVirtualFile virtualFile = (DBVirtualFile) file;

                return Commons.nvl(virtualFile.getCachedViewProvider(),
                        () -> createViewProvider(
                                file,
                                language,
                                manager,
                                eventSystemEnabled));
            } else {
                return createViewProvider(
                        file,
                        language,
                        manager,
                        eventSystemEnabled);
            }
        } else{
            return new SingleRootFileViewProvider(manager, file, eventSystemEnabled);
        }
    }

    @NotNull
    private DatabaseFileViewProvider createViewProvider(@NotNull VirtualFile file, Language language, @NotNull PsiManager manager, boolean eventSystemEnabled) {
        return new DatabaseFileViewProvider(manager.getProject(), file, eventSystemEnabled, language);
    }
}
