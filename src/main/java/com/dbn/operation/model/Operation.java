package com.dbn.operation.model;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.operation.definition.OperationRegistry;
import com.dbn.operation.definition.OperationType;
import com.dbn.operation.definition.OperationDefinition;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.*;

@Getter
@Setter
public final class Operation implements PersistentStateElement {
    private String typeId;
    private OperationInput input;
    private OperationOutput output;


    public OperationType getType(){
        return OperationRegistry.getOperationType(typeId);
    }

    public OperationDefinition getDefinition(){
        return OperationRegistry.getOperationDefinition(typeId);
    }

    @Override
    public void readState(Element element) {
        typeId = stringAttribute(element, "type-id");
        Element inputElement = element.getChild("input");
        Element outputElement = element.getChild("output");

        input = getDefinition().createInput();
        output = getDefinition().createOutput();

        input.readState(inputElement);
        output.readState(outputElement);

    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "type-id", typeId);
        Element inputElement = newElement(element, "input");
        Element outputElement = newElement(element, "output");
        input.writeState(inputElement);
        output.writeState(outputElement);
    }

    // operationform. operationchainform , toolwindow -->stack of operationChainForms(card layouts,
}
