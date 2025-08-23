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
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.language.common.psi.PsiUtil.getFileManager;
import static javax.swing.JLayeredPane.DRAG_LAYER;

public class ChatMessageSectionCodeForm extends ChatMessageSectionForm {
    private JPanel mainPanel;
    private JPanel codeViewerPanel;
    private final EditorEx codeViewer;

    public ChatMessageSectionCodeForm(DBNForm parent, EditorEx codeViewer) {
        super(parent);
        this.codeViewer = codeViewer;

        mainPanel.setOpaque(false);
        mainPanel.setBorder(JBUI.Borders.empty(10));
        codeViewerPanel.add(codeViewer.getComponent());

        initActionToolbar();
    }

    private void initActionToolbar() {
        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        String content = codeViewer.getDocument().getText();
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionPanel, true, new CopyContentAction(content));
        JComponent component = actionToolbar.getComponent();
        component.setOpaque(false);
        component.setBorder(Borders.EMPTY_BORDER);
        actionPanel.add(component, BorderLayout.NORTH);

        JComponent viewerComponent = codeViewer.getComponent();
        UserInterface.visitRecursively(viewerComponent, JLayeredPane.class, p -> p.add(actionPanel, DRAG_LAYER));
    }

    public static ChatMessageSectionCodeForm create(DBNForm parent, ConnectionHandler connection, ChatMessageSection section){
        EditorEx codeViewer = createViewer(connection, section);
        if (codeViewer == null) return null;

        return new ChatMessageSectionCodeForm(parent, codeViewer);
    }

    @Override
    protected void applyTextContent(TextContent content) {
        Documents.setText(codeViewer.getDocument(), content.getText());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Nullable
    private static EditorEx createViewer(ConnectionHandler connection, ChatMessageSection section) {
        Language language = section.getLanguage();
        if (language == null) return null;

        LanguageFileType fileType = language.getAssociatedFileType();
        String fileName = "ai_preview_file." + (fileType == null ? "txt" : fileType.getDefaultExtension());
        VirtualFile file = new LightVirtualFile(fileName, language, section.getContent());

        Project project = connection.getProject();
        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        contextManager.setConnection(file, connection);

        FileManager fileManager = getFileManager(project);
        FileViewProvider viewProvider = fileManager.createFileViewProvider(file, true);
        PsiFile psiFile = viewProvider.getPsi(language);
        if (psiFile == null) {
            Language baseLanguage = viewProvider.getBaseLanguage();
            if (baseLanguage != language) {
                psiFile = viewProvider.getPsi(baseLanguage);
            }
        }

        if (psiFile == null) return null;

        Document document = Documents.getDocument(psiFile);
        if (document == null) return null;

        EditorEx viewer = Viewers.createViewer(document, project, file, file.getFileType());
        viewer.setEmbeddedIntoDialogWrapper(false);
        //Editors.initEditorHighlighter(viewer, language, connection);

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

        return viewer;
    }

    @Override
    public void dispose() {
        Editors.releaseEditor(codeViewer);
    }
}
