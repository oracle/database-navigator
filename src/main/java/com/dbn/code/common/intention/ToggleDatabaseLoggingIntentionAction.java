package com.dbn.code.common.intention;

import com.dbn.common.dispose.Checks;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.language.common.PsiFileRef;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.dbn.assistant.editor.AssistantPrompt.Flavor.COMMENT;
import static com.dbn.assistant.editor.AssistantPrompt.Flavor.SELECTION;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Editors.isMainEditor;
import static com.dbn.common.util.Files.isDbLanguagePsiFile;
import static com.dbn.connection.ConnectionHandler.isLiveConnection;
import static com.dbn.database.DatabaseFeature.DATABASE_LOGGING;
import static com.dbn.debugger.DatabaseDebuggerManager.isDebugConsole;

public class ToggleDatabaseLoggingIntentionAction extends EditorIntentionAction {
    private PsiFileRef<?> lastChecked;

    @Override
    public EditorIntentionType getType() {
        return EditorIntentionType.TOGGLE_LOGGING;
    }


    @Override
    @NotNull
    public String getText() {
        ConnectionHandler connection = getLastCheckedConnection();
        if (Checks.isValid(connection)) {
            DatabaseCompatibilityInterface compatibility = connection.getCompatibilityInterface();
            String databaseLogName = compatibility.getDatabaseLogName();
            boolean loggingEnabled = connection.isLoggingEnabled();
            if (Strings.isEmpty(databaseLogName)) {
                return loggingEnabled ? "Disable database logging" : "Enable database logging";
            } else {
                return (loggingEnabled ? "Disable logging (" : "Enable logging (") + databaseLogName + ')';
            }
        }

        return "Toggle database logging";
    }

    @Override
    public Icon getIcon(int flags) {
        ConnectionHandler connection = getLastCheckedConnection();
        if (connection != null) {
            return connection.isLoggingEnabled() ? Icons.EXEC_LOG_OUTPUT_DISABLE : Icons.EXEC_LOG_OUTPUT_ENABLE;
        }
        return Icons.EXEC_LOG_OUTPUT_CONSOLE;
    }


    ConnectionHandler getLastCheckedConnection() {
        PsiFile psiFile = PsiFileRef.from(lastChecked);
        if (isNotValid(psiFile)) return null;

        ConnectionHandler connection = getConnection(psiFile);
        if (!supportsLogging(connection)) return null;

        return connection;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (isDatabaseAssistantPrompt(editor, psiElement, COMMENT, SELECTION)) return false;

        PsiFile psiFile = psiElement.getContainingFile();
        if (!isDbLanguagePsiFile(psiFile)) return false;

        VirtualFile file = psiFile.getVirtualFile();
        if (file instanceof DBSourceCodeVirtualFile) return false;
        if (file instanceof VirtualFileWindow) return false;
        if (isDebugConsole(file)) return false;
        if (!isMainEditor(editor)) return false;

        lastChecked = PsiFileRef.of(psiFile);
        ConnectionHandler connection = getConnection(psiFile);
        return supportsLogging(connection);
    }

    private static boolean supportsLogging(ConnectionHandler connection) {
        return isLiveConnection(connection) && DATABASE_LOGGING.isSupported(connection);
    }

    @Override
    public void invoke(@NotNull final Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PsiFile psiFile = psiElement.getContainingFile();
        ConnectionHandler connection = getConnection(psiFile);
        if (DATABASE_LOGGING.isSupported(connection)) {
            connection.setLoggingEnabled(!connection.isLoggingEnabled());
        }
    }
}
