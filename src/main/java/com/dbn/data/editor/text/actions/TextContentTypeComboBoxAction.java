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

package com.dbn.data.editor.text.actions;

import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.action.Lookups;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.data.editor.text.ui.TextEditorForm;
import com.dbn.editor.data.options.DataEditorQualifiedEditorSettings;
import com.dbn.editor.data.options.DataEditorSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public class TextContentTypeComboBoxAction extends ComboBoxAction {
    private TextEditorForm editorForm;

    public TextContentTypeComboBoxAction(TextEditorForm editorForm) {
        this.editorForm = editorForm;
        Presentation presentation = getTemplatePresentation();
        TextContentType contentType = editorForm.getContentType();
        presentation.setText(contentType.getName());
        presentation.setIcon(contentType.getIcon());
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent button, @NotNull DataContext dataContext) {
        Project project = Lookups.getProject(button);
        DataEditorQualifiedEditorSettings qualifiedEditorSettings = DataEditorSettings.getInstance(project).getQualifiedEditorSettings();
        
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        for (TextContentType contentType : qualifiedEditorSettings.getContentTypes()) {
            if (contentType.isSelected()) {
                actionGroup.add(new TextContentTypeSelectAction(editorForm, contentType));
            }

        }
        return actionGroup;
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        TextContentType contentType = editorForm.getContentType();
        presentation.setText(contentType.getName());
        presentation.setIcon(contentType.getIcon());
    }
}
