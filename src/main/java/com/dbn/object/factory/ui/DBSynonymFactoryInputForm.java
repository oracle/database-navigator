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
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseIdentifierCase;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.ui.DBObjectTypeSelector;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.object.factory.ObjectFactoryManager;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.factory.model.DBObjectAttributeType.SYNONYM_TARGET_OBJECT_NAME;
import static com.dbn.object.factory.model.DBObjectAttributeType.SYNONYM_TARGET_OBJECT_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SYNONYM_TARGET_SCHEMA;
import static com.dbn.object.type.DBObjectType.SCHEMA;

public class DBSynonymFactoryInputForm extends DBSchemaObjectFactoryInputForm {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;
    private JTextField nameTextField;
    private JLabel targetSchemaLabel;
    private JLabel targetObjectTypeLabel;
    private JLabel targetObjectLabel;
    private DBObjectSelector<DBSchema> targetSchemaComboBox;
    private DBObjectTypeSelector targetObjectTypeComboBox;
    private DBObjectSelector<DBObject> targetObjectComboBox;
    private JCheckBox preserveCaseCheckBox;
    private DBNInfoLabel preserveCaseInfoLabel;

    private boolean targetObjectSelectorInitialized;

    public DBSynonymFactoryInputForm(@NotNull DBNComponent parent, DBObjectSpec input) {
        super(parent, input);

        initContextComponents();
        initHeaderForm();
        initPreserveCaseFields();
        initComboBoxes();
        resetFormChanges();
    }

    private void initPreserveCaseFields() {
        preserveCaseInfoLabel.setContent(getPreserveCaseInfoText());
    }

    private void initComboBoxes() {
        targetSchemaComboBox
                .initialize(this, SCHEMA)
                .withConnectionContext(this::getConnection)
                .withValueLoader(this::loadSchemas)
                .withValuePreselector(() -> SYNONYM_TARGET_SCHEMA.of(input))
                .withValueLoadConsumer(values -> populateTargetObjects())
                .triggerLoad();

        targetObjectTypeComboBox
                .withValueLoader(this::loadTargetObjectTypes)
                .withValuePreselector(type -> type == SYNONYM_TARGET_OBJECT_TYPE.of(input))
                .withValueLoadConsumer(values -> populateTargetObjects())
                .triggerLoad();
    }

    private void initTargetObjectSelector() {
        DBObjectType objectType = getSelectedTargetObjectType();
        if (objectType == null) return;

        if (!targetObjectSelectorInitialized) {
            targetObjectComboBox
                    .initialize(this, objectType)
                    .withConnectionContext(this::getConnection)
                    .withSchemaContext(this::getSelectedTargetSchema)
                    .withValueLoader(this::loadTargetObjects)
                    .withValuePreselector(() -> SYNONYM_TARGET_OBJECT_NAME.of(input));
            targetObjectSelectorInitialized = true;
        }

        targetObjectComboBox.triggerLoad();
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedTargetSchema()), array(targetObjectTypeComboBox));
        fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedTargetSchema()) && isValid(getSelectedTargetObjectType()),
                array(targetObjectComboBox));
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(targetSchemaLabel, targetSchemaComboBox);
        alignerData.registerFieldGroup(targetObjectTypeLabel, targetObjectTypeComboBox);
        alignerData.registerFieldGroup(targetObjectLabel, targetObjectComboBox);
    }

    @Override
    protected void initEventListeners() {
        onSelectionChange(targetSchemaComboBox, e -> populateTargetObjects());
        onSelectionChange(targetObjectTypeComboBox, e -> populateTargetObjects());
    }

    private void populateTargetObjects() {
        updateFieldAvailability();
        if (!targetObjectSelectorInitialized) {
            initTargetObjectSelector();
        } else {
            targetObjectComboBox.clearValues();
            targetObjectComboBox.reloadValues();
        }
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField,
                n -> isNotEmptyOrSpaces(n.trim()),
                txt("msg.objects.error.ObjectNameRequired", getObjectType().getDisplayName()));
        addTextValidation(nameTextField,
                n -> isEmptyOrSpaces(n) || isWord(n.trim()),
                txt("msg.objects.error.ValidObjectNameRequired", getObjectType().getDisplayName()));
        addSelectionValidation(targetSchemaComboBox, txt("msg.shared.error.SelectTargetSchema"));
        addSelectionValidation(targetObjectTypeComboBox,
                txt("msg.objects.error.SelectObject", txt("app.object.label.TargetObjectType")));
        addSelectionValidation(targetObjectComboBox,
                txt("msg.objects.error.SelectObject", txt("app.object.label.TargetObject")));
    }

    private List<DBSchema> loadSchemas() {
        DBObjectBundle objectBundle = getConnection().getObjectBundle();
        return objectBundle.getSchemas();
    }

    private List<DBObjectType> loadTargetObjectTypes() {
        return DBObjectType.BROWSABLE_TYPES.stream()
                .filter(DBObjectType::isSchemaObject)
                .filter(type -> type.isSupported(getConnection()))
                .toList();
    }

    private List<DBObject> loadTargetObjects() {
        DBSchema schema = getSelectedTargetSchema();
        DBObjectType objectType = getSelectedTargetObjectType();
        if (schema == null || objectType == null) return List.of();

        return schema.getChildObjects(objectType);
    }

    private DBSchema getSelectedTargetSchema() {
        return getSelection(targetSchemaComboBox);
    }

    private DBObjectType getSelectedTargetObjectType() {
        return targetObjectTypeComboBox.getSelectedValue();
    }

    private DBObject getSelectedTargetObject() {
        return targetObjectComboBox.getSelectedValue();
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        super.applyFormChanges();
        DBSchema targetSchema = getSelectedTargetSchema();
        DBObject targetObject = getSelectedTargetObject();
        input.setAttributeValue(SYNONYM_TARGET_SCHEMA, targetSchema == null ? null : targetSchema.getName());
        input.setAttributeValue(SYNONYM_TARGET_OBJECT_TYPE, getSelectedTargetObjectType());
        input.setAttributeValue(SYNONYM_TARGET_OBJECT_NAME, targetObject == null ? null : targetObject.getName());
        input.setIdentifierCase(getSelectedIdentifierCase());
    }

    @Override
    protected void initStatePersistence() {
        StateAttributes state = ObjectFactoryManager.getInstance(ensureProject()).getState(getObjectType());
        initPersistence(preserveCaseCheckBox, state, "preserve-identifier-case");
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
        if (!isInitialized()) return;

        targetSchemaComboBox
                .withValuePreselector(() -> SYNONYM_TARGET_SCHEMA.of(input))
                .reloadValues();
        targetObjectTypeComboBox
                .withValuePreselector(type -> type == SYNONYM_TARGET_OBJECT_TYPE.of(input))
                .reloadValues();
        if (targetObjectSelectorInitialized) {
            targetObjectComboBox
                    .withValuePreselector(() -> SYNONYM_TARGET_OBJECT_NAME.of(input))
                    .reloadValues();
        }
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
