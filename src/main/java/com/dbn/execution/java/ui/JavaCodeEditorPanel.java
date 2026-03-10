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

package com.dbn.execution.java.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.sql.SQLFileType;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.JavaCodeFragmentFactory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static com.dbn.common.ui.util.ScrollPanes.recalibrateScrollContainer;
import static java.lang.Math.max;
import static java.lang.Math.min;

@Getter
@Setter
public class JavaCodeEditorPanel extends JPanel implements StatefulDisposable {
    private final ProjectRef project;
    private Document document;
    private EditorEx editor;
    private boolean disposed;


    public JavaCodeEditorPanel(Disposable parent, Project project) {
        super(new BorderLayout());
        this.project = ProjectRef.of(project);
        setBorder(Borders.insetBorder(4));
        initCodeEditor();

        Disposer.register(parent, this);
    }

    public Project getProject() {
        return project.ensure();
    }

    private void initCodeEditor() {
        Project project = getProject();

        JavaCodeFragmentFactory factory = JavaCodeFragmentFactory.getInstance(project);
        JavaCodeFragment codeFragment = factory.createCodeBlockCodeFragment("", null, true);
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        document = documentManager.getDocument(codeFragment);

        VirtualFile virtualFile = codeFragment.getVirtualFile();


        PsiFile psiFile = PsiUtil.getPsiFile(project, document);
        if (psiFile != null) {
            DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
            codeAnalyzer.setHighlightingEnabled(psiFile, false);
        }

/*
        FileManager fileManager = getFileManager(project);
        FileViewProvider viewProvider = fileManager.createFileViewProvider(virtualFile, true);
        PsiFile psiFile = viewProvider.getPsi(Languages.getJavaLanguage());
        document = psiFile == null ? null : Documents.getDocument(psiFile);
*/

        editor = Editors.createEditor(document, project, virtualFile, SQLFileType.INSTANCE);
        editor.setEmbeddedIntoDialogWrapper(true);
        Editors.updateEditorScrollPane(editor);

        EditorSettings settings = editor.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setCaretRowShown(false);
        settings.setLineNumbersShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(0);
        settings.setRightMarginShown(false);
        settings.setUseTabCharacter(false);
        settings.setShowIntentionBulb(false);
        settings.setGutterIconsShown(false);

        document.addDocumentListener(createAutoResizer(), this);

        JComponent editorComponent = editor.getComponent();
        add(editorComponent);
    }

    private @NotNull DocumentListener createAutoResizer() {
        return new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                int lineCount = editor.getDocument().getLineCount();
                int lineHeight = editor.getLineHeight();
                int preferredHeight = (min(max(lineCount, 4), 8) + 1) * lineHeight;

                JComponent editorComponent = editor.getComponent();
                Dimension currentSize = editorComponent.getPreferredSize();
                Dimension newSize = new Dimension(currentSize.width, preferredHeight);
                if (currentSize.height == newSize.height) return;

                editorComponent.setPreferredSize(newSize);
                recalibrateScrollContainer(JavaCodeEditorPanel.this);
            }
        };
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(editor);
        editor = null;
        document = null;
    }

    public String getText() {
        return editor.getDocument().getText();
    }

    public void setText(String code) {
        code = code == null ? "" : code;
        Documents.setText(editor.getDocument(), code);
    }
}
