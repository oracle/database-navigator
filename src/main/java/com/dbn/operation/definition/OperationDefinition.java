package com.dbn.operation.definition;

import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.validation.ValidationException;
import com.dbn.operation.model.OperationInput;
import com.dbn.operation.model.OperationOutput;
import com.dbn.operation.model.OperationSession;

import java.util.List;

public interface OperationDefinition  {
    OperationType getType();
    OperationInput createInput();
    OperationOutput createOutput();
    DBNForm createInputUi(OperationInput input, OperationSession chain);
    DBNForm createOutputUi(OperationOutput output, OperationSession chain) throws Exception;
    List<ValidationException> validateInput(OperationInput input);
    OperationOutput execute(OperationInput input, OperationSession chain) throws Exception;

}
