package com.dbn.object;

import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBMiningModelAlgorithm;
import com.dbn.object.type.DBMiningModelFunction;

public interface DBMiningModel extends DBSchemaObject {
    String getName();

    DBMiningModelFunction getMiningFunction();

    DBMiningModelAlgorithm getAlgorithm();

    String getAlgorithmType();

    boolean isInmemory();

    boolean isPartitioned();

    boolean isExternalData();

    long getModelSize();
}
