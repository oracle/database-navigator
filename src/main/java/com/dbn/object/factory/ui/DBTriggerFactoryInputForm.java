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
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.misc.DBNMultiSelectComboBox;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseIdentifierCase;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.psql.PSQLFileType;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.object.DBDataset;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.object.factory.ObjectFactoryManager;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBTriggerEvent;
import com.dbn.object.type.DBTriggerTarget;
import com.dbn.object.type.DBTriggerType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.List;

import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.disableFormField;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_BODY;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_EVENTS;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_FOR_EACH_ROW;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_FUNCTION_NAME;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TARGET;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TARGET_DATASET;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TARGET_SCHEMA;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TYPE;
import static com.dbn.object.type.DBObjectType.DATASET;
import static com.dbn.object.type.DBObjectType.DATASET_TRIGGER;
import static com.dbn.object.type.DBObjectType.SCHEMA;

public class DBTriggerFactoryInputForm extends DBSchemaObjectFactoryInputForm {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel editorPanel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;
    private JTextField nameTextField;
    private JLabel triggerFunctionNameLabel;
    private JTextField triggerFunctionNameTextField;
    private JLabel targetDatasetLabel;
    private JLabel triggerTargetLabel;
    private JLabel targetSchemaLabel;
    private JLabel triggerTypeLabel;
    private JLabel triggerEventsLabel;
    private JLabel triggerForEachRowLabel;
    private DBObjectSelector<DBDataset> targetDatasetComboBox;
    private DBNComboBox<DBTriggerTarget> triggerTargetComboBox;
    private DBObjectSelector<DBSchema> targetSchemaComboBox;
    private DBNComboBox<DBTriggerType> triggerTypeComboBox;
    private DBNMultiSelectComboBox<DBTriggerEvent> triggerEventsComboBox;
    private JCheckBox triggerForEachRowCheckBox;
    private JCheckBox preserveCaseCheckBox;
    private DBNInfoLabel preserveCaseInfoLabel;
    private EditorEx sqlEditor;
    private Document document;

    private String triggerName;
    public DBTriggerFactoryInputForm(@NotNull DBNComponent parent, DBObjectSpec input) {
        super(parent, input);

        initContextComponents();
        initHeaderForm();
        initTriggerFunctionName();
        initTargetDataset();
        initTriggerTarget();
        initTriggerType();
        initTriggerEvents();
        initPreserveCaseFields();
        resetFormChanges();
        triggerName = nameTextField.getText().trim();

        onSelectionChange(triggerEventsComboBox, e -> validateFormFields());
        onTextChange(nameTextField, e -> {
            updateTriggerFunctionName();
            validateFormFields();
        });
        whenFirstShown(this::initEditor);
    }

    private void initTriggerFunctionName() {
        boolean supported = supports(TRIGGER_FUNCTION_NAME);
        triggerFunctionNameLabel.setVisible(supported);
        triggerFunctionNameTextField.setVisible(supported);
        if (supported) {
            onTextChange(triggerFunctionNameTextField, e -> validateFormFields());
        }
    }

    private void updateTriggerFunctionName() {
        if (!supports(TRIGGER_FUNCTION_NAME)) return;

        String newTriggerName = nameTextField.getText().trim();
        if (isEmptyOrSpaces(newTriggerName)) return;

        String generatedFunctionName = triggerName + "_function";
        if (triggerFunctionNameTextField.getText().trim().equals(generatedFunctionName)) {
            triggerFunctionNameTextField.setText(newTriggerName + "_function");
        }
        triggerName = newTriggerName;
    }
    private void initTargetDataset() {
        boolean datasetTrigger = supports(TRIGGER_TARGET_DATASET);
        targetDatasetLabel.setVisible(datasetTrigger);
        targetDatasetComboBox.setVisible(datasetTrigger);
        triggerForEachRowLabel.setVisible(datasetTrigger);
        triggerForEachRowCheckBox.setVisible(datasetTrigger);
        if (datasetTrigger && isRowTriggerMandatory()) {
            triggerForEachRowCheckBox.setSelected(true);
            triggerForEachRowCheckBox.setEnabled(false);
        }
        if (!datasetTrigger) return;

        targetDatasetComboBox
                .initialize(this, DATASET)
                .withConnectionContext(this::getConnection)
                .withSchemaContext(input::getSchema)
                .withValueLoader(() -> input.getSchema().getDatasets())
                .withValuePreselector(() -> TRIGGER_TARGET_DATASET.value(input))
                .withValueLoadConsumer(values -> validateFormFields());
        disableFormField(targetDatasetComboBox, "READ_ONLY");
        targetDatasetComboBox.triggerLoad();
    }

    private void initTriggerTarget() {
        List<DBTriggerTarget> triggerTargets = getSupportedValues(TRIGGER_TARGET);
        boolean targetSupported = supports(TRIGGER_TARGET);
        triggerTargetLabel.setVisible(targetSupported);
        triggerTargetComboBox.setVisible(targetSupported);
        targetSchemaLabel.setVisible(false);
        targetSchemaComboBox.setVisible(false);
        if (!targetSupported) return;

        triggerTargetComboBox.setValues(triggerTargets);
        triggerTargetComboBox.setSelectedValue(TRIGGER_TARGET.value(input));
        if (triggerTargets.size() == 1) {
            triggerTargetComboBox.setEnabled(false);
        }
        onSelectionChange(triggerTargetComboBox, target -> {
            updateTargetSchemaVisibility();
            validateFormFields();
        });

        if (supports(TRIGGER_TARGET_SCHEMA)) {
            targetSchemaComboBox
                    .initialize(this, SCHEMA)
                    .withConnectionContext(this::getConnection)
                    .withValueLoader(this::loadSchemas)
                    .withValuePreselector(() -> TRIGGER_TARGET_SCHEMA.value(input))
                    .withValueLoadConsumer(values -> validateFormFields());
            onSelectionChange(targetSchemaComboBox, e -> validateFormFields());
            targetSchemaComboBox.triggerLoad();
        }
        updateTargetSchemaVisibility();
    }

    private List<DBSchema> loadSchemas() {
        return getConnection().getObjectBundle().getSchemas();
    }

    private void updateTargetSchemaVisibility() {
        boolean visible = supports(TRIGGER_TARGET_SCHEMA) &&
                triggerTargetComboBox.getSelectedValue() == DBTriggerTarget.SCHEMA;
        targetSchemaLabel.setVisible(visible);
        targetSchemaComboBox.setVisible(visible);
    }

    private void initTriggerType() {
        List<DBTriggerType> triggerTypes = getSupportedValues(TRIGGER_TYPE);
        boolean supported = supports(TRIGGER_TYPE);
        triggerTypeLabel.setVisible(supported);
        triggerTypeComboBox.setVisible(supported);
        if (!supported) return;

        triggerTypeComboBox.setValues(triggerTypes);
        triggerTypeComboBox.setSelectedValue(TRIGGER_TYPE.value(input));
    }

    private void initTriggerEvents() {
        DBTriggerEvent[] selectedEvents = TRIGGER_EVENTS.values(input);
        List<DBTriggerEvent> triggerEvents = getSupportedValues(TRIGGER_EVENTS);
        triggerEventsComboBox.setSingleSelection(!allowsMultiple(TRIGGER_EVENTS));
        triggerEventsComboBox.setValues(triggerEvents);
        triggerEventsComboBox.setSelectedItems(selectedEvents);
    }

    private void initPreserveCaseFields() {
        preserveCaseInfoLabel.setContent(getPreserveCaseInfoText());
    }

    private void initEditor() {
        ConnectionHandler connection = getConnection();
        DBLanguageDialect languageDialect = connection.getLanguageDialect(PSQLLanguage.INSTANCE);
        if (languageDialect == null) {
            languageDialect = PSQLLanguage.INSTANCE.getMainLanguageDialect();
        }

        DBLanguagePsiFile triggerBodyFile = DBLanguagePsiFile.createFromText(
                ensureProject(),
                "trigger.psql",
                languageDialect,
                TRIGGER_BODY.value(input),
                connection,
                input.getSchemaId());
        if (triggerBodyFile == null) return;

        document = Documents.ensureDocument(triggerBodyFile);
        Documents.onDocumentChanged(document, this, e -> validateFormFields());
        sqlEditor = Editors.createEditor(document, ensureProject(), triggerBodyFile.getVirtualFile(), PSQLFileType.INSTANCE);
        sqlEditor.setEmbeddedIntoDialogWrapper(true);
        sqlEditor.setPlaceholder(txt("app.object.placeholder.TriggerBody"));
        sqlEditor.setShowPlaceholderWhenFocused(true);
        Editors.initEditorHighlighter(sqlEditor, PSQLLanguage.INSTANCE, connection);

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
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(targetDatasetLabel, targetDatasetComboBox);
        alignerData.registerFieldGroup(triggerTargetLabel, triggerTargetComboBox);
        alignerData.registerFieldGroup(targetSchemaLabel, targetSchemaComboBox);
        alignerData.registerFieldGroup(triggerFunctionNameLabel, triggerFunctionNameTextField);
        alignerData.registerFieldGroup(triggerTypeLabel, triggerTypeComboBox);
        alignerData.registerFieldGroup(triggerEventsLabel, triggerEventsComboBox);
        alignerData.registerFieldGroup(triggerForEachRowLabel, triggerForEachRowCheckBox);
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField,
                n -> isNotEmptyOrSpaces(n.trim()),
                txt("msg.objects.error.ObjectNameRequired", getObjectType().getDisplayName()));
        addTextValidation(nameTextField,
                n -> isEmptyOrSpaces(n) || isWord(n.trim()),
                txt("msg.objects.error.ValidObjectNameRequired", getObjectType().getDisplayName()));
        if (requires(TRIGGER_FUNCTION_NAME)) {
            addTextValidation(triggerFunctionNameTextField,
                    n -> isNotEmptyOrSpaces(n.trim()),
                    txt("msg.objects.error.TriggerFunctionNameRequired"));
            addTextValidation(triggerFunctionNameTextField,
                    n -> isEmptyOrSpaces(n) || isWord(n.trim()),
                    txt("msg.objects.error.ValidTriggerFunctionNameRequired"));
        }
        if (requires(TRIGGER_TYPE)) {
            addSelectionValidation(triggerTypeComboBox,
                    txt("msg.objects.error.SelectObject", txt("app.object.label.TriggerType")));
        }
        if (requires(TRIGGER_EVENTS)) {
            addValidation(triggerEventsComboBox, component -> hasSelectedTriggerEvent() ? null : txt("msg.objects.error.TriggerEventRequired"));
        }
        if (requires(TRIGGER_BODY)) {
            addValidation(editorPanel, c -> validateTriggerBody());
        }
        if (requires(TRIGGER_TARGET_DATASET)) {
            addSelectionValidation(targetDatasetComboBox,
                    txt("msg.objects.error.SelectObject", txt("app.object.label.TriggerTargetDataset")));
        }
        if (supports(TRIGGER_TARGET)) {
            if (requires(TRIGGER_TARGET)) {
                addSelectionValidation(triggerTargetComboBox,
                        txt("msg.objects.error.SelectObject", txt("app.objects.property.TriggerTarget")));
            }
            if (requires(TRIGGER_TARGET_SCHEMA)) {
                addValidation(targetSchemaComboBox,
                        component -> triggerTargetComboBox.getSelectedValue() == DBTriggerTarget.SCHEMA &&
                                getSelection(targetSchemaComboBox) == null ? txt("msg.shared.error.SelectTargetSchema") : null);
            }
        }
    }

    private String validateTriggerBody() {
        String triggerBody = document == null ? "" : document.getText().trim();
        return isEmptyOrSpaces(triggerBody) ? txt("msg.objects.error.TriggerBodyRequired") : null;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        super.applyFormChanges();
        if (supports(TRIGGER_FUNCTION_NAME)) {
            input.setAttributeValue(TRIGGER_FUNCTION_NAME, triggerFunctionNameTextField.getText().trim());
        }
        if (supports(TRIGGER_TYPE)) {
            input.setAttributeValue(TRIGGER_TYPE, triggerTypeComboBox.getSelectedValue());
        }
        input.setAttributeValues(TRIGGER_EVENTS, getSelectedTriggerEvents());
        if (supports(TRIGGER_TARGET)) {
            input.setAttributeValue(TRIGGER_TARGET, triggerTargetComboBox.getSelectedValue());
            if (supports(TRIGGER_TARGET_SCHEMA)) {
                DBSchema targetSchema = getSelectedTargetSchema();
                input.setAttributeValue(TRIGGER_TARGET_SCHEMA,
                        triggerTargetComboBox.getSelectedValue() == DBTriggerTarget.SCHEMA && targetSchema != null ?
                                targetSchema.getName() : null);
            }
        }
        if (supports(TRIGGER_TARGET_DATASET)) {
            input.setAttributeValue(TRIGGER_TARGET_DATASET,
                    getSelectedTargetDataset() == null ? null : getSelectedTargetDataset().getName());
        }
        if (supports(TRIGGER_FOR_EACH_ROW)) {
            input.setAttributeValue(TRIGGER_FOR_EACH_ROW, triggerForEachRowCheckBox.isSelected());
        }
        input.setAttributeValue(TRIGGER_BODY, document == null ? "" : document.getText().trim());
        input.setIdentifierCase(getSelectedIdentifierCase());
    }

    private DBTriggerEvent[] getSelectedTriggerEvents() {
        return triggerEventsComboBox.getSelectedItems().toArray(DBTriggerEvent[]::new);
    }

    private boolean hasSelectedTriggerEvent() {
        return !triggerEventsComboBox.getSelectedItems().isEmpty();
    }

    private boolean isRowTriggerMandatory() {
        return requires(TRIGGER_FOR_EACH_ROW, true);
    }

    private DBDataset getSelectedTargetDataset() {
        return getSelection(targetDatasetComboBox);
    }

    private DBSchema getSelectedTargetSchema() {
        return getSelection(targetSchemaComboBox);
    }

    @Override
    protected void initStatePersistence() {
        StateAttributes state = ObjectFactoryManager.getInstance(ensureProject()).getState(getObjectType());
        initPersistence(preserveCaseCheckBox, state, "preserve-identifier-case");
        if (supports(TRIGGER_TYPE)) {
            initPersistence(triggerTypeComboBox, state, "trigger-type");
        }
        if (supports(TRIGGER_TARGET)) {
            initPersistence(triggerTargetComboBox, state, "trigger-target");
        }
        if (getObjectType() == DATASET_TRIGGER) {
            initPersistence(triggerForEachRowCheckBox, state, "for-each-row");
            if (isRowTriggerMandatory()) {
                triggerForEachRowCheckBox.setSelected(true);
                triggerForEachRowCheckBox.setEnabled(false);
            }
        }
        initPersistence(triggerEventsComboBox, state, "trigger-event", DBTriggerEvent::getName);
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

    @Override
    public void resetFormChanges() {
        super.resetFormChanges();
        if (supports(TRIGGER_FUNCTION_NAME)) {
            triggerFunctionNameTextField.setText(TRIGGER_FUNCTION_NAME.value(input));
        }
        if (supports(TRIGGER_TYPE)) {
            triggerTypeComboBox.setSelectedValue(TRIGGER_TYPE.value(input));
        }
        if (supports(TRIGGER_TARGET)) {
            triggerTargetComboBox.setSelectedValue(TRIGGER_TARGET.value(input));
            if (supports(TRIGGER_TARGET_SCHEMA)) {
                targetSchemaComboBox
                        .withValuePreselector(() -> TRIGGER_TARGET_SCHEMA.value(input))
                        .reloadValues();
            }
            updateTargetSchemaVisibility();
        }
        if (supports(TRIGGER_FOR_EACH_ROW)) {
            triggerForEachRowCheckBox.setSelected(isRowTriggerMandatory() || TRIGGER_FOR_EACH_ROW.is(input));
        }
        if (supports(TRIGGER_TARGET_DATASET) && isInitialized()) {
            targetDatasetComboBox
                    .withValuePreselector(() -> TRIGGER_TARGET_DATASET.value(input))
                    .reloadValues();
        }
        initTriggerEvents();
    }

    @Override
    public void focus() {
        getNameTextField().requestFocus();
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
