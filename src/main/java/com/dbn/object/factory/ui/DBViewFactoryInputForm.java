/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.object.factory.ui;

import com.dbn.common.state.StateAttributes;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseIdentifierCase;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.sql.SQLFileType;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.factory.ObjectFactoryManager;
import com.dbn.object.factory.model.DBObjectSpec;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.factory.model.DBObjectAttributeType.OBJECT_DETAIL;
import static com.dbn.object.type.DBObjectType.MATERIALIZED_VIEW;

public class DBViewFactoryInputForm extends DBSchemaObjectFactoryInputForm {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel editorPanel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;
    private JLabel nameLabel;
    private JTextField nameTextField;
    private JCheckBox preserveCaseCheckBox;
    private DBNInfoLabel preserveCaseInfoLabel;
    private EditorEx sqlEditor;
    private Document document;

    public DBViewFactoryInputForm(@NotNull DBNComponent parent, DBObjectSpec input) {
        super(parent, input);

        initContextComponents();
        initHeaderForm();
        initNameLabel();
        initPreserveCaseFields();
        resetFormChanges();

        whenFirstShown(this::initEditor);
    }

    private void initNameLabel() {
        String labelKey = getObjectType() == MATERIALIZED_VIEW ?
                "app.object.label.MaterializedViewName" :
                "app.object.label.ViewName";
        nameLabel.setText(txt(labelKey));
    }

    private void initPreserveCaseFields() {
        preserveCaseInfoLabel.setContent(getPreserveCaseInfoText());
    }

    private void initEditor() {
        ConnectionHandler connection = getConnection();
        DBLanguageDialect languageDialect = connection.getLanguageDialect(SQLLanguage.INSTANCE);
        if (languageDialect == null) {
            languageDialect = SQLLanguage.INSTANCE.getMainLanguageDialect();
        }

        DBLanguagePsiFile selectStatementFile = DBLanguagePsiFile.createFromText(
                ensureProject(),
                "view.sql",
                languageDialect,
                OBJECT_DETAIL.of(input),
                connection,
                input.getSchemaId());
        if (selectStatementFile == null) return;

        document = Documents.ensureDocument(selectStatementFile);
        Documents.onDocumentChanged(document, this, e -> validateFormFields());
        sqlEditor = Editors.createEditor(document, ensureProject(), selectStatementFile.getVirtualFile(), SQLFileType.INSTANCE);
        sqlEditor.setEmbeddedIntoDialogWrapper(true);
        sqlEditor.setPlaceholder(txt("app.object.placeholder.SelectStatement"));
        sqlEditor.setShowPlaceholderWhenFocused(true);
        Editors.initEditorHighlighter(sqlEditor, SQLLanguage.INSTANCE, connection);

        EditorSettings settings = sqlEditor.getSettings();
        settings.setLineNumbersShown(false);
        settings.setGutterIconsShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setCaretRowShown(false);
        settings.setRightMarginShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(2);
        Editors.updateEditorScrollPane(sqlEditor);
        editorPanel.add(sqlEditor.getComponent(), java.awt.BorderLayout.CENTER);
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        super.applyFormChanges();
        input.setAttributeValue(OBJECT_DETAIL, document == null ? "" : document.getText().trim());
        input.setIdentifierCase(getSelectedIdentifierCase());
    }

    @Override
    protected void initStatePersistence() {
        StateAttributes state = ObjectFactoryManager.getInstance(ensureProject()).getState(getObjectType());
        initPersistence(preserveCaseCheckBox, state, "preserve-identifier-case");
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField,
                n -> isNotEmptyOrSpaces(n),
                txt("msg.objects.error.ObjectNameRequired", getObjectType().getDisplayName()));
        addTextValidation(nameTextField,
                n -> isEmptyOrSpaces(n) || isWord(n.trim()),
                txt("msg.objects.error.ValidObjectNameRequired", getObjectType().getDisplayName()));
        addValidation(editorPanel, c -> validateSelectStatement());
    }

    private String validateSelectStatement() {
        String selectStatement = document == null ? "" : document.getText().trim();
        return isEmptyOrSpaces(selectStatement) ? txt("msg.objects.error.SelectStatementRequired") : null;
    }

    @Override
    protected DatabaseIdentifierCase getSelectedIdentifierCase() {
        return preserveCaseCheckBox.isSelected() ?
                DatabaseIdentifierCase.PRESERVE :
                getDefaultIdentifierCase();
    }

    @Override
    protected DBNComboBox<ConnectionHandler> getConnectionComboBox() {
        return connectionComboBox;
    }

    @Override
    protected DBNComboBox<SchemaId> getSchemaComboBox() {
        return schemaComboBox;
    }

    @Override
    protected JPanel getHeaderPanel() {
        return headerPanel;
    }

    @Override
    protected JTextField getNameTextField() {
        return nameTextField;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(sqlEditor);
        sqlEditor = null;
        document = null;
        super.disposeInner();
    }
}
