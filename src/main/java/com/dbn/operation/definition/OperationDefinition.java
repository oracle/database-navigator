package com.dbn.operation.definition;

import com.dbn.common.ui.form.DBNForm;
import com.dbn.operation.model.OperationSession;
import com.dbn.operation.model.OperationInput;
import com.dbn.operation.model.OperationOutput;
import com.intellij.internal.statistic.eventLog.validator.rules.ValidationError;

import java.util.List;

public interface OperationDefinition  {
    OperationType getType();
    OperationInput createInput();
    OperationOutput createOutput();
    DBNForm createInputUi(OperationInput input, OperationSession chain);
    DBNForm createOutputUi(OperationOutput output, OperationSession chain) throws Exception;
    List<ValidationError> validateInput(OperationInput input);
    OperationOutput execute(OperationInput input, OperationSession chain) throws Exception;

}
