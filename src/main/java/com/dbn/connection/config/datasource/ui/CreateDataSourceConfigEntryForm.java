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

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Json;
import com.dbn.common.util.Messages;
import com.dbn.common.outcome.Outcome;
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.impl.DBDataSourceConfigEntryImpl;
import com.dbn.object.management.ObjectManagementService;
import com.dbn.object.type.DBObjectType;
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
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.util.regex.Pattern;

import static com.dbn.common.file.FileTypes.getJsonFileType;
import static com.dbn.common.util.Strings.isEmpty;

public class CreateDataSourceConfigEntryForm extends DBNFormBase {
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9._-]*$");
    private static final int KEY_MAX_LENGTH = 128;
    private static final String DEFAULT_ENTRY_KEY = "new_entry";
    private static final String DOCUMENTATION_URL = "https://docs.oracle.com/en/database/oracle/oracle-database/23/netag/configuring-centralized-configuration-provider-naming-method.html";
    private static final String FEATURE_HINT = """
            Configuration Entries lets you save reusable connection configuration profiles in the database.

            Create a key and JSON settings once, then let clients reference that entry using DATA_SOURCE_CONFIG_KEY. Add only the sections you need, including optional driver blocks for jdbc, oci, pyo, and njs.
            """;
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
    private JLabel statusLabel;

    private final ConnectionHandler connection;
    private EditorEx jsonEditor;
    private DBNHeaderForm headerForm;

    CreateDataSourceConfigEntryForm(
            @Nullable Disposable parent,
            @NotNull ConnectionHandler connection) {
        super(parent, connection.getProject());
        this.connection = connection;

        initHeader();
        initFeatureInfo();
        initEditor();
        initDefaults();
    }

    private void initHeader() {
        headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    }

    private void initFeatureInfo() {
        DBNHintForm hintForm = new DBNHintForm(this, TextContent.plain(FEATURE_HINT), null, true);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);

        HyperLinkForm hyperLinkForm = HyperLinkForm.create(
                "Documentation:",
                "Centralized Config Provider",
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

    private void initDefaults() {
        keyTextField.setText(DEFAULT_ENTRY_KEY);
        keyTextField.setToolTipText("Entry key: starts with a letter, then letters, digits, dot, dash, or underscore.");
        setStatus("Specify a key and JSON payload, then click Create.");
    }

    void createEntry(Runnable onSuccess) {
        CreateEntryInput input = readAndValidateInput();
        if (input == null) return;

        DBDataSourceConfigEntryImpl entry = new DBDataSourceConfigEntryImpl(connection, input.key(), input.value());
        ObjectManagementService.getInstance(getProject()).createObject(entry, new OutcomeHandler.HighPriority() {
            @Override
            public void handle(Outcome outcome) {
                reloadEntryList();
                setStatus("Created '" + input.key() + "'.");
                onSuccess.run();
            }
        });
    }

    private @Nullable CreateEntryInput readAndValidateInput() {
        String key = keyTextField.getText().trim();
        String value = readEditorText().trim();

        if (!validateKey(key)) return null;
        if (!validateJson(value)) return null;
        return new CreateEntryInput(key, value);
    }

    private boolean validateKey(@NotNull String key) {
        if (isEmpty(key)) {
            Messages.showWarningDialog(getProject(), "Validation", "Entry key is required.");
            return false;
        }
        if (key.length() > KEY_MAX_LENGTH) {
            Messages.showWarningDialog(getProject(), "Validation", "Entry key must be at most " + KEY_MAX_LENGTH + " characters.");
            return false;
        }
        if (!KEY_PATTERN.matcher(key).matches()) {
            Messages.showWarningDialog(
                    getProject(),
                    "Validation",
                    "Invalid entry key. It must start with a letter and use only letters, digits, '.', '-', or '_'.");
            return false;
        }
        return true;
    }

    private boolean validateJson(@NotNull String value) {
        if (isEmpty(value)) {
            Messages.showWarningDialog(getProject(), "Validation", "JSON payload is required.");
            return false;
        }

        try {
            Json.readAsMap(value);
            return true;
        } catch (Exception e) {
            Messages.showErrorDialog(getProject(), "Invalid JSON payload", e);
            return false;
        }
    }

    private void reloadEntryList() {
        DBObjectList<?> objectList = connection.getObjectBundle().getObjectList(DBObjectType.DATA_SOURCE_CONFIG_ENTRY);
        if (objectList != null) {
            objectList.reloadInBackground();
        }
    }

    private @NotNull String readEditorText() {
        return jsonEditor == null ? "" : jsonEditor.getDocument().getText();
    }

    private void setStatus(@NotNull String status) {
        statusLabel.setText(status);
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

    private record CreateEntryInput(String key, String value) {}
}
