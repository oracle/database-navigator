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

package com.dbn.editor.data.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ContextAction;
import com.dbn.editor.data.DatasetEditor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@BackgroundUpdate
@NoArgsConstructor
abstract class AbstractDataEditorAction extends ContextAction<DatasetEditor> {

    protected AbstractDataEditorAction(String text) {
        super(text);
    }

    @Override
    protected DatasetEditor getContext(@NotNull AnActionEvent e) {
        return DatasetEditor.get(e);
    }
}
