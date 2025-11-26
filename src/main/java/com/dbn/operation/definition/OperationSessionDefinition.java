package com.dbn.operation.definition;

import com.dbn.operation.model.OperationSession;

import java.util.List;

public interface OperationSessionDefinition {
    OperationSessionType getType();
    List<OperationDefinition> getInitialOperations();
    List<OperationDefinition> getNextOperations(OperationSession chain);
}
