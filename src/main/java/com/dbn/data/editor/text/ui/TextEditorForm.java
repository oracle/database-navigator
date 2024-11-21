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
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.data.editor.text.actions.TextContentTypeComboBoxAction;
import com.dbn.data.editor.ui.DataEditorComponent;
import com.dbn.data.editor.ui.UserValueHolder;
import com.dbn.data.type.GenericDataType;
import com.dbn.data.value.LargeObjectValue;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguageFileType;
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
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.sql.SQLException;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class TextEditorForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel editorPanel;
    private JPanel actionsPanel;

    private EditorEx editor;
    private String error;
    private String text;

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

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel,
                "DBNavigator.Place.DataEditor.LobContentTypeEditor", true,
                new TextContentTypeComboBoxAction(this));
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);

        text = Strings.removeCharacter(nvl(readUserValue(), ""), '\r');
        initEditor();
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
        FileType fileType = userValueHolder.getContentType().getFileType();
        if (fileType instanceof LanguageFileType) {
            LanguageFileType languageFileType = (LanguageFileType) fileType;

            virtualFile = new LightVirtualFile("text_editor_file." + fileType.getDefaultExtension(), fileType, text);
            virtualFile.putUserData(UserDataKeys.HAS_CONNECTIVITY_CONTEXT, false);

            if (fileType instanceof DBLanguageFileType) {
                DBLanguageFileType dbLanguageFileType = (DBLanguageFileType) fileType;
                DBLanguage dbLanguage = cast(dbLanguageFileType.getLanguage());

                ConnectionHandler connection = userValueHolder.getConnection();
                DBLanguageDialect languageDialect = DBLanguageDialect.get(dbLanguage, connection);
                virtualFile.putUserData(UserDataKeys.LANGUAGE_DIALECT, languageDialect);
            }

            FileManager fileManager = ((PsiManagerEx)PsiManager.getInstance(project)).getFileManager();
            FileViewProvider viewProvider = fileManager.createFileViewProvider(virtualFile, true);
            PsiFile psiFile = viewProvider.getPsi(languageFileType.getLanguage());
            document = psiFile == null ? null : Documents.getDocument(psiFile);
        }

        document = nvl(document, () -> Documents.createDocument(text));

        document.addDocumentListener(documentListener);
        editor = Editors.createEditor(document, project, virtualFile, fileType);
        editor.setEmbeddedIntoDialogWrapper(true);
        editor.getContentComponent().setFocusTraversalKeysEnabled(false);

        if (fileType instanceof DBLanguageFileType) {
            DBLanguageFileType dbFileType = (DBLanguageFileType) fileType;
            DBLanguage language = (DBLanguage) dbFileType.getLanguage();
            Editors.initEditorHighlighter(editor, language, (ConnectionHandler) null);
        }

        int scrollOffset = 0;
        if (oldEditor!= null) {
            scrollOffset = oldEditor.getScrollingModel().getVerticalScrollOffset();
            editorPanel.remove(oldEditor.getComponent());
            Editors.releaseEditor(oldEditor);
        }
        editorPanel.add(editor.getComponent(), BorderLayout.CENTER);
        editor.getScrollingModel().scrollVertically(scrollOffset);
    }

    public void setContentType(TextContentType contentType){
        if (userValueHolder.getContentType() != contentType) {
            userValueHolder.setContentType(contentType);
            initEditor();
        }
    }

    @Nullable
    public String readUserValue() {
        GenericDataType dataType = GenericDataType.LITERAL;
        try {
            Object userValue = userValueHolder.getUserValue();
            if (userValue instanceof String) {
                return (String) userValue;
            } else if (userValue instanceof LargeObjectValue) {
                LargeObjectValue largeObjectValue = (LargeObjectValue) userValue;
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

    public TextContentType getContentType() {
        return userValueHolder.getContentType();
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
