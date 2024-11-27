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

package com.dbn.code.common.intention;

import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.SchemaId;
import com.dbn.connection.SessionId;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.assistant.editor.AssistantPrompt.Flavor.COMMENT;
import static com.dbn.assistant.editor.AssistantPrompt.Flavor.SELECTION;
import static com.dbn.connection.ConnectionHandler.isLiveConnection;

public class DatabaseConnectIntentionAction extends EditorIntentionAction {
    @Override
    public EditorIntentionType getType() {
        return EditorIntentionType.CONNECT;
    }

    @Override
    @NotNull
    public String getText() {
        return "Connect to database";
    }


    @Override
    public Icon getIcon(int flags) {
        return Icons.CONNECTION_CONNECTED;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        // do not show the intention in a db-assistant context of type COMMENT or SELECTION
        if (isDatabaseAssistantPrompt(editor, psiElement, COMMENT, SELECTION)) return false;

        PsiFile psiFile = psiElement.getContainingFile();
        if (psiFile instanceof DBLanguagePsiFile) {
            DBLanguagePsiFile dbLanguagePsiFile = (DBLanguagePsiFile) psiFile;
            ConnectionHandler connection = dbLanguagePsiFile.getConnection();
            if (isLiveConnection(connection) && !connection.canConnect() && !connection.isConnected()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PsiFile psiFile = psiElement.getContainingFile();
        if (psiFile instanceof DBLanguagePsiFile) {
            DBLanguagePsiFile dbLanguagePsiFile = (DBLanguagePsiFile) psiFile;
            ConnectionHandler connection = dbLanguagePsiFile.getConnection();
            if (isLiveConnection(connection)) {
                connection.getInstructions().setAllowAutoConnect(true);

                DatabaseSession databaseSession = dbLanguagePsiFile.getSession();
                SessionId sessionId = databaseSession == null ? SessionId.MAIN : databaseSession.getId();
                SchemaId schemaId = dbLanguagePsiFile.getSchemaId();

                ConnectionManager connectionManager = ConnectionManager.getInstance(project);
                ConnectionAction.invoke(null, true, connection,
                        (action) -> connectionManager.testConnection(connection, schemaId, sessionId, false, true));
            }
        }
    }
}
