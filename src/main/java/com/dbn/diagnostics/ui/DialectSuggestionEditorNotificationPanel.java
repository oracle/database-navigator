/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dbn.diagnostics.ui;

import com.dbn.common.editor.EditorNotificationPanel;
import com.dbn.common.message.MessageType;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.editor.code.options.CodeEditorGeneralSettings;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.dialect.DBLanguageDialectCache;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Documents.getDocument;
import static com.dbn.common.util.Documents.onDocumentChanged;
import static com.dbn.common.util.Documents.touchDocument;
import static com.dbn.common.util.Documents.whenDocumentsCommitted;
import static com.dbn.common.util.Editors.updateEditorNotifications;
import static com.dbn.common.util.Messages.options;
import static com.dbn.diagnostics.ParserIssueEditorNotificationProvider.dismissDialectNotification;
import static com.dbn.diagnostics.ParserIssueEditorNotificationProvider.isDialectNotificationUpdatePending;
import static com.dbn.diagnostics.ParserIssueEditorNotificationProvider.setDialectNotificationUpdatePending;
import static com.dbn.nls.NlsResources.txt;

public class DialectSuggestionEditorNotificationPanel extends EditorNotificationPanel {
    private final DBLanguagePsiFile psiFile;
    private final DBLanguageDialect suggestedDialect;
    private final VirtualFile contentFile;
    private final Editor editor;

    public DialectSuggestionEditorNotificationPanel(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull FileEditor fileEditor,
            @NotNull DBLanguagePsiFile psiFile,
            @NotNull DBLanguageDialect suggestedDialect) {
        super(project, file, fileEditor, MessageType.WARNING);
        this.psiFile = psiFile;
        this.suggestedDialect = suggestedDialect;
        this.contentFile = psiFile.getVirtualFile();
        this.editor = Editors.getEditor(fileEditor);

        setIcon(AllIcons.Actions.IntentionBulb);
        setText(txt("ntf.diagnostics.text.DialectSuggestion", suggestedDialect.getDisplayName()));
        createActionLabel(txt("ntf.diagnostics.action.UseSuggestedDialect", suggestedDialect.getDisplayName()), this::applySuggestion);
        DBLanguageDialect currentDialect = DBLanguageDialectCache.getSelectedDialect(psiFile);
        if (currentDialect == null) {
            currentDialect = DBLanguageDialectCache.getCurrentDialect(psiFile);
        }
        if (currentDialect != null) {
            createActionLabel(txt("ntf.diagnostics.action.KeepDialect", currentDialect.getDisplayName()), this::keepDialect);
        }
        createActionLabel(txt("app.shared.action.Dismiss"), this::dismiss);

        Document document = getDocument(psiFile);
        if (document != null) {
            onDocumentChanged(document, this, event -> refreshWhenInvalid(project));
        }
    }

    private void refreshWhenInvalid(@NotNull Project project) {
        if (isDialectNotificationUpdatePending(contentFile)) return;
        setDialectNotificationUpdatePending(contentFile, true);

        whenDocumentsCommitted(project, () -> {
            setDialectNotificationUpdatePending(contentFile, false);
            if (isDisposed()) return;
            updateEditorNotifications(project, getFile());
        });
    }

    private void applySuggestion() {
        if (isNotValid(editor)) return;

        Project project = getProject();
        ConnectionHandler connection = psiFile.getConnection();
        if (connection == null) {
            DBLanguageDialectCache.setSelectedDialect(psiFile, suggestedDialect);
        } else {
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
            contextManager.setVirtualConnection(contentFile, databaseType(suggestedDialect));
        }

        touchDocument(editor, true);
        whenDocumentsCommitted(project, () -> {
            if (isNotValid(psiFile)) return;
            updateEditorNotifications(project, contentFile);
        });
    }

    private void keepDialect() {
        DBLanguageDialectCache.keepDialect(psiFile);
        updateEditorNotifications(getProject(), contentFile);
    }

    private void dismiss() {
        Project project = getProject();
        int option = Messages.showConfirmationDialog(
                project,
                txt("msg.diagnostics.title.DismissDialectSuggestion"),
                txt("msg.diagnostics.question.DismissDialectSuggestion"),
                options(
                        txt("app.diagnostics.action.DismissForFile"),
                        txt("app.diagnostics.action.DismissForProject"),
                        txt("msg.shared.button.Cancel")),
                0);

        if (option == 0) {
            dismissDialectNotification(contentFile);
            updateEditorNotifications(project, getFile());
        } else if (option == 1) {
            CodeEditorGeneralSettings.get(project).setShowDialectSuggestionNotifications(false);
            updateEditorNotifications(project, null);
        }
    }

    @NotNull
    private static DatabaseType databaseType(@NotNull DBLanguageDialect dialect) {
        return switch (dialect.getIdentifier()) {
            case ORACLE_SQL, ORACLE_PLSQL -> DatabaseType.ORACLE;
            case MYSQL_SQL, MYSQL_PSQL -> DatabaseType.MYSQL;
            case POSTGRES_SQL, POSTGRES_PSQL -> DatabaseType.POSTGRES;
            case SQLITE_SQL, SQLITE_PSQL -> DatabaseType.SQLITE;
            case ISO92_SQL -> DatabaseType.ISO92;
        };
    }
}
