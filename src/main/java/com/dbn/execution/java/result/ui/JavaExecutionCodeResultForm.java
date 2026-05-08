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

package com.dbn.execution.java.result.ui;

import com.dbn.common.file.FileTypes;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Strings;
import com.dbn.execution.common.input.ExecutionValue;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.execution.common.input.CodeBlocks.extractCodeBlock;

public class JavaExecutionCodeResultForm extends JavaExecutionResultDetailForm {
    private JPanel mainPanel;
    private JPanel codeViewerPanel;

    private EditorEx editor;

    JavaExecutionCodeResultForm(JavaExecutionResultForm parent, ExecutionValue<String> fieldValue) {
        super(parent, fieldValue);

        String text = extractCodeBlock(fieldValue.getValue());
        Project project = getProject();

        text = Strings.removeCharacter(nvl(text, ""), '\r');
        Document document = Documents.createDocument(text);

        editor = Editors.createEditor(document, project, null, FileTypes.getJavaFileType());
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


        editor.getContentComponent().setFocusTraversalKeysEnabled(false);

        codeViewerPanel.add(editor.getComponent());
        codeViewerPanel.setBorder(IdeBorderFactory.createBorder());
    }


    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(editor);
        editor = null;
    }
}
