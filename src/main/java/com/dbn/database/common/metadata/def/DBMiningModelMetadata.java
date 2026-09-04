package com.dbn.database.common.metadata.def;

import com.dbn.database.common.metadata.DBObjectMetadata;
import com.dbn.database.common.security.ObjectIdentifier;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.SQLException;

public interface DBMiningModelMetadata extends DBObjectMetadata {
    @ObjectIdentifier
    String getModelName() throws SQLException;

    String getMiningFunction() throws SQLException;

    String getAlgorithm() throws SQLException;

    String getAlgorithmType() throws SQLException;

    boolean isExternalData() throws SQLException;

    boolean isPartitioned() throws SQLException;

    boolean isInmemory() throws SQLException;

    long getModelSize() throws SQLException;


    @Data
    @AllArgsConstructor
    class Record implements DBObjectMetadata {
        private final String modelName;
    }
}
