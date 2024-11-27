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

package com.dbn.language.sql.structure;

import com.dbn.common.editor.structure.EmptyStructureViewModel;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiEditorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class SQLStructureViewBuilderFactory implements PsiStructureViewFactory {

    @Override
    public StructureViewBuilder getStructureViewBuilder(@NotNull final PsiFile psiFile) {
        return new TreeBasedStructureViewBuilder() {
            @NotNull
            //@Override TODO older versions support. Decommission
            public StructureViewModel createStructureViewModel() {
                return createStructureViewModel(null);
            }

            @NotNull
            @Override
            public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
                try {
                    return !psiFile.isValid() ||
                            psiFile.getProject().isDisposed() ||
                            PsiEditorUtil.Service.getInstance() == null ?
                            EmptyStructureViewModel.INSTANCE :
                            new SQLStructureViewModel(editor, psiFile);
                } catch (Throwable e) {
                    conditionallyLog(e);
                    // TODO dirty workaround (compatibility issue)
                    return EmptyStructureViewModel.INSTANCE;
                }
            }
        };
    }
}