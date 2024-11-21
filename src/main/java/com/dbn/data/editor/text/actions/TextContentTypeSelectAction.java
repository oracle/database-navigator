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

package com.dbn.data.editor.text.actions;

import com.dbn.common.action.BasicAction;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.data.editor.text.ui.TextEditorForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class TextContentTypeSelectAction extends BasicAction {
    private final TextEditorForm editorForm;
    private final TextContentType contentType;

    public TextContentTypeSelectAction(TextEditorForm editorForm, TextContentType contentType) {
        super(contentType.getName(), null, contentType.getIcon());
        this.contentType = contentType;
        this.editorForm = editorForm;
    }

    public TextContentType getContentType() {
        return contentType;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        editorForm.setContentType(contentType);

    }
}
