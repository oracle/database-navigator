package com.dbn.object.impl;

import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBAIModelMetadata;
import com.dbn.object.DBAIModel;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.type.DBAIModelAlgorithm;
import com.dbn.object.type.DBAIModelMiningFunction;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@Getter
public class DBAIModelImpl  extends DBSchemaObjectImpl<DBAIModelMetadata> implements DBAIModel {
    private DBAIModelMiningFunction miningFunction;
    private DBAIModelAlgorithm algorithm;
    private String algorithmType;
    private boolean inmemory;
    private boolean partitioned;
    private boolean externalData;
    private long modelSize;

    public DBAIModelImpl(@NotNull DBSchema schema, DBAIModelMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBAIModelMetadata metadata) throws SQLException {
        this.miningFunction = DBAIModelMiningFunction.get(metadata.getMiningFunction());
        this.algorithm = DBAIModelAlgorithm.get(metadata.getAlgorithm());
        this.algorithmType = metadata.getAlgorithmType();
        this.inmemory = metadata.isInmemory();
        this.partitioned = metadata.isPartitioned();
        this.externalData = metadata.isExternalData();
        this.modelSize = metadata.getModelSize();

        return metadata.getModelName();
    }

    @Override
    public @NotNull DBObjectType getObjectType() {
        return DBObjectType.AI_MODEL;
    }
}
