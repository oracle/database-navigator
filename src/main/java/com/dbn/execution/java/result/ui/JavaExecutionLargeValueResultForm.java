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

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Strings;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.data.editor.text.TextContentTypeOwner;
import com.dbn.data.editor.text.actions.TextContentTypeComboBoxAction;
import com.dbn.data.value.LargeObjectValue;
import com.dbn.execution.common.input.ExecutionValue;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.sql.SQLException;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Messages.showWarningDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;

public class JavaExecutionLargeValueResultForm extends DBNFormBase implements TextContentTypeOwner {
    private JPanel actionsPanel;
    private JPanel mainPanel;
    private JPanel largeValuePanel;

    private final DBObjectRef<DBJavaParameter> parameter;
    private EditorEx editor;
    private TextContentType contentType;

    JavaExecutionLargeValueResultForm(JavaExecutionResultForm parent, DBJavaParameter parameter, ExecutionValue fieldValue) {
        super(parent);
        this.parameter = DBObjectRef.of(parameter);

        String text = "";
        Project project = getProject();
        Object value = fieldValue.getValue();
        if (value instanceof LargeObjectValue largeObjectValue) {
            try {
                text = largeObjectValue.read();
            } catch (SQLException e) {
                conditionallyLog(e);
                showWarningDialog(project,
                        txt("msg.execution.title.MethodArgumentLoadError"),
                        txt("msg.execution.message.MethodArgumentLoadError", parameter.getName(), e.getMessage()));
            }
        } else if (value instanceof String) {
            text = (String) value;
        }

        text = Strings.removeCharacter(nvl(text, ""), '\r');
        Document document = Documents.createDocument(text);

        contentType = TextContentType.get(project, getCanonicalName(parameter.getJavaClassRef()));

        if (contentType == null) contentType = TextContentType.getPlainText(project);

        editor = Editors.createEditor(document, project, null, contentType.getFileType());
        editor.getContentComponent().setFocusTraversalKeysEnabled(false);

        largeValuePanel.add(editor.getComponent(), BorderLayout.CENTER);
        largeValuePanel.setBorder(IdeBorderFactory.createBorder());

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, new TextContentTypeComboBoxAction(this));
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
        this.contentType = contentType;
        Editors.initEditorHighlighter(editor, contentType);
    }

    @Override
    public TextContentType getContentType() {
        return contentType;
    }

    public DBJavaParameter getParameter() {
        return parameter.get();
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
