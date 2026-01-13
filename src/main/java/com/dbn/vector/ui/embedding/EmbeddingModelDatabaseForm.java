package com.dbn.vector.ui.embedding;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBAIModel;
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
import static com.dbn.object.type.DBObjectType.AI_MODEL;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static java.util.Collections.emptyList;

public class EmbeddingModelDatabaseForm extends VectorToolboxFormBase {
    private JPanel mainPanel;
    private JLabel schemaLabel;
    private JLabel modelLabel;
    private DBObjectSelector<DBAIModel> modelComboBox;
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
        addSelectionValidation(schemaComboBox, "Please select a schema");
        addSelectionValidation(modelComboBox, "Please select or create a model");
    }

    private List<DBAIModel> loadModels() {
        DBSchema schema = getSelectedSchema();
        if (schema == null) return emptyList();

        return schema.getAIModels();
    }

    @Nullable
    public DBSchema getSelectedSchema() {
        return getSelection(schemaComboBox);
    }

    @Nullable
    public DBAIModel getSelectedModel() {
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

        modelComboBox.initialize(this, AI_MODEL)
                .withConnectionContext(() -> getConnection())
                .withSchemaContext(() -> getSelectedSchema())
                .withValueLoader(() -> loadModels())
                .withValuePreselector(() -> config.getModelName())
                .withObjectFactory("New AI Model...")
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
