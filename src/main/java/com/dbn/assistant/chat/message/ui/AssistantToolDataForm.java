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

import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Languages;
import com.dbn.common.util.Viewers;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import static com.dbn.language.common.psi.PsiUtil.getFileManager;

public class AssistantToolDataForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel requestDataPanel;
    private JPanel responseDataPanel;

    private final EditorEx requestViewer;
    private final EditorEx responseViewer;

    public AssistantToolDataForm(@Nullable Disposable parent, @NotNull Project project, String request, String response) {
        super(parent, project);

        requestViewer = createViewer(project, "ai_tool_request.json", request);
        responseViewer = createViewer(project, "ai_tool_response.json", response);

        if (requestViewer != null && responseViewer != null) {
            requestDataPanel.add(requestViewer.getComponent());
            responseDataPanel.add(responseViewer.getComponent());
        } else {
            JTextPane requestTextPane = new JTextPane();
            JTextPane responseTextPane = new JTextPane();

            requestTextPane.setEditable(false);
            requestTextPane.setText(request);
            responseTextPane.setEditable(false);
            requestTextPane.setText(response);

            requestDataPanel.add(requestTextPane);
            responseDataPanel.add(responseTextPane);
        }
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private static @Nullable EditorEx createViewer(Project project, String fileName, String content) {
        Language language = Languages.getJsonLanguage();
        VirtualFile file =  new LightVirtualFile(fileName, language, content);

        PsiFile psiFile = initPreviewPsiFile(project, file, language);
        if (psiFile == null) return null;

        Document document = Documents.getDocument(psiFile);
        if (document == null) return null;

        return createViewer(document, project, file);
    }

    public static @Nullable PsiFile initPreviewPsiFile(Project project, VirtualFile file, Language language) {
        FileManager fileManager = getFileManager(project);
        FileViewProvider viewProvider = fileManager.createFileViewProvider(file, true);
        PsiFile psiFile = viewProvider.getPsi(language);
        if (psiFile != null) {
            DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
            codeAnalyzer.setHighlightingEnabled(psiFile, false);
        }
        return psiFile;
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

    public static void showPopup(DataContext context, String request, String response) {
        Project project = context.getData(CommonDataKeys.PROJECT);
        if (project == null) return;

        AssistantToolDataForm dataForm = new AssistantToolDataForm(null, project, request, response);
        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(dataForm.getMainComponent(), null);
        JBPopup popup = popupBuilder.createPopup();
        Disposer.register(popup, dataForm);
        popup.showInBestPositionFor(context);
    }

    @Override
    public void dispose() {
        Editors.releaseEditor(requestViewer);
        Editors.releaseEditor(responseViewer);
    }
}
