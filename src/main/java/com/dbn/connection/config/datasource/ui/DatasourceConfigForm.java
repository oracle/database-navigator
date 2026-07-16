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

package com.dbn.connection.config.datasource.ui;

import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Json;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBDatasourceConfig;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.object.impl.DBDatasourceConfigImpl;
import com.dbn.object.management.ObjectManagementService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.regex.Pattern;

import static com.dbn.common.file.FileTypes.getJsonFileType;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.disableFormField;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.enableFormField;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.SCHEMA;

public class DatasourceConfigForm extends DBNFormBase {
    private static final String READ_ONLY = "READ_ONLY";
    private static final Pattern CONFIG_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]*$");
    private static final int IDENTIFIER_MAX_LENGTH = 128;
    private static final String DEFAULT_CONFIG_NAME = "new_configuration";
    private static final String DOCUMENTATION_URL = "https://docs.oracle.com/en/database/oracle/oracle-database/23/netag/configuring-centralized-configuration-provider-naming-method.html";
    private static final String DEFAULT_JSON_TEMPLATE = """
            {
              "connect_descriptor": "",
              "jdbc": {},
              "oci": {},
              "pyo": {},
              "njs": {}
            }
            """;

    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel hyperlinkPanel;
    private DBObjectSelector<DBSchema> ownerComboBox;
    private JTextField configNameTextField;
    private JPanel editorPanel;

    private final ConnectionHandler connection;
    @Nullable private final DBDatasourceConfig entry;
    private final boolean canCreateInAnySchema;
    private EditorEx jsonEditor;
    private DBNHeaderForm headerForm;

    DatasourceConfigForm(
            @Nullable Disposable parent,
            @NotNull ConnectionHandler connection) {
        this(parent, connection, null, null, false);
    }

    DatasourceConfigForm(
            @Nullable Disposable parent,
            @NotNull ConnectionHandler connection,
            boolean canCreateInAnySchema) {
        this(parent, connection, null, null, canCreateInAnySchema);
    }

    DatasourceConfigForm(
            @Nullable Disposable parent,
            @NotNull DBDatasourceConfig entry,
            @NotNull String value) {
        this(parent, entry.getConnection(), entry, value, false);
    }

    private DatasourceConfigForm(
            @Nullable Disposable parent,
            @NotNull ConnectionHandler connection,
            @Nullable DBDatasourceConfig entry,
            @Nullable String value,
            boolean canCreateInAnySchema) {
        super(parent, connection.getProject());
        this.connection = connection;
        this.entry = entry;
        this.canCreateInAnySchema = canCreateInAnySchema;

        initHeader();
        initFeatureInfo();
        initEditor();
        initInputs(value);
    }

    private void initHeader() {
        headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    }

    private void initFeatureInfo() {
        DBNHintForm hintForm = new DBNHintForm(this, TextContent.plain(txt("cfg.datasourceConfig.hint.Feature")), null, true);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);

        HyperLinkForm hyperLinkForm = HyperLinkForm.create(
                txt("cfg.datasourceConfig.link.Documentation"),
                txt("cfg.datasourceConfig.link.ConfigProvider"),
                DOCUMENTATION_URL);
        hyperlinkPanel.add(hyperLinkForm.getComponent(), BorderLayout.EAST);
    }

    private void initEditor() {
        FileType jsonFileType = getJsonFileType();
        VirtualFile virtualFile = new LightVirtualFile("data_source_config_store.json", jsonFileType, "");
        Document document = Documents.createDocument(DEFAULT_JSON_TEMPLATE);

        jsonEditor = Editors.createEditor(document, ensureProject(), virtualFile, jsonFileType);
        jsonEditor.setEmbeddedIntoDialogWrapper(true);
        jsonEditor.setPlaceholder(DEFAULT_JSON_TEMPLATE);

        EditorSettings settings = jsonEditor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setFoldingOutlineShown(true);
        settings.setLineMarkerAreaShown(false);
        settings.setRightMarginShown(false);

        Editors.updateEditorScrollPane(jsonEditor);
        editorPanel.add(jsonEditor.getComponent(), BorderLayout.CENTER);
    }

    private void initInputs(@Nullable String value) {
        String ownerName = entry == null ? connection.getUserName() : entry.getOwnerName();
        setText(configNameTextField, entry == null ? DEFAULT_CONFIG_NAME : entry.getConfigName());

        boolean creating = entry == null;
        ownerComboBox
                .initialize(this, SCHEMA)
                .withConnectionContext(() -> connection)
                .withValueLoader(() -> connection.getObjectBundle().getSchemas())
                .withValuePreselector(() -> ownerName)
                .triggerLoad();
        if (creating && canCreateInAnySchema) {
            enableFormField(ownerComboBox, READ_ONLY);
        } else {
            disableFormField(ownerComboBox, READ_ONLY);
        }
        configNameTextField.setEnabled(creating);
        ownerComboBox.setToolTipText(txt("cfg.datasourceConfig.text.OwnerFieldTooltip"));
        configNameTextField.setToolTipText(txt("cfg.datasourceConfig.text.ConfigNameFieldTooltip"));
        if (entry != null && value != null) {
            Documents.setText(getProject(), jsonEditor.getDocument(), value);
        }
    }

    @Override
    protected void initValidation() {
        addSelectionValidation(ownerComboBox, txt("cfg.datasourceConfig.error.OwnerRequired"));
        addTextValidation(configNameTextField, c -> isNotEmpty(c.trim()), txt("cfg.datasourceConfig.error.ConfigNameRequired"));
        addTextValidation(configNameTextField, c -> c.trim().isEmpty() || c.trim().length() <= IDENTIFIER_MAX_LENGTH, txt("cfg.datasourceConfig.error.ConfigNameTooLong", IDENTIFIER_MAX_LENGTH));
        addTextValidation(configNameTextField, c -> c.trim().isEmpty() || CONFIG_NAME_PATTERN.matcher(c.trim()).matches(), txt("cfg.datasourceConfig.error.ConfigNameInvalid"));
        addValidation(editorPanel, c -> validateJson());
    }

    void createEntry(OutcomeHandler successHandler) {
        getManagementService().createObject(inputsToEntry(), successHandler);
    }

    void updateEntry(OutcomeHandler successHandler) {
        getManagementService().updateObject(inputsToEntry(), successHandler);
    }

    private DBDatasourceConfigImpl inputsToEntry() {
        DBSchema owner = getSelection(ownerComboBox);
        String ownerName = owner == null ? "" : owner.getName();
        String configName = getText(configNameTextField).trim();
        String value = readEditorText().trim();
        return new DBDatasourceConfigImpl(connection, ownerName, configName, value);
    }

    @NotNull
    private ObjectManagementService getManagementService() {
        return ObjectManagementService.getInstance(getProject());
    }

    private @Nullable String validateJson() {
        String value = readEditorText().trim();
        if (value.isBlank()) return txt("cfg.datasourceConfig.error.JsonRequired");

        try {
            Json.readAsMap(value);
            return null;
        } catch (Exception e) {
            return txt("cfg.datasourceConfig.error.JsonInvalid");
        }
    }

    private @NotNull String readEditorText() {
        return jsonEditor == null ? "" : jsonEditor.getDocument().getText();
    }

    @Override
    public @NotNull JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        headerForm = null;
        Editors.releaseEditor(jsonEditor);
        jsonEditor = null;
        super.disposeInner();
    }
}
