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

package com.dbn.common.presentation.provider;

import com.intellij.psi.PsiFile;

import javax.swing.Icon;

public class PsiFilePresentationProvider extends PresentationProviderBase<PsiFile> {


    public PsiFilePresentationProvider() {
        super(PsiFile.class);
    }

    @Override
    public String getName(PsiFile object) {
        return object.getVirtualFile().getPath();
    }

    @Override
    public String getTypeName(PsiFile object) {
        return object.getFileType().getName() + " file";
    }

    @Override
    public Icon getIcon(PsiFile object) {
        return object.getIcon(0);
    }
}
