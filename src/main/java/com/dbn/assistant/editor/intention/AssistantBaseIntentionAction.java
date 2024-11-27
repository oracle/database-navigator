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

package com.dbn.assistant.editor.intention;

import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.assistant.editor.AssistantEditorAdapter;
import com.dbn.assistant.editor.AssistantEditorUtil;
import com.dbn.assistant.editor.AssistantPrompt;
import com.dbn.code.common.intention.EditorIntentionAction;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import static com.dbn.assistant.editor.AssistantEditorUtil.isAssistantAvailable;
import static com.dbn.assistant.editor.AssistantPromptUtil.isAssistantPromptAvailable;
import static com.dbn.assistant.editor.AssistantPromptUtil.resolveAssistantPrompt;
import static com.dbn.common.dispose.Checks.isNotValid;

/**
 * Intention action stub for DB-Assistant editor intentions
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Dan Cioca (Oracle)
 */
public abstract class AssistantBaseIntentionAction extends EditorIntentionAction {
    @Override
    public final boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        return isAssistantAvailable(editor) && isAssistantPromptAvailable(editor, element);
    }

    @Override
    public final void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        ConnectionHandler connection = AssistantEditorUtil.getConnection(editor);
        if (isNotValid(connection)) return;

        AssistantPrompt prompt = resolveAssistantPrompt(editor, element);
        if (prompt == null) return;

        AssistantEditorAdapter.submitQuery(project, editor, connection.getConnectionId(), prompt.getText(), getAction());
    }

    protected abstract PromptAction getAction();

    protected abstract String getActionName();

    @NotNull
    @Override
    public final String getText() {
        return "Select AI - " + getActionName();
    }
}
