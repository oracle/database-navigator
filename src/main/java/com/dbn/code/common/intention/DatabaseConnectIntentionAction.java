package com.dbn.code.common.intention;

import com.dbn.common.icon.Icons;
import com.dbn.connection.*;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

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
