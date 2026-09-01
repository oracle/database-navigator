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
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMachineLearningInterface;
import com.dbn.ml.ui.MLToolboxFormBase;
import com.dbn.ml.util.MLCSVParser;
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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.common.text.TextContent.html;
import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ClientProperty.LOADING;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setTextSilently;
import static com.dbn.ml.model.source.MLSourceType.OBJECT_STORAGE;
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
    private DBNInfoLabel loadColumnsInfoLabel;

    private final AtomicInteger columnLoadSignature = new AtomicInteger();
    private List<String> discoveredColumns = new ArrayList<>();
    private Set<String> numericColumns = new HashSet<>();
    private String sourceCredentialSchemaName;
    private String sourceCredentialName;
    private boolean sourceTextChanged;

    public MLSourceCloudForm(@Nullable Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
        loadColumnsInfoLabel.setContent(html(this, "info/load_columns_info.html.ft"));
    }

    private void initCredentialComboBoxes() {
        CloudSourceConfig config = getConfig();

        credentialSchemaComboBox
                .initialize(this, SCHEMA)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadSchemas())
                .withValuePreselector(() -> config.getCredentialSchemaName())
                .withValueLoadConsumer(values -> onCredentialSchemasLoaded());

        credentialComboBox.clearValues();
        credentialComboBox
                .initialize(this, CREDENTIAL)
                .withConnectionContext(() -> getConnection())
                .withSchemaContext(() -> getSelectedCredentialSchema())
                .withValueLoader(List::of)
                .withValuePreselector(() -> config.getCredentialName())
                .withObjectFactory(txt("cfg.machineLearning.action.NewCredential"))
                .withValueLoadConsumer(values -> onCredentialsLoaded());

        updateFieldAvailability();
        credentialSchemaComboBox.triggerLoad();
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
        onSelectionChange(credentialSchemaComboBox, schema -> {
            populateCredentials(schema);
            if (!LOADING.is(credentialSchemaComboBox)) {
                updateCredentialSelection();
                invalidateDiscoveredColumns();
            }
        });
        onSelectionChange(credentialComboBox, c -> {
            if (!LOADING.is(credentialSchemaComboBox) &&
                    !LOADING.is(credentialComboBox)) {
                updateCredentialSelection();
                invalidateDiscoveredColumns();
            }
        });

        onTextChange(uriField, e -> invalidateTextSource());
        onTextChange(delimiterField, e -> invalidateTextSource());
        noCredentialCheckBox.addActionListener(e -> {
            updateFieldAvailability();
            updateCredentialSelection();
            invalidateDiscoveredColumns();
        });
        hasHeaderCheckBox.addActionListener(e -> invalidateDiscoveredColumns());
        loadColumnsButton.addActionListener(e -> loadColumnsFromCloud());
    }

    private void invalidateTextSource() {
        if (sourceTextChanged) return;

        sourceTextChanged = true;
        clearDiscoveredColumns();
        notifySourceInvalidated();
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

    private void populateCredentials(DBSchema schema) {
        updateFieldAvailability();
        credentialComboBox.withValueLoader(() -> schema == null ? emptyList() : schema.getCredentials());
        credentialComboBox.reloadValues();
    }

    private void onCredentialSchemasLoaded() {
        if (!isNoCredential() &&
                getSelectedCredentialSchema() == null &&
                updateCredentialSelection()) {
            invalidateDiscoveredColumns();
        }
    }

    private void onCredentialsLoaded() {
        if (!isNoCredential() && getSelectedCredentialSchema() == null) return;

        if (updateCredentialSelection()) {
            invalidateDiscoveredColumns();
        } else {
            notifySourceLoaded();
        }
    }

    private boolean updateCredentialSelection() {
        String schemaName = isNoCredential() ? null : getSelectedCredentialSchemaName();
        String credentialName = isNoCredential() ? null : getSelectedCredential();
        boolean changed = !Objects.equals(sourceCredentialSchemaName, schemaName) ||
                !Objects.equals(sourceCredentialName, credentialName);
        sourceCredentialSchemaName = schemaName;
        sourceCredentialName = credentialName;
        return changed;
    }

    private @Nullable DBSchema getSelectedCredentialSchema() {
        return getSelection(credentialSchemaComboBox);
    }

    private void notifySourceChanged() {
        ensureParentFrom(MLSourceForm.class).notifySourceChanged(OBJECT_STORAGE);
    }

    private void notifySourceInvalidated() {
        ensureParentFrom(MLSourceForm.class).notifySourceInvalidated(OBJECT_STORAGE);
    }

    private void notifySourceLoaded() {
        ensureParentFrom(MLSourceForm.class).notifySourceLoaded(OBJECT_STORAGE);
    }

    public String getSelectedUri() {
        return uriField.getText().trim();
    }

    private @Nullable String getSelectedCredential() {
        if (noCredentialCheckBox.isSelected()) return null;
        return getObjectName(getSelection(credentialComboBox));
    }

    private @Nullable String getSelectedCredentialSchemaName() {
        return getObjectName(getSelectedCredentialSchema());
    }

    private String getSelectedDelimiter() {
        String delimiter = delimiterField.getText();
        return delimiter == null || delimiter.isEmpty() ? "," : delimiter;
    }

    private boolean hasHeader() {
        return hasHeaderCheckBox.isSelected();
    }

    private boolean isNoCredential() {
        return noCredentialCheckBox.isSelected();
    }

    boolean isConfiguredSourceSelected() {
        CloudSourceConfig config = getConfig();
        boolean noCredential = isNoCredential();
        if (!Objects.equals(getSelectedUri(), trim(config.getFileUri())) ||
                noCredential != config.isNoCredential() ||
                !Objects.equals(getSelectedDelimiter(), normalizeDelimiter(config.getDelimiter())) ||
                hasHeader() != config.isHasHeader()) {
            return false;
        }

        return noCredential ||
                Objects.equals(getSelectedCredentialSchemaName(), config.getCredentialSchemaName()) &&
                Objects.equals(getSelectedCredential(), config.getCredentialName());
    }

    private static String normalizeDelimiter(String delimiter) {
        return delimiter == null || delimiter.isEmpty() ? "," : delimiter;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public List<String> getDiscoveredColumns() {
        return List.copyOf(discoveredColumns);
    }

    private void invalidateDiscoveredColumns() {
        clearDiscoveredColumns();
        notifySourceChanged();
    }

    private void clearDiscoveredColumns() {
        columnLoadSignature.incrementAndGet();
        discoveredColumns.clear();
        numericColumns.clear();
        resetLoadColumnsButton();
    }

    private void loadColumnsFromCloud() {
        String uri = getSelectedUri();
        if (uri.isEmpty() || !uri.startsWith("https://")) {
            return;
        }

        if (!isNoCredential() && getSelectedCredential() == null) return;

        String credential = getSelectedCredential();
        String delimiter = getSelectedDelimiter();
        boolean headerPresent = hasHeader();
        sourceTextChanged = false;

        ConnectionHandler connection = getConnection();
        int signature = columnLoadSignature.incrementAndGet();

        loadColumnsButton.setEnabled(false);
        loadColumnsButton.setText(txt("cfg.machineLearning.button.Loading"));

        Background.run(() -> {
            try {
                String sample = DatabaseInterfaceInvoker.load(HIGH,
                        txt("prc.machineLearning.title.LoadingColumns"),
                        txt("prc.machineLearning.text.ReadingCloudCsvSample"),
                        connection.getProject(),
                        connection.getConnectionId(),
                        conn -> {
                            DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();
                            return mlInterface.getCloudCsvSample(conn, credential, uri);
                        });
                MLCSVParser.Profile profile = MLCSVParser.profile(
                        new StringReader(sample == null ? "" : sample),
                        delimiter,
                        headerPresent,
                        MLCSVParser.CLOUD_SAMPLE_ROWS);

                Dispatch.run(loadColumnsButton, () -> {
                    if (!matchesColumnLoadSignature(signature)) return;

                    discoveredColumns = new ArrayList<>(profile.getColumns());
                    numericColumns = new HashSet<>(profile.getNumericColumns());
                    resetLoadColumnsButton();
                    notifySourceLoaded();
                });
            } catch (Exception ex) {
                log.error("Failed to load columns from cloud source", ex);
                Dispatch.run(loadColumnsButton, () -> {
                    if (!matchesColumnLoadSignature(signature)) return;

                    resetLoadColumnsButton();
                    Messages.showErrorDialog(
                            getProject(),
                            txt("msg.machineLearning.title.MLToolboxError"),
                            txt("msg.machineLearning.error.CloudColumnsLoadFailed", ex.getMessage()));
                });
            }
        });
    }

    private boolean matchesColumnLoadSignature(int signature) {
        return signature == columnLoadSignature.get();
    }

    private void resetLoadColumnsButton() {
        loadColumnsButton.setEnabled(true);
        loadColumnsButton.setText(txt("cfg.machineLearning.button.LoadColumns"));
    }

    private CloudSourceConfig getConfig() {
        return getMLRequest().getSourceConfig().getCloudSourceConfig();
    }

    @Override
    public void resetFormChanges() {
        columnLoadSignature.incrementAndGet();
        resetLoadColumnsButton();

        CloudSourceConfig config = getConfig();
        setTextSilently(uriField, config.getFileUri() != null ? config.getFileUri() : "");
        noCredentialCheckBox.setSelected(config.isNoCredential());
        setTextSilently(delimiterField, config.getDelimiter() != null ? config.getDelimiter() : ",");
        hasHeaderCheckBox.setSelected(config.isHasHeader());
        sourceCredentialSchemaName = config.isNoCredential() ? null : config.getCredentialSchemaName();
        sourceCredentialName = config.isNoCredential() ? null : config.getCredentialName();
        initCredentialComboBoxes();
        discoveredColumns = new ArrayList<>(config.getDiscoveredColumns());
        numericColumns = new HashSet<>(config.getNumericColumns());
        sourceTextChanged = false;
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
