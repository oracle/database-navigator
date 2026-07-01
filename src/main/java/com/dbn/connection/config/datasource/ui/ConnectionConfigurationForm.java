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
import com.dbn.object.DBConnectionConfiguration;
import com.dbn.object.impl.DBConnectionConfigurationImpl;
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
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.nls.NlsResources.txt;

public class ConnectionConfigurationForm extends DBNFormBase {
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9._-]*$");
    private static final int KEY_MAX_LENGTH = 128;
    private static final String DEFAULT_ENTRY_KEY = "new_entry";
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
    private JTextField keyTextField;
    private JPanel editorPanel;

    private final ConnectionHandler connection;
    @Nullable private final DBConnectionConfiguration entry;
    private EditorEx jsonEditor;
    private DBNHeaderForm headerForm;

    ConnectionConfigurationForm(
            @Nullable Disposable parent,
            @NotNull ConnectionHandler connection) {
        this(parent, connection, null, null);
    }

    ConnectionConfigurationForm(
            @Nullable Disposable parent,
            @NotNull DBConnectionConfiguration entry,
            @NotNull String value) {
        this(parent, entry.getConnection(), entry, value);
    }

    private ConnectionConfigurationForm(
            @Nullable Disposable parent,
            @NotNull ConnectionHandler connection,
            @Nullable DBConnectionConfiguration entry,
            @Nullable String value) {
        super(parent, connection.getProject());
        this.connection = connection;
        this.entry = entry;

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
        DBNHintForm hintForm = new DBNHintForm(this, TextContent.plain(txt("cfg.connectionConfig.hint.Feature")), null, true);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);

        HyperLinkForm hyperLinkForm = HyperLinkForm.create(
                txt("cfg.connectionConfig.link.Documentation"),
                txt("cfg.connectionConfig.link.ConfigProvider"),
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
        setText(keyTextField, entry == null ? DEFAULT_ENTRY_KEY : entry.getName());
        keyTextField.setEnabled(entry == null);
        keyTextField.setToolTipText(txt("cfg.connectionConfig.text.KeyFieldTooltip"));
        if (entry != null && value != null) {
            Documents.setText(getProject(), jsonEditor.getDocument(), value);
        }
    }

    @Override
    protected void initValidation() {
        addTextValidation(keyTextField, c -> isNotEmpty(c.trim()), txt("cfg.connectionConfig.error.KeyRequired"));
        addTextValidation(keyTextField, c -> c.trim().isEmpty() || c.trim().length() <= KEY_MAX_LENGTH, txt("cfg.connectionConfig.error.KeyTooLong", KEY_MAX_LENGTH));
        addTextValidation(keyTextField, c -> c.trim().isEmpty() || KEY_PATTERN.matcher(c.trim()).matches(), txt("cfg.connectionConfig.error.KeyInvalid"));
        addValidation(editorPanel, c -> validateJson());
    }

    void createEntry(OutcomeHandler successHandler) {
        getManagementService().createObject(inputsToEntry(), successHandler);
    }

    void updateEntry(OutcomeHandler successHandler) {
        getManagementService().updateObject(inputsToEntry(), successHandler);
    }

    private DBConnectionConfigurationImpl inputsToEntry() {
        String key = getText(keyTextField).trim();
        String value = readEditorText().trim();
        return new DBConnectionConfigurationImpl(connection, key, value);
    }

    @NotNull
    private ObjectManagementService getManagementService() {
        return ObjectManagementService.getInstance(getProject());
    }

    private @Nullable String validateJson() {
        String value = readEditorText().trim();
        if (value.isBlank()) return txt("cfg.connectionConfig.error.JsonRequired");

        try {
            Json.readAsMap(value);
            return null;
        } catch (Exception e) {
            return txt("cfg.connectionConfig.error.JsonInvalid");
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
