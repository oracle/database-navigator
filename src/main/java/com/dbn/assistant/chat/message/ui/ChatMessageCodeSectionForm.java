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

package com.dbn.assistant.chat.message.ui;

import com.dbn.assistant.chat.message.ChatMessageSection;
import com.dbn.assistant.chat.message.action.CopyContentAction;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Viewers;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static com.dbn.assistant.chat.message.ChatMessageSectionType.CODE;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.language.common.psi.PsiUtil.getFileManager;
import static javax.swing.JLayeredPane.DRAG_LAYER;

public class ChatMessageCodeSectionForm extends ChatMessageSectionForm {
    private static final AtomicLong previewFileIndex = new AtomicLong(0);

    private JPanel mainPanel;
    private JPanel codeViewerPanel;
    private final EditorEx codeViewer;

    private final ConnectionRef connection;
    private Language language;

    private ChatMessageCodeSectionForm(DBNForm parent, ConnectionHandler connection, EditorEx codeViewer, ChatMessageSection section) {
        super(parent, CODE);
        this.codeViewer = codeViewer;
        this.language = section.getLanguage();
        this.connection = ConnectionRef.of(connection);

        mainPanel.setOpaque(false);
        mainPanel.setBorder(JBUI.Borders.empty(10));
        codeViewerPanel.add(codeViewer.getComponent());

        initActionToolbar();
    }

    private void initActionToolbar() {
        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        CopyContentAction copyContentAction = new CopyContentAction(() -> codeViewer.getDocument().getText());
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionPanel, true, copyContentAction);
        JComponent component = actionToolbar.getComponent();
        component.setOpaque(false);
        component.setBorder(Borders.EMPTY_BORDER);
        actionPanel.add(component, BorderLayout.NORTH);

        JComponent viewerComponent = codeViewer.getComponent();
        UserInterface.visitRecursively(viewerComponent, JLayeredPane.class, p -> p.add(actionPanel, DRAG_LAYER));
    }

    public ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    @Nullable
    public static ChatMessageCodeSectionForm create(DBNForm parent, ConnectionHandler connection, ChatMessageSection section){
        EditorEx codeViewer = createViewer(connection, section);
        if (codeViewer == null) return null;

        return new ChatMessageCodeSectionForm(parent, connection, codeViewer, section);
    }

    @Override
    protected void applyContent(TextContent content, @Nullable Language language) {
        String text = content.getText();
        if (language == null || Objects.equals(this.language, language)) {
            DocumentEx document = codeViewer.getDocument();
            Documents.setText(document, text);
            return;
        }

        // language has changed, create a new viewer
        this.language = language;

        EditorEx oldCodeViewer = codeViewer;
        EditorEx codeViewer = createViewer(getConnection(), text, language);
        if (codeViewer != null) {
            codeViewerPanel.removeAll();
            codeViewerPanel.add(codeViewer.getComponent());
            Editors.releaseEditor(oldCodeViewer);
        }
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Nullable
    private static EditorEx createViewer(ConnectionHandler connection, ChatMessageSection section) {
        String content = section.getContent();
        Language language = section.getLanguage();
        if (language == null) return null;

        return createViewer(connection, content, language);
    }

    private static @Nullable EditorEx createViewer(ConnectionHandler connection, String content, Language language) {
        Project project = connection.getProject();

        VirtualFile file = initPreviewFile(content, language);
        PsiFile psiFile = initPreviewPsiFile(project, file, language);
        if (psiFile == null) return null;

        Document document = Documents.getDocument(psiFile);
        if (document == null) return null;

        initDatabaseContext(file, connection);
        return createViewer(document, project, file);
    }

    private static @NotNull EditorEx createViewer(Document document, Project project, VirtualFile file) {
        EditorEx viewer = Viewers.createViewer(document, project, file, file.getFileType());
        viewer.setEmbeddedIntoDialogWrapper(false);

        Editors.updateEditorScrollPane(viewer);

        EditorSettings settings = viewer.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setLineNumbersShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setRightMarginShown(false);
        settings.setCaretRowShown(false);
        settings.setUseSoftWraps(true);
        settings.setAdditionalLinesCount(0);
        settings.setAutoCodeFoldingEnabled(false);
        settings.setShowIntentionBulb(false);
        settings.setGutterIconsShown(false);
        return viewer;
    }

    private static @NotNull VirtualFile initPreviewFile(String content, Language language) {
        language = nvl(language, PlainTextLanguage.INSTANCE);

        //LanguageFileType fileType = language.getAssociatedFileType();
        //String fileName = "ai_preview_file_" + previewFileIndex.incrementAndGet() + "." + (fileType == null ? "txt" : fileType.getDefaultExtension());
        String fileName = "ai_preview_file_" + previewFileIndex.incrementAndGet();
        return new LightVirtualFile(fileName, language, content);
    }

    public static @Nullable PsiFile initPreviewPsiFile(Project project, VirtualFile file, Language language) {
        FileManager fileManager = getFileManager(project);
        FileViewProvider viewProvider = fileManager.createFileViewProvider(file, true);
        PsiFile psiFile = viewProvider.getPsi(language);
        if (psiFile == null) {
            Language baseLanguage = viewProvider.getBaseLanguage();
            if (baseLanguage != language) {
                psiFile = viewProvider.getPsi(baseLanguage);
            }
        }

        if (psiFile != null) {
            DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
            codeAnalyzer.setHighlightingEnabled(psiFile, false);
        }
        return psiFile;
    }

    private static void initDatabaseContext(VirtualFile file, ConnectionHandler connection) {
        Project project = connection.getProject();
        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        contextManager.setConnection(file, connection);
    }

    @Override
    public void dispose() {
        Editors.releaseEditor(codeViewer);
    }
}
