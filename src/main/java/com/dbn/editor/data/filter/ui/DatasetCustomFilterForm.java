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

package com.dbn.editor.data.filter.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.GuardedBlocks;
import com.dbn.common.util.Strings;
import com.dbn.editor.data.filter.DatasetCustomFilter;
import com.dbn.language.sql.SQLFileType;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.DBDataset;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.dbn.vfs.file.DBDatasetFilterVirtualFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.Objects;

import static com.dbn.common.ui.util.ClientProperty.COMPONENT_GROUP_QUALIFIER;
import static com.dbn.common.ui.util.ClientProperty.NO_INDENT;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.editor.code.content.GuardedBlockType.READONLY_DOCUMENT_SECTION;

public class DatasetCustomFilterForm extends ConfigurationEditorForm<DatasetCustomFilter> {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel editorPanel;
    private JTextField nameTextField;
    private JLabel errorLabel;
    private JLabel queryLabel;

    private Document document;
    private EditorEx editor;
    private String statement;
    private int conditionOffset;
    private static final String COMMENT = "-- enter your custom conditions here";

    public DatasetCustomFilterForm(DBDataset dataset, DatasetCustomFilter filter) {
        super(filter);

        NO_INDENT.set(mainPanel, true);
        nameTextField.setText(filter.getDisplayName());

        initSelectStatement(dataset, filter);
        initErrorLabel(filter);

        // delay the initialization of the filter editor to force psi write action in the dialog modality state
        whenShown(() -> initFilterEditor(dataset, filter) );
    }

    private void initSelectStatement(DBDataset dataset, DatasetCustomFilter filter) {
        @NonNls
        StringBuilder selectStatement = new StringBuilder("select * from ");
        selectStatement.append(dataset.getSchemaName(true)).append('.');
        selectStatement.append(dataset.getName(true));
        selectStatement.append(" where ");
        conditionOffset = selectStatement.length();

        selectStatement.append("\n");

        String condition = filter.getCondition();
        boolean isValidCondition = Strings.isNotEmptyOrSpaces(condition);
        selectStatement.append(isValidCondition ? condition : COMMENT);

        statement = selectStatement.toString();
    }

    private void initFilterEditor(DBDataset dataset, DatasetCustomFilter filter) {
        Project project = dataset.getProject();

        String condition = filter.getCondition();
        boolean isValidCondition = Strings.isNotEmptyOrSpaces(condition);

        DBDatasetFilterVirtualFile filterFile = new DBDatasetFilterVirtualFile(dataset, statement);
        DatabaseFileViewProvider viewProvider = new DatabaseFileViewProvider(project, filterFile, true);
        PsiFile selectStatementFile = filterFile.initializePsiFile(viewProvider, SQLLanguage.INSTANCE);

        document = Documents.ensureDocument(selectStatementFile);
        GuardedBlocks.createGuardedBlock(document, READONLY_DOCUMENT_SECTION, 0, conditionOffset,
                "This predefined part of the query cannot be modified. Please add your filter conditions after the 'WHERE' keyword.");
        editor = Editors.createEditor(document, project, filterFile, SQLFileType.INSTANCE);
        Editors.initEditorHighlighter(editor, SQLLanguage.INSTANCE, dataset);

        editor.setEmbeddedIntoDialogWrapper(true);
        editor.getCaretModel().moveToOffset(conditionOffset);
        if (!isValidCondition) editor.getSelectionModel().setSelection(conditionOffset, document.getTextLength());

        Editors.updateEditorScrollPane(editor);

        EditorSettings settings = editor.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setCaretRowShown(false);
        settings.setLineNumbersShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(2);
        settings.setRightMarginShown(false);
        settings.setUseTabCharacter(true);

        editorPanel.add(editor.getComponent(), BorderLayout.CENTER);
    }

    private void initErrorLabel(DatasetCustomFilter filter) {
        if (filter.getError() == null) {
            errorLabel.setVisible(false);
            errorLabel.setText("");
        } else {
            errorLabel.setVisible(true);
            errorLabel.setText(filter.getError());
            errorLabel.setIcon(Icons.EXEC_MESSAGES_ERROR);
        }
    }

    @Override
    protected void initAccessibility() {
        COMPONENT_GROUP_QUALIFIER.set(queryLabel, true);
    }

    public String getFilterName() {
        return getText(nameTextField);
    }

   /*************************************************
    *                  SettingsEditor               *
    ************************************************
    * @return*/
    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        DatasetCustomFilter filter = getConfiguration();
        String condition = document.getText().substring(conditionOffset);
        if (Objects.equals(condition, COMMENT))
            filter.setCondition(""); else
            filter.setCondition(condition);
        filter.setName(getText(nameTextField));
    }

    @Override
    public void resetFormChanges() {

    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(editor);
        editor = null;
        document = null;
        super.disposeInner();
    }
}
