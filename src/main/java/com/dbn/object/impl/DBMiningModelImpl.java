package com.dbn.object.impl;

import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBMiningModelMetadata;
import com.dbn.object.DBMiningModel;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.type.DBMiningModelAlgorithm;
import com.dbn.object.type.DBMiningModelFunction;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@Getter
public class DBMiningModelImpl extends DBSchemaObjectImpl<DBMiningModelMetadata> implements DBMiningModel {
    private DBMiningModelFunction miningFunction;
    private DBMiningModelAlgorithm algorithm;
    private String algorithmType;
    private boolean inmemory;
    private boolean partitioned;
    private boolean externalData;
    private long modelSize;

    public DBMiningModelImpl(@NotNull DBSchema schema, DBMiningModelMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBMiningModelMetadata metadata) throws SQLException {
        this.miningFunction = DBMiningModelFunction.get(metadata.getMiningFunction());
        this.algorithm = DBMiningModelAlgorithm.get(metadata.getAlgorithm());
        this.algorithmType = metadata.getAlgorithmType();
        this.inmemory = metadata.isInmemory();
        this.partitioned = metadata.isPartitioned();
        this.externalData = metadata.isExternalData();
        this.modelSize = metadata.getModelSize();

        return metadata.getModelName();
    }

    @Override
    public @NotNull DBObjectType getObjectType() {
        return DBObjectType.MINING_MODEL;
    }
}
