package com.dbn.database.common.metadata.def;

import com.dbn.database.common.metadata.DBObjectMetadata;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.SQLException;

public interface DBModelMetadata extends DBObjectMetadata {
    String getModelName() throws SQLException;


    @Data
    @AllArgsConstructor
    class Record implements DBObjectMetadata {
        private final String modelName;
    }
}
