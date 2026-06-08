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

package com.dbn.ml.ui.source;

import com.dbn.common.cloud.CloudSourceConfig;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMachineLearningInterface;
import com.dbn.ml.ui.MLToolboxForm;
import com.dbn.ml.ui.MLToolboxFormBase;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectSelector;
import com.intellij.openapi.Disposable;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.CREDENTIAL;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static java.util.Collections.emptyList;


/**
 * Form for cloud object storage source selection.
 * Supports OCI, AWS S3, Azure Blob, and GCP via DBMS_CLOUD.
 */
@Slf4j
public class MLSourceCloudForm extends MLToolboxFormBase {
    private JPanel mainPanel;
    private JLabel uriLabel;
    private JLabel credentialSchemaLabel;
    private JLabel credentialLabel;
    private JLabel delimiterLabel;
    private JLabel hasHeaderLabel;
    private JTextField uriField;
    private JCheckBox noCredentialCheckBox;
    private DBObjectSelector<DBSchema> credentialSchemaComboBox;
    private DBObjectSelector<DBCredential> credentialComboBox;
    private JTextField delimiterField;
    private JCheckBox hasHeaderCheckBox;
    private JButton loadColumnsButton;

    private List<String> discoveredColumns = new ArrayList<>();
    private Set<String> numericColumns = new HashSet<>();

    public MLSourceCloudForm(@Nullable Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
        initCredentialComboBoxes();
    }

    private void initCredentialComboBoxes() {
        CloudSourceConfig config = getConfig();

        credentialSchemaComboBox
                .initialize(this, SCHEMA)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadSchemas())
                .withValuePreselector(() -> config.getCredentialSchemaName())
                .triggerLoad();

        credentialComboBox
                .initialize(this, CREDENTIAL)
                .withConnectionContext(() -> getConnection())
                .withSchemaContext(() -> getSelectedCredentialSchema())
                .withValueLoader(() -> loadCredentials())
                .withValuePreselector(() -> config.getCredentialName())
                .withObjectFactory(txt("cfg.machineLearning.action.NewCredential"))
                .triggerLoad();

        updateFieldAvailability();
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(uriLabel, uriField);
        alignerData.registerFieldGroup(credentialSchemaLabel, credentialSchemaComboBox);
        alignerData.registerFieldGroup(credentialLabel, credentialComboBox);
        alignerData.registerFieldGroup(delimiterLabel, delimiterField);
        alignerData.registerFieldGroup(hasHeaderLabel, hasHeaderCheckBox);
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(
                () -> !noCredentialCheckBox.isSelected() && isValid(getSelectedCredentialSchema()),
                array(credentialComboBox));
        fieldAdapter.initFieldsAvailability(
                () -> !noCredentialCheckBox.isSelected(),
                array(credentialSchemaComboBox, credentialSchemaLabel, credentialLabel));
    }

    @Override
    protected void initEventListeners() {
        onSelectionChange(credentialSchemaComboBox, s -> populateCredentials());
        onTextChange(uriField, e -> notifySourceChanged());
        noCredentialCheckBox.addActionListener(e -> updateFieldAvailability());
        loadColumnsButton.addActionListener(e -> loadColumnsFromCloud());
    }

    @Override
    protected void initValidation() {
        addValidation(uriField,
                t -> {
                    String text = t.getText().trim();
                    return !text.isEmpty() && text.startsWith("https://");
                },
                txt("msg.machineLearning.error.HttpsUriRequired"));
    }

    private void populateCredentials() {
        updateFieldAvailability();
        credentialComboBox.reloadValues();
    }

    private @Nullable DBSchema getSelectedCredentialSchema() {
        return getSelection(credentialSchemaComboBox);
    }

    private List<DBCredential> loadCredentials() {
        DBSchema schema = getSelectedCredentialSchema();
        if (schema == null) return emptyList();
        return schema.getCredentials();
    }

    private void notifySourceChanged() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm != null) {
            toolboxForm.onSourceChanged();
        }
    }

    public String getSelectedUri() {
        return uriField.getText().trim();
    }

    public @Nullable String getSelectedCredential() {
        if (noCredentialCheckBox.isSelected()) return null;
        DBCredential credential = getSelection(credentialComboBox);
        return credential == null ? null : credential.getName();
    }

    public @Nullable String getSelectedCredentialSchemaName() {
        DBSchema schema = getSelectedCredentialSchema();
        return schema == null ? null : schema.getName();
    }

    public String getSelectedDelimiter() {
        String delimiter = delimiterField.getText();
        return (delimiter != null && !delimiter.isEmpty()) ? delimiter : ",";
    }

    public List<String> getDiscoveredColumns() {
        return discoveredColumns;
    }

    private void loadColumnsFromCloud() {
        String uri = uriField.getText().trim();
        if (uri.isEmpty() || !uri.startsWith("https://")) {
            return;
        }

        String credential = getSelectedCredential();
        String delimiter = getSelectedDelimiter();

        ConnectionHandler connection = getConnection();

        loadColumnsButton.setEnabled(false);
        loadColumnsButton.setText(txt("cfg.machineLearning.button.Loading"));

        Background.run(() -> {
            try {
                DatabaseInterfaceInvoker.execute(HIGH,
                        txt("prc.machineLearning.title.LoadingColumns"),
                        txt("prc.machineLearning.text.ReadingCloudCsvHeader"),
                        connection.getProject(),
                        connection.getConnectionId(),
                        conn -> {
                            DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();
                            String fileHead = mlInterface.getCloudCsvHeader(conn, credential, uri);

                            List<String> columns = parseHeaderLine(fileHead, delimiter);
                            Set<String> numeric = detectNumericColumns(fileHead, columns, delimiter);

                            Dispatch.run(loadColumnsButton, () -> {
                                discoveredColumns = columns;
                                numericColumns = numeric;
                                loadColumnsButton.setEnabled(true);
                                loadColumnsButton.setText(txt("cfg.machineLearning.button.LoadColumns"));
                                notifySourceChanged();
                            });
                        });
            } catch (Exception ex) {
                log.error("Failed to load columns from cloud source", ex);
                Dispatch.run(loadColumnsButton, () -> {
                    loadColumnsButton.setEnabled(true);
                    loadColumnsButton.setText(txt("cfg.machineLearning.button.LoadColumns"));
                });
            }
        });
    }

    private List<String> parseHeaderLine(String fileHead, String delimiter) {
        List<String> columns = new ArrayList<>();
        if (fileHead == null || fileHead.isEmpty()) return columns;

        // Extract first line only
        int newlineIdx = fileHead.indexOf('\n');
        String headerLine = newlineIdx >= 0 ? fileHead.substring(0, newlineIdx) : fileHead;
        headerLine = headerLine.replace("\r", "").trim();

        if (headerLine.isEmpty()) return columns;

        String[] parts = headerLine.split(java.util.regex.Pattern.quote(delimiter));
        for (String part : parts) {
            String col = part.trim();
            // Remove surrounding quotes if present
            if (col.length() >= 2 && col.startsWith("\"") && col.endsWith("\"")) {
                col = col.substring(1, col.length() - 1);
            }
            if (!col.isEmpty()) {
                columns.add(col);
            }
        }
        return columns;
    }

    private Set<String> detectNumericColumns(String fileHead, List<String> columns, String delimiter) {
        Set<String> numeric = new HashSet<>();
        if (fileHead == null || columns.isEmpty()) return numeric;

        String quotedDelimiter = java.util.regex.Pattern.quote(delimiter);
        String[] lines = fileHead.split("\\r?\\n");
        // Skip header line (index 0), use first data line to detect types
        for (int lineIdx = 1; lineIdx < lines.length; lineIdx++) {
            String line = lines[lineIdx].trim();
            if (line.isEmpty()) continue;
            String[] values = line.split(quotedDelimiter);
            for (int i = 0; i < columns.size() && i < values.length; i++) {
                String val = values[i].trim();
                if (val.isEmpty()) continue;
                try {
                    Double.parseDouble(val);
                    numeric.add(columns.get(i));
                } catch (NumberFormatException ignored) {
                    // not numeric — leave out of set
                }
            }
            break; // first data row is enough
        }
        return numeric;
    }

    private CloudSourceConfig getConfig() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return new CloudSourceConfig();
        return toolboxForm.getMLRequest().getSourceConfig().getCloudSourceConfig();
    }

    @Override
    public void resetFormChanges() {
        CloudSourceConfig config = getConfig();
        uriField.setText(config.getFileUri() != null ? config.getFileUri() : "");
        noCredentialCheckBox.setSelected(config.isNoCredential());
        delimiterField.setText(config.getDelimiter() != null ? config.getDelimiter() : ",");
        hasHeaderCheckBox.setSelected(config.isHasHeader());
        initCredentialComboBoxes();
    }

    @Override
    public void applyFormChanges() {
        CloudSourceConfig config = getConfig();
        config.setFileUri(uriField.getText().trim());
        config.setNoCredential(noCredentialCheckBox.isSelected());
        config.setCredentialSchemaName(noCredentialCheckBox.isSelected() ? null : getSelectedObjectName(credentialSchemaComboBox, config.getCredentialSchemaName()));
        config.setCredentialName(noCredentialCheckBox.isSelected() ? null : getSelectedObjectName(credentialComboBox, config.getCredentialName()));
        config.setDelimiter(delimiterField.getText());
        config.setHasHeader(hasHeaderCheckBox.isSelected());
        config.setDiscoveredColumns(new ArrayList<>(discoveredColumns));
        config.setNumericColumns(new HashSet<>(numericColumns));
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
