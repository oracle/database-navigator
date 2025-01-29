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

package com.dbn.execution.java.result.ui;

import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.data.value.LargeObjectValue;
import com.dbn.editor.data.options.DataEditorQualifiedEditorSettings;
import com.dbn.editor.data.options.DataEditorSettings;
import com.dbn.execution.java.ArgumentValue;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.sql.SQLException;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class JavaExecutionLargeValueResultForm extends DBNFormBase {
    private JPanel actionsPanel;
    private JPanel mainPanel;
    private JPanel largeValuePanel;

    private final DBObjectRef<DBJavaParameter> argument;
    private EditorEx editor;
    private TextContentType contentType;

    JavaExecutionLargeValueResultForm(JavaExecutionResultForm parent, DBJavaParameter argument, ArgumentValue argumentValue) {
        super(parent);
        this.argument = DBObjectRef.of(argument);

        String text = "";
        Project project = getProject();
        Object value = argumentValue.getValue();
        if (value instanceof LargeObjectValue) {
            LargeObjectValue largeObjectValue = (LargeObjectValue) value;
            try {
                text = largeObjectValue.read();
            } catch (SQLException e) {
                conditionallyLog(e);
                Messages.showWarningDialog(project, "Load error", "Could not load value for argument " + argument.getName() + ". Cause: " + e.getMessage());
            }
        } else if (value instanceof String) {
            text = (String) value;
        }

        text = Strings.removeCharacter(nvl(text, ""), '\r');
        Document document = Documents.createDocument(text);

        String contentTypeName = argument.getParameterType();
        contentType = TextContentType.get(project, contentTypeName);

        if (contentType == null) contentType = TextContentType.getPlainText(project);

        editor = Editors.createEditor(document, project, null, contentType.getFileType());
        editor.getContentComponent().setFocusTraversalKeysEnabled(false);

        largeValuePanel.add(editor.getComponent(), BorderLayout.CENTER);


        largeValuePanel.setBorder(IdeBorderFactory.createBorder());

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, new ContentTypeComboBoxAction());
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);


/*
        ActionToolbar actionToolbar = ActionUtil.createActionToolbar("", true,
                new CursorResultFetchNextRecordsAction(executionResult, resultTable),
                new CursorResultViewRecordAction(resultTable),
                ActionUtil.SEPARATOR,
                new CursorResultExportAction(resultTable, argument));

        actionsPanel.add(actionToolbar.getComponent());
*/
    }

    public void setContentType(TextContentType contentType) {
        Editors.initEditorHighlighter(editor, contentType);
    }

    public DBJavaParameter getArgument() {
        return argument.get();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public class ContentTypeComboBoxAction extends ComboBoxAction {

        ContentTypeComboBoxAction() {
            Presentation presentation = getTemplatePresentation();
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
                    actionGroup.add(new ContentTypeSelectAction(contentType));
                }

            }
            return actionGroup;
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setText(contentType.getName());
            presentation.setIcon(contentType.getIcon());
        }
    }

    @Getter
    public class ContentTypeSelectAction extends ProjectAction {
        private final TextContentType contentType;

        ContentTypeSelectAction(TextContentType contentType) {
            this.contentType = contentType;
        }

        @Override
        protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
            Presentation presentation = e.getPresentation();
            presentation.setText(contentType.getName());
            presentation.setIcon(contentType.getIcon());
        }

        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
            Editors.initEditorHighlighter(editor, contentType);
            JavaExecutionLargeValueResultForm.this.contentType = contentType;
        }
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(editor);
        editor = null;
    }
}
