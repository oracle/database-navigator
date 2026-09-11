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

package com.dbn.data.editor.text.ui;

import com.dbn.common.action.UserDataKeys;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Json;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.data.editor.text.TextContentTypeOwner;
import com.dbn.data.editor.text.actions.TextContentTypeComboBoxAction;
import com.dbn.data.editor.text.actions.TextEditorRevertAction;
import com.dbn.data.editor.ui.DataEditorComponent;
import com.dbn.data.editor.ui.UserValueHolder;
import com.dbn.data.type.GenericDataType;
import com.dbn.data.value.JsonValue;
import com.dbn.data.value.LargeObjectValue;
import com.dbn.data.value.XmlTypeValue;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguageFileType;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.sql.SQLException;
import java.util.function.Predicate;

import static com.dbn.common.file.FileTypes.getDtdFileType;
import static com.dbn.common.file.FileTypes.getJsonFileType;
import static com.dbn.common.file.FileTypes.getTextFileType;
import static com.dbn.common.file.FileTypes.getXmlFileType;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.language.common.psi.PsiUtil.getFileManager;
import static com.dbn.nls.NlsResources.txt;
import static com.intellij.openapi.util.text.StringUtil.convertLineSeparators;

public class TextEditorForm extends DBNFormBase implements TextContentTypeOwner {
    private JPanel mainPanel;
    private JPanel editorPanel;
    private JPanel actionsPanel;
    private JPanel loadingDataPanel;
    private JPanel loadingIconPanel;

    private EditorEx editor;
    private PsiFile psiFile;
    private String text;
    private String originalText;
    private boolean contentLoading;

    private final UserValueHolder<?> userValueHolder;
    private final DataEditorComponent textEditorAdapter;
    private final DocumentListener documentListener;


    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public TextEditorForm(TextEditorDialog parent, DocumentListener documentListener, UserValueHolder<?> userValueHolder, DataEditorComponent textEditorAdapter) {
        super(parent);
        this.documentListener = documentListener;
        this.userValueHolder = userValueHolder;
        this.textEditorAdapter = textEditorAdapter;

        Project project = getProject();
        if (userValueHolder.getContentType() == null) {
            userValueHolder.setContentType(TextContentType.getPlainText(project));
        }

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true,
                new TextContentTypeComboBoxAction(this),
                new TextEditorRevertAction(this));
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);

        loadingIconPanel.add(new AsyncProcessIcon("Loading"));
        contentLoading = true;
        loadingDataPanel.setVisible(true);
        text = "";
        originalText = "";
        initEditor();
        loadContent();
    }

    private void initEditor() {
        Document document = null;
        EditorEx oldEditor = editor;
        if (oldEditor != null) {
            document = oldEditor.getDocument();
            document.removeDocumentListener(documentListener);
            text = document.getText();
            document = null;
        }

        Project project = ensureProject();
        VirtualFile virtualFile = null;
        psiFile = null;
        FileType fileType = userValueHolder.getContentType().getFileType();
        if (fileType instanceof LanguageFileType languageFileType) {

            virtualFile = new LightVirtualFile("text_editor_file." + fileType.getDefaultExtension(), fileType, text);
            virtualFile.putUserData(UserDataKeys.HAS_CONNECTIVITY_CONTEXT, false);

            if (fileType instanceof DBLanguageFileType dbLanguageFileType) {
                DBLanguage dbLanguage = cast(dbLanguageFileType.getLanguage());

                ConnectionHandler connection = userValueHolder.getConnection();
                DBLanguageDialect languageDialect = DBLanguageDialect.get(dbLanguage, connection);
                virtualFile.putUserData(UserDataKeys.LANGUAGE_DIALECT, languageDialect);
            }

            FileManager fileManager = getFileManager(project);
            FileViewProvider viewProvider = fileManager.createFileViewProvider(virtualFile, true);
            psiFile = viewProvider.getPsi(languageFileType.getLanguage());
            if (contentLoading) {
                PsiUtil.setHighlightingEnabled(psiFile, false);
            }
            document = psiFile == null ? null : Documents.getDocument(psiFile);
        }

        document = nvl(document, () -> Documents.createDocument(text));

        document.addDocumentListener(documentListener, this);
        editor = Editors.createEditor(document, project, virtualFile, fileType);
        editor.setEmbeddedIntoDialogWrapper(true);
        editor.getContentComponent().setFocusTraversalKeysEnabled(false);
        Editors.updateEditorScrollPane(editor);
        Editors.setEditorReadonly(editor, contentLoading);

        if (fileType instanceof DBLanguageFileType dbFileType) {
            DBLanguage language = (DBLanguage) dbFileType.getLanguage();
            Editors.initEditorHighlighter(editor, language, (ConnectionHandler) null);
        }

        int scrollOffset = 0;
        if (oldEditor!= null) {
            scrollOffset = oldEditor.getScrollingModel().getVerticalScrollOffset();
            editorPanel.remove(oldEditor.getComponent());
            Editors.releaseEditor(oldEditor);
        }
        editorPanel.add(editor.getComponent());
        editor.getScrollingModel().scrollVertically(scrollOffset);
    }

    private void loadContent() {
        Dispatch.async(mainPanel,
                () -> readUserValue(),
                t -> contentLoaded(t));
    }

    private void contentLoaded(@Nullable String loadedText) {
        if (isDisposed()) return;

        text = convertLineSeparators(nvl(loadedText, ""));
        originalText = text;
        Document document = editor.getDocument();
        document.removeDocumentListener(documentListener);
        try {
            Documents.resetText(editor, text, false);
        } finally {
            document.addDocumentListener(documentListener, this);
        }


        contentLoading = false;
        Editors.setEditorReadonly(editor, false);
        PsiUtil.setHighlightingEnabled(psiFile, true);
        loadingDataPanel.setVisible(false);
    }

    public boolean isContentChanged() {
        return !contentLoading && editor != null && !editor.getDocument().getText().equals(originalText);
    }

    public void revertChanges() {
        if (!isContentChanged()) return;

        text = originalText;
        Document document = editor.getDocument();
        document.removeDocumentListener(documentListener);
        try {
            Documents.resetText(editor, originalText, false);
        } finally {
            document.addDocumentListener(documentListener, this);
        }

        TextEditorDialog dialog = ensureParentComponent();
        dialog.resetActions();
    }

    public void setContentType(TextContentType contentType){
        if (userValueHolder.getContentType() == contentType) return;

        userValueHolder.setContentType(contentType);
        initEditor();
    }

    public TextContentType getContentType() {
        return userValueHolder.getContentType();
    }

    @Override
    public Predicate<TextContentType> getContentTypeFilter() {
        Object userValue = userValueHolder.getUserValue();
        if (userValue instanceof JsonValue) {
            return t -> {
                FileType fileType = t.getFileType();
                return fileType == getJsonFileType() ||
                        fileType == getTextFileType();
            };
        }

        if (userValue instanceof XmlTypeValue) {
            return t -> {
                FileType fileType = t.getFileType();
                return fileType == getXmlFileType() ||
                        fileType == getDtdFileType() ||
                        fileType == getTextFileType();
            };
        }

        return TextContentTypeOwner.super.getContentTypeFilter();
    }

    @Nullable
    public String readUserValue() {
        GenericDataType dataType = GenericDataType.LITERAL;
        try {
            Object userValue = userValueHolder.getUserValue();
            if (userValue instanceof String stringUserValue) {
                return stringUserValue;
            }

            if (userValue instanceof JsonValue jsonValue) {
                return Json.formatJsonContent(jsonValue.getData());
            }

            if (userValue instanceof LargeObjectValue largeObjectValue) {
                dataType = largeObjectValue.getGenericDataType();
                return largeObjectValue.read();
            }
        } catch (SQLException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(getProject(), txt("msg.dataEditor.error.ContentLoadError", dataType), e);
        }
        return null;
    }

    @NotNull
    public String getText() {
        return editor.getDocument().getText();
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(editor);
        super.disposeInner();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return editor.getContentComponent();
    }
}
