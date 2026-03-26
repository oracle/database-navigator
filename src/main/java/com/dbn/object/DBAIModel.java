package com.dbn.object;

import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBAIModelAlgorithm;
import com.dbn.object.type.DBAIModelMiningFunction;

public interface DBAIModel extends DBSchemaObject {
    String getName();

    DBAIModelMiningFunction getMiningFunction();

    DBAIModelAlgorithm getAlgorithm();

    String getAlgorithmType();

    boolean isInmemory();

    boolean isPartitioned();

    boolean isExternalData();

    long getModelSize();
}
