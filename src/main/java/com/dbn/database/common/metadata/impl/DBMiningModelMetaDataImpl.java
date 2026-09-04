package com.dbn.database.common.metadata.impl;

import com.dbn.database.common.metadata.DBObjectMetadataBase;
import com.dbn.database.common.metadata.def.DBMiningModelMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public  class DBMiningModelMetaDataImpl extends DBObjectMetadataBase implements DBMiningModelMetadata {

    public DBMiningModelMetaDataImpl(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public String getModelName() throws SQLException {
        return getString("MODEL_NAME");
    }

    @Override
    public String getMiningFunction() throws SQLException {
        return getString("MINING_FUNCTION");
    }

    @Override
    public String getAlgorithm() throws SQLException {
        return getString("ALGORITHM");
    }

    public String getAlgorithmType() throws SQLException {
        return getString("ALGORITHM_TYPE");
    }

    @Override
    public boolean isExternalData() throws SQLException {
        return isYesFlag("EXTERNAL_DATA");
    }

    @Override
    public boolean isPartitioned() throws SQLException {
        return isYesFlag("PARTITIONED");
    }

    @Override
    public boolean isInmemory() throws SQLException {
        return isYesFlag("INMEMORY");
    }

    @Override
    public long getModelSize() throws SQLException {
        return resultSet.getLong("MODEL_SIZE");
    }
}
