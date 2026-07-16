package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBDataset;
import com.dbn.object.DBDatasetTrigger;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.DATASET_TRIGGER;

public class DBDatasetTriggerSourceCodeAdapter extends DBMetadataSourceCodeAdapter<DBDatasetTrigger> {
    public DBDatasetTriggerSourceCodeAdapter() { super(DATASET_TRIGGER); }

    @Override
    public ResultSet loadSourceCode(DBDatasetTrigger trigger, DBContentType contentType, DBNConnection connection) throws SQLException {
        DBDataset dataset = trigger.getDataset();

        DatabaseMetadataInterface metadataInterface = trigger.getMetadataInterface();
        return metadataInterface.loadDatasetTriggerSourceCode(
                dataset.getSchemaName(),
                dataset.getName(),
                trigger.getSchemaName(),
                trigger.getName(),
                connection);
    }

    @Override
    public void saveSourceCode(DBDatasetTrigger trigger, DBContentType contentType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        DBDataset dataset = trigger.getDataset();

        DatabaseDataDefinitionInterface dataDefinitionInterface = trigger.getDataDefinitionInterface();
        dataDefinitionInterface.updateTrigger(
                dataset.getSchemaName(true),
                dataset.getName(true),
                trigger.getName(),
                oldCode,
                newCode,
                connection);
    }
}
