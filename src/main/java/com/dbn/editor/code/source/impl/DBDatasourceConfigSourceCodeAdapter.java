package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseDatasourceConfigInterface;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBDatasourceConfig;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.DATASOURCE_CONFIG;

public class DBDatasourceConfigSourceCodeAdapter extends DBMetadataSourceCodeAdapter<DBDatasourceConfig> {
    public DBDatasourceConfigSourceCodeAdapter() {
        super(DATASOURCE_CONFIG);
    }

    @Override
    public ResultSet loadSourceCode(DBDatasourceConfig config, DBContentType contentType, DBNConnection connection) throws SQLException {
        DatabaseMetadataInterface metadataInterface = config.getMetadataInterface();
        return metadataInterface.loadDatasourceConfigSourceCode(
                config.getSchemaName(),
                config.getName(),
                connection);
    }

    @Override
    public void saveSourceCode(DBDatasourceConfig config, DBContentType contentType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        DatabaseDatasourceConfigInterface datasourceConfigInterface = config.getDatasourceConfigInterface();
        datasourceConfigInterface.updateDatasourceConfig(
                config.getSchemaName(),
                config.getName(),
                newCode,
                connection);

        config.updateValue(newCode);
    }
}
