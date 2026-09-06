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

package com.dbn.vector.ui.request;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBMiningModel;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.vector.model.request.EmbeddingModelDatabaseSpec;
import com.dbn.vector.ui.VectorToolboxFormBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.MINING_MODEL;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static java.util.Collections.emptyList;

public class EmbeddingModelDatabaseForm extends VectorToolboxFormBase {
    private JPanel mainPanel;
    private JLabel schemaLabel;
    private JLabel modelLabel;
    private DBObjectSelector<DBMiningModel> modelComboBox;
    private DBObjectSelector<DBSchema> schemaComboBox;

    public EmbeddingModelDatabaseForm(@NotNull VectorToolboxFormBase parent) {
        super(parent);
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(schemaLabel, schemaComboBox);
        alignerData.registerFieldGroup(modelLabel, modelComboBox);
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedSchema()), array(modelComboBox));
    }

    @Override
    protected void initValidation() {
        addSelectionValidation(schemaComboBox, txt("msg.shared.error.SelectSchema"));
        addSelectionValidation(modelComboBox, txt("msg.vector.error.SelectOrCreateModel"));
    }

    private List<DBMiningModel> loadModels() {
        DBSchema schema = getSelectedSchema();
        if (schema == null) return emptyList();

        return schema.getMiningModels();
    }

    @Nullable
    public DBSchema getSelectedSchema() {
        return getSelection(schemaComboBox);
    }

    @Nullable
    public DBMiningModel getSelectedModel() {
        return getSelection(modelComboBox);
    }

    private void initComboBoxes() {
        EmbeddingModelDatabaseSpec config = getConfig();
        ConnectionHandler connection = getConnection();

        schemaComboBox
                .initialize(this, SCHEMA)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadSchemas())
                .withValuePreselector(() -> config.getSchemaName())
                .triggerLoad();

        modelComboBox.initialize(this, MINING_MODEL)
                .withConnectionContext(() -> getConnection())
                .withSchemaContext(() -> getSelectedSchema())
                .withValueLoader(() -> loadModels())
                .withValuePreselector(() -> config.getModelName())
                .withObjectFactory(txt("app.vector.action.NewMiningModel"))
                .triggerLoad();

        updateFieldAvailability();
    }

    @Override
    protected void initEventListeners() {
        onSelectionChange(schemaComboBox, s -> populateModels());
    }

    @Override
    public void resetFormChanges() {
        initComboBoxes();
    }

    @Override
    public void applyFormChanges() {
        EmbeddingModelDatabaseSpec config = getConfig();
        config.setSchemaName(getSelectedObjectName(schemaComboBox, config.getSchemaName()));
        config.setModelName(getSelectedObjectName(modelComboBox, config.getModelName()));
    }

    private void populateModels() {
        updateFieldAvailability();
        modelComboBox.reloadValues();
    }


    public EmbeddingModelDatabaseSpec getConfig() {
        return getEmbeddingRequest().getModelConfig().getDatabaseModelConfig();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
