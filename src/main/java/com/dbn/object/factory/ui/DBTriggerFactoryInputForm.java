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

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.ui.alignment.FieldAlignerData;
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
import com.dbn.language.psql.PSQLFileType;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.object.DBDataset;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.object.factory.ObjectFactoryManager;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBTriggerEvent;
import com.dbn.object.type.DBTriggerType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.dbn.common.ui.Layouts.horizontalBoxLayout;
import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.disableFormField;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.factory.model.DBObjectAttributeType.OBJECT_DETAIL;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_EVENTS;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_FOR_EACH_ROW;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TARGET_DATASET;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TYPE;
import static com.dbn.object.type.DBObjectType.DATASET;
import static com.dbn.object.type.DBObjectType.DATASET_TRIGGER;

public class DBTriggerFactoryInputForm extends DBSchemaObjectFactoryInputForm {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel editorPanel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;
    private JTextField nameTextField;
    private JLabel targetDatasetLabel;
    private JLabel triggerTypeLabel;
    private JLabel triggerEventsLabel;
    private JLabel triggerForEachRowLabel;
    private DBObjectSelector<DBDataset> targetDatasetComboBox;
    private DBNComboBox<DBTriggerType> triggerTypeComboBox;
    private JPanel triggerEventsPanel;
    private JCheckBox triggerForEachRowCheckBox;
    private JCheckBox preserveCaseCheckBox;
    private DBNInfoLabel preserveCaseInfoLabel;
    private EditorEx sqlEditor;
    private Document document;

    private List<DBTriggerEvent> triggerEvents = List.of();
    private List<JToggleButton> triggerEventButtons = List.of();

    public DBTriggerFactoryInputForm(@NotNull DBNComponent parent, DBObjectSpec input) {
        super(parent, input);

        initContextComponents();
        initHeaderForm();
        initTargetDataset();
        initTriggerType();
        initTriggerEvents();
        initPreserveCaseFields();
        resetFormChanges();

        onTextChange(nameTextField, e -> validateFormFields());
        whenFirstShown(this::initEditor);
    }

    private void initTargetDataset() {
        boolean datasetTrigger = getObjectType() == DATASET_TRIGGER;
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
                .withValuePreselector(() -> TRIGGER_TARGET_DATASET.of(input))
                .withValueLoadConsumer(values -> validateFormFields());
        disableFormField(targetDatasetComboBox, "READ_ONLY");
        targetDatasetComboBox.triggerLoad();
    }

    private void initTriggerType() {
        List<DBTriggerType> triggerTypes = getSupportedValues(TRIGGER_TYPE);
        triggerTypeComboBox.setValues(triggerTypes);
        triggerTypeComboBox.setSelectedValue(TRIGGER_TYPE.of(input));
    }

    private void initTriggerEvents() {
        DBTriggerEvent[] selectedEvents = TRIGGER_EVENTS.of(input);
        triggerEvents = getSupportedValues(TRIGGER_EVENTS);
        triggerEventButtons = new ArrayList<>();
        triggerEventsPanel.removeAll();
        horizontalBoxLayout(triggerEventsPanel);
        for (DBTriggerEvent event : triggerEvents) {
            if (!triggerEventButtons.isEmpty()) {
                triggerEventsPanel.add(Box.createHorizontalStrut(8));
            }
            JToggleButton toggleButton = new JToggleButton(event.getName().toUpperCase(Locale.ROOT));
            toggleButton.setHorizontalTextPosition(SwingConstants.LEFT);
            Insets margin = toggleButton.getMargin();
            toggleButton.setMargin(JBUI.insets(margin.top, 8, margin.bottom, 8));
            toggleButton.setSelected(isSelected(event, selectedEvents));
            toggleButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            toggleButton.setMaximumSize(toggleButton.getPreferredSize());
            toggleButton.setAlignmentY(Component.CENTER_ALIGNMENT);
            toggleButton.addItemListener(e -> {
                if (!allowsMultipleTriggerEvents() && toggleButton.isSelected()) {
                    for (JToggleButton button : triggerEventButtons) {
                        if (button != toggleButton) button.setSelected(false);
                    }
                }
                updateTriggerEventButton(toggleButton);
                validateFormFields();
            });
            updateTriggerEventButton(toggleButton);
            triggerEventsPanel.add(toggleButton);
            triggerEventButtons.add(toggleButton);
        }
        triggerEventsPanel.revalidate();
        triggerEventsPanel.repaint();
    }

    private static boolean isSelected(DBTriggerEvent event, DBTriggerEvent[] selectedEvents) {
        return selectedEvents != null && Arrays.asList(selectedEvents).contains(event);
    }

    private static void updateTriggerEventButton(JToggleButton button) {
        boolean selected = button.isSelected();
        button.setIcon(selected ? Icons.ACTION_CHECK : Icons.COMMON_EMPTY);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(true);
        button.setForeground(Colors.getLabelForeground());
        //button.setPreferredSize(new Dimension(200, -1));
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
                OBJECT_DETAIL.of(input),
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
        alignerData.registerFieldGroup(triggerTypeLabel, triggerTypeComboBox);
        alignerData.registerFieldGroup(triggerEventsLabel, triggerEventsPanel);
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
        addSelectionValidation(triggerTypeComboBox,
                txt("msg.objects.error.SelectObject", txt("app.object.label.TriggerType")));
        addValidation(triggerEventsPanel, panel -> hasSelectedTriggerEvent() ? null : txt("msg.objects.error.TriggerEventRequired"));
        addValidation(editorPanel, c -> validateTriggerBody());
        if (getObjectType() == DATASET_TRIGGER) {
            addSelectionValidation(targetDatasetComboBox,
                    txt("msg.objects.error.SelectObject", txt("app.object.label.TriggerTargetDataset")));
        }
    }

    private String validateTriggerBody() {
        String triggerBody = document == null ? "" : document.getText().trim();
        return isEmptyOrSpaces(triggerBody) ? txt("msg.objects.error.TriggerBodyRequired") : null;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        super.applyFormChanges();
        input.setAttributeValue(TRIGGER_TYPE, triggerTypeComboBox.getSelectedValue());
        input.setAttributeValue(TRIGGER_EVENTS, getSelectedTriggerEvents());
        input.setAttributeValue(TRIGGER_TARGET_DATASET,
                getSelectedTargetDataset() == null ? null : getSelectedTargetDataset().getName());
        input.setAttributeValue(TRIGGER_FOR_EACH_ROW, triggerForEachRowCheckBox.isSelected());
        input.setAttributeValue(OBJECT_DETAIL, document == null ? "" : document.getText().trim());
        input.setIdentifierCase(getSelectedIdentifierCase());
    }

    private DBTriggerEvent[] getSelectedTriggerEvents() {
        List<DBTriggerEvent> selectedEvents = new ArrayList<>();
        for (int i = 0; i < triggerEvents.size(); i++) {
            if (triggerEventButtons.get(i).isSelected()) {
                selectedEvents.add(triggerEvents.get(i));
            }
        }
        return selectedEvents.toArray(DBTriggerEvent[]::new);
    }

    private boolean hasSelectedTriggerEvent() {
        return triggerEventButtons.stream().anyMatch(JToggleButton::isSelected);
    }

    private boolean allowsMultipleTriggerEvents() {
        return getObjectTypeSpec().allowsMultiple(TRIGGER_EVENTS);
    }

    private boolean isRowTriggerMandatory() {
        List<Boolean> booleanOptions = getSupportedValues(TRIGGER_FOR_EACH_ROW);
        return booleanOptions.size() == 1 && booleanOptions.get(0);
    }

    private DBDataset getSelectedTargetDataset() {
        return getSelection(targetDatasetComboBox);
    }

    @Override
    protected void initStatePersistence() {
        StateAttributes state = ObjectFactoryManager.getInstance(ensureProject()).getState(getObjectType());
        initPersistence(preserveCaseCheckBox, state, "preserve-identifier-case");
        initTriggerTypePersistence(state);
        if (getObjectType() == DATASET_TRIGGER) {
            initPersistence(triggerForEachRowCheckBox, state, "for-each-row");
            if (isRowTriggerMandatory()) {
                triggerForEachRowCheckBox.setSelected(true);
                triggerForEachRowCheckBox.setEnabled(false);
            }
        }
        for (int i = 0; i < triggerEvents.size(); i++) {
            DBTriggerEvent event = triggerEvents.get(i);
            initPersistence(triggerEventButtons.get(i), state, "trigger-event-" + event.getName());
        }
    }

    private void initTriggerTypePersistence(StateAttributes state) {
        String savedType = state.getAttribute("trigger-type");
        if (!isEmptyOrSpaces(savedType)) {
            for (int i = 0; i < triggerTypeComboBox.getItemCount(); i++) {
                DBTriggerType type = triggerTypeComboBox.getItemAt(i);
                if (type.getName().equalsIgnoreCase(savedType)) {
                    triggerTypeComboBox.setSelectedValue(type);
                    break;
                }
            }
        }
        onSelectionChange(triggerTypeComboBox, type -> {
            if (type != null) state.setAttribute("trigger-type", type.getName());
        });
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
        triggerTypeComboBox.setSelectedValue(TRIGGER_TYPE.of(input));
        triggerForEachRowCheckBox.setSelected(isRowTriggerMandatory() || TRIGGER_FOR_EACH_ROW.is(input));
        if (getObjectType() == DATASET_TRIGGER && isInitialized()) {
            targetDatasetComboBox
                    .withValuePreselector(() -> TRIGGER_TARGET_DATASET.of(input))
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
