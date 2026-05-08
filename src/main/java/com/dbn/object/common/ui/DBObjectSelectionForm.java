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

package com.dbn.object.common.ui;

import com.dbn.common.filter.Filter;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.common.util.Strings.startsWithVowel;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static com.dbn.object.type.DBObjectType.TABLE;

public class DBObjectSelectionForm<T extends DBObject> extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel schemaLabel;
    private JLabel objectLabel;

    private DBObjectSelector<DBSchema> schemaComboBox;
    private DBObjectSelector<T> objectComboBox;
    private JPanel headerPanel;
    private JPanel hintPanel;

    private final DBObjectSelectionInput<T> input;

    public DBObjectSelectionForm(@NotNull Disposable parent, @NotNull DBObjectSelectionInput<T> input) {

        super(parent);
        this.input = input;

        initHeaderPanel();
        initHintPanel();
        initObjectLabel();

        resetFormChanges();
    }

    private void initHeaderPanel() {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, getConnection());
        headerPanel.add(headerForm.getComponent());
    }

    private void initHintPanel() {
        TextContent hint = input.getHint();
        if (hint == null) {
            hintPanel.setVisible(false);
        } else {
            DBNHintForm hintForm = new DBNHintForm(this, hint, null, true);
            hintPanel.add(hintForm.getComponent());
        }
    }

    private void initObjectLabel() {
        String objectTypeName = input.getObjectType().getTitleCasedName();
        objectLabel.setText(objectTypeName);
    }

    public ConnectionHandler getConnection() {
        return input.getConnection();
    }

    public ConnectionId getConnectionId() {
        return getConnection().getConnectionId();
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedSchema()), array(objectComboBox));
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(schemaLabel, schemaComboBox);
        alignerData.registerFieldGroup(objectLabel, objectComboBox);
    }

    private void initComboBoxes() {
        schemaComboBox
                .initialize(this, SCHEMA)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadSchemas())
                .withValuePreselector(input.getSchemaPreselector())
                .triggerLoad();

        objectComboBox
                .initialize(this, TABLE)
                .withConnectionContext(() -> getConnection())
                .withSchemaContext(() -> getSelectedSchema())
                .withValueLoader(() -> loadObjects())
                .withValuePreselector(input.getObjectPreselector())
                .triggerLoad();

        updateFieldAvailability();
    }

    protected List<DBSchema> loadSchemas() {
        DBObjectBundle objectBundle = getConnection().getObjectBundle();

        List<DBSchema> schemas = objectBundle.getSchemas();
        Filter<DBSchema> schemaFilter = input.getSchemaFilter();
        if (schemaFilter == null) return schemas;

        return filter(schemas, schemaFilter);
    }

    private List<T> loadObjects() {
        DBSchema schema = getSelectedSchema();
        if (schema == null) return Collections.emptyList();

        DBObjectType objectType = input.getObjectType();
        List<T> objects = schema.getChildObjects(objectType);

        Filter<T> objectFilter = input.getObjectFilter();
        if (objectFilter == null) return objects;

        return filter(objects, objectFilter);
    }

    protected void initEventListeners() {
        onSelectionChange(schemaComboBox, v -> populateTables());
        onSelectionChange(objectComboBox, v -> populateColumns());
    }

    @Override
    protected void initValidation() {
        addSelectionValidation(schemaComboBox, "Please select a schema");
        DBObjectType objectType = input.getObjectType();
        String objectTypeName = objectType.getTitleCasedName();

        addSelectionValidation(objectComboBox, "Please select " + (startsWithVowel(objectTypeName) ? "an " : "a ") +  objectTypeName);
    }

    private void populateColumns() {
        updateFieldAvailability();
    }

    private void populateTables() {
        updateFieldAvailability();
        objectComboBox.reloadValues();
    }

    @Override
    public void resetFormChanges() {
        initComboBoxes();
    }

    @Override
    public void applyFormChanges() {

    }

    @Nullable
    public DBSchema getSelectedSchema() {
        return ComboBoxes.getSelection(schemaComboBox);
    }

    @Nullable
    public T getSelectedObject() {
        return ComboBoxes.getSelection(objectComboBox);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
