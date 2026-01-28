package com.dbn.database.common.metadata.impl;

import com.dbn.database.common.metadata.DBObjectMetadataBase;
import com.dbn.database.common.metadata.def.DBModelMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public  class DBAIModelMetaDataImpl extends DBObjectMetadataBase implements DBModelMetadata {

    public DBAIModelMetaDataImpl(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public String getModelName() throws SQLException {
        return getString("MODEL_NAME");
    }
}
