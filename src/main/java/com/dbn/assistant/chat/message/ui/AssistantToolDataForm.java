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

import com.dbn.assistant.tool.execution.AssistantToolInvocation;
import com.dbn.assistant.tool.info.AssistantToolInfoProvider;
import com.dbn.common.action.DataKeys;
import com.dbn.common.color.Colors;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Languages;
import com.dbn.common.util.Viewers;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Objects;

import static com.dbn.assistant.tool.execution.AssistantToolRequestLimits.createPreview;
import static com.dbn.assistant.tool.execution.AssistantToolRequestLimits.isPreviewOversized;
import static com.dbn.common.util.Editors.restrictEditorHeight;
import static com.dbn.language.common.psi.PsiUtil.getFileManager;

public class AssistantToolDataForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel requestDataPanel;
    private JPanel responseDataPanel;
    private JLabel typeNameLabel;
    private JLabel categoryNameLabel;
    private DBNInfoLabel typeInfoLabel;
    private DBNInfoLabel categoryInfoLabel;
    private JLabel typeLabel;
    private JLabel categoryLabel;
    private JPanel toolInfoPanel;

    private final AssistantToolInfoProvider info;
    private final AssistantToolInvocation invocation;

    private EditorEx requestViewer;
    private EditorEx responseViewer;

    public AssistantToolDataForm(DBNComponent parent, AssistantToolInfoProvider info, AssistantToolInvocation invocation) {
        super(parent);
        this.info = info;
        this.invocation = invocation;

        initDataHeader();
        initDataViewers();
    }

    private void initDataHeader() {
        if (info.isExternalTool()) {
            toolInfoPanel.setVisible(false);
            return;
        }

        Color faded = Colors.faded(UIUtil.getLabelForeground());
        typeLabel.setForeground(faded);
        typeLabel.setFont(Fonts.regular(-1));
        categoryLabel.setForeground(faded);
        categoryLabel.setFont(Fonts.regular(-1));

        typeNameLabel.setText(info.getToolTypeName());
        categoryNameLabel.setText(info.getToolCategoryName());

        typeInfoLabel.setContent(TextContent.plain(info.getToolTypeDescription()));
        categoryInfoLabel.setContent(TextContent.plain(info.getToolCategoryDescription()));
    }

    private void initDataViewers() {
        Project project = getProject();
        String requestContent = preparePreviewContent(invocation.getRequestContent());
        String responseContent = invocation.getResponseContent();

        requestViewer = createViewer(project, "ai_tool_request.json", requestContent);
        responseViewer = createViewer(project, "ai_tool_response.json", responseContent);

        if (requestViewer != null && responseViewer != null) {
            requestDataPanel.add(requestViewer.getComponent());
            responseDataPanel.add(responseViewer.getComponent());
            restrictEditorHeight(responseViewer, this, 300);
        } else {
            JTextPane requestTextPane = new JTextPane();
            JTextPane responseTextPane = new JTextPane();

            requestTextPane.setEditable(false);
            requestTextPane.setText(requestContent);
            responseTextPane.setEditable(false);
            responseTextPane.setText(responseContent);

            requestDataPanel.add(requestTextPane);
            responseDataPanel.add(responseTextPane);
        }
    }

    public void updateResponse() {
        DocumentEx responseDocument = responseViewer.getDocument();
        String responseText = responseDocument.getText();
        String responseContent = invocation.getResponseContent();
        if (Objects.equals(responseText, responseContent)) return;

        Documents.setText(responseDocument, responseContent);
    }


    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private static @Nullable EditorEx createViewer(Project project, String fileName, String content) {
        if (isPreviewOversized(content)) return null;

        Language language = Languages.getJsonLanguage();
        VirtualFile file =  new LightVirtualFile(fileName, language, content);

        PsiFile psiFile = initPreviewPsiFile(project, file, language);
        if (psiFile == null) return null;

        Document document = Documents.getDocument(psiFile);
        if (document == null) return null;

        return createViewer(document, project, file);
    }

    private static String preparePreviewContent(String content) {
        return isPreviewOversized(content) ? createPreview(content, content.length()) : content;
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
        viewer.setEmbeddedIntoDialogWrapper(true);

        Editors.updateEditorScrollPane(viewer);

        EditorSettings settings = viewer.getSettings();
        viewer.getComponent().setMaximumSize(new Dimension(-1, 200));

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

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.ASSISTANT_TOOL_DATA_FORM.is(dataId)) return this;
        return null;
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(requestViewer);
        Editors.releaseEditor(responseViewer);
    }
}
