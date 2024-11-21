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

package com.dbn.editor.session.ui;

import com.dbn.common.action.BasicAction;
import com.dbn.common.action.ToggleAction;
import com.dbn.common.action.UserDataKeys;
import com.dbn.common.color.Colors;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.exception.OutdatedContentException;
import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.PooledThread;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Borderless;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Strings;
import com.dbn.common.util.Viewers;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.editor.session.SessionBrowser;
import com.dbn.editor.session.SessionBrowserManager;
import com.dbn.editor.session.model.SessionBrowserModelRow;
import com.dbn.editor.session.ui.table.SessionBrowserTable;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.PsiFileRef;
import com.dbn.language.sql.SQLFileType;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.DBSchema;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.dbn.vfs.file.DBSessionStatementVirtualFile;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.util.concurrent.atomic.AtomicReference;

public class SessionBrowserCurrentSqlPanel extends DBNFormBase {
    private JPanel actionsPanel;
    private JPanel viewerPanel;
    private JPanel mainPanel;

    private final WeakRef<SessionBrowser> sessionBrowser;
    private PsiFileRef<DBLanguagePsiFile> psiFile;
    private DBSessionStatementVirtualFile virtualFile;
    private Document document;
    private EditorEx viewer;
    private Object selectedSessionId;

    private final AtomicReference<PooledThread> refreshHandle = new AtomicReference<>();


    SessionBrowserCurrentSqlPanel(DBNComponent parent, SessionBrowser sessionBrowser) {
        super(parent);
        this.sessionBrowser = WeakRef.of(sessionBrowser);
        createStatementViewer();

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, "", true, new RefreshAction(), new WrapUnwrapContentAction());
        actionsPanel.add(actionToolbar.getComponent(),BorderLayout.WEST);
        actionsPanel.setBorder(Borders.lineBorder(JBColor.border(), 0, 0, 1, 0));
    }

    @NotNull
    public SessionBrowser getSessionBrowser() {
        return sessionBrowser.ensure();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    private void setPreviewText(String text) {
        Documents.setText(document, text);
    }

    private void setSchemaId(SchemaId schemaId) {
        getVirtualFile().setSchemaId(schemaId);
    }

    @NotNull
    public DBSessionStatementVirtualFile getVirtualFile() {
        return Failsafe.nn(virtualFile);
    }

    void loadCurrentStatement() {
        SessionBrowser sessionBrowser = getSessionBrowser();
        SessionBrowserTable editorTable = sessionBrowser.getBrowserTable();
        if (editorTable.getSelectedRowCount() == 1) {
            SessionBrowserModelRow selectedRow = editorTable.getModel().getRowAtIndex(editorTable.getSelectedRow());
            if (selectedRow != null) {
                setPreviewText("-- Loading...");
                selectedSessionId = selectedRow.getSessionId();

                Object sessionId = selectedSessionId;
                String schemaName = selectedRow.getSchema();
                Project project = sessionBrowser.getProject();

                Background.run(project, refreshHandle, () -> {
                    ConnectionHandler connection = getConnection();
                    DBSchema schema = null;
                    if (Strings.isNotEmpty(schemaName)) {
                        schema = connection.getObjectBundle().getSchema(schemaName);
                    }

                    checkCancelled(sessionId);
                    SessionBrowserManager sessionBrowserManager = SessionBrowserManager.getInstance(project);
                    String sql = sessionBrowserManager.loadSessionCurrentSql(connection, sessionId);
                    sql = sql.trim().replaceAll("\r\n", "\n").replaceAll("\r", "\n");

                    checkCancelled(sessionId);
                    setSchemaId(SchemaId.from(schema));
                    setPreviewText(sql);
                });
            } else {
                setPreviewText("");
            }
        } else {
            setPreviewText("");
        }
    }

    private void checkCancelled(Object sessionId) {
        if (selectedSessionId == null || !selectedSessionId.equals(sessionId)) {
            throw new OutdatedContentException(this);
        }
    }

    @NotNull
    private ConnectionHandler getConnection() {
        return getSessionBrowser().getConnection();
    }

    public DBLanguagePsiFile getPsiFile() {
        return psiFile.get();
    }

    private void createStatementViewer() {
        SessionBrowser sessionBrowser = getSessionBrowser();
        Project project = sessionBrowser.getProject();
        ConnectionHandler connection = getConnection();
        virtualFile = new DBSessionStatementVirtualFile(sessionBrowser, "");

        DBLanguageDialect languageDialect = DBLanguageDialect.get(SQLLanguage.INSTANCE, connection);
        virtualFile.putUserData(UserDataKeys.LANGUAGE_DIALECT, languageDialect);

        DatabaseFileViewProvider viewProvider = new DatabaseFileViewProvider(project, virtualFile, true);
        DBLanguagePsiFile psiFile = (DBLanguagePsiFile) virtualFile.initializePsiFile(viewProvider, SQLLanguage.INSTANCE);
        this.psiFile = PsiFileRef.of(psiFile);
        document = Documents.getDocument(psiFile);


        viewer = Viewers.createViewer(document, project, virtualFile, SQLFileType.INSTANCE);
        viewer.setEmbeddedIntoDialogWrapper(false);
        viewer.setBorder(null);
        Editors.setEditorReadonly(viewer, true);
        Editors.initEditorHighlighter(viewer, SQLLanguage.INSTANCE, connection);
        //statementViewer.setBackgroundColor(colorsScheme.getColor(ColorKey.find("CARET_ROW_COLOR")));

        EditorSettings settings = viewer.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setLineNumbersShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(2);
        settings.setRightMarginShown(false);
        settings.setUseSoftWraps(true);
        settings.setCaretRowShown(false);

        JComponent viewerComponent = viewer.getComponent();
        viewerComponent.setFocusable(false);
        viewerPanel.add(viewerComponent, BorderLayout.CENTER);

        JScrollPane viewerScrollPane = viewer.getScrollPane();
        viewerScrollPane.setViewportBorder(Borders.lineBorder(Colors.getReadonlyEditorBackground(), 4));
        Borderless.markBorderless(viewerScrollPane.getViewport().getView());
    }


    public class WrapUnwrapContentAction extends ToggleAction {
        WrapUnwrapContentAction() {
            super("Wrap/Unwrap", "", Icons.ACTION_WRAP_TEXT);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return viewer != null && viewer.getSettings().isUseSoftWraps();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            viewer.getSettings().setUseSoftWraps(state);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            boolean isWrapped = viewer != null && viewer.getSettings().isUseSoftWraps();
            e.getPresentation().setText(isWrapped ? "Unwrap Content" : "Wrap Content");

        }
    }

    public class RefreshAction extends BasicAction {
        RefreshAction() {
            super("Reload", "", Icons.ACTION_REFRESH);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            loadCurrentStatement();
        }
    }

    public EditorEx getViewer() {
        return viewer;
    }


    @Override
    public void disposeInner() {
        Editors.releaseEditor(viewer);
        virtualFile = null;
        super.disposeInner();
    }

}
