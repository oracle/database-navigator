package com.dbn.operation.model;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.UUIDs;
import com.dbn.connection.ConnectionId;
import lombok.Getter;
import org.jdom.Element;

import java.util.List;

import java.util.concurrent.CopyOnWriteArrayList;

import static com.dbn.common.options.setting.Settings.*;
import static com.dbn.common.options.setting.Settings.setStringAttribute;

@Getter

public final class OperationSession implements PersistentStateElement {
    private  String sessionId = UUIDs.compact();
    private  ConnectionId connectionId;
    private final List<Operation> operations = new CopyOnWriteArrayList<>();


    @Override
    public void readState(Element element) {
        sessionId = stringAttribute(element, "session-id");
        connectionId = connectionIdAttribute(element, "connection-id");
        Element operationsElement = element.getChild("operations");
        List <Element> operationElements = operationsElement.getChildren("operation");
        for (Element operationElement : operationElements) {
            Operation operation = new Operation();
            operation.readState(operationElement);
        }
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "session-id", sessionId);
        setStringAttribute(element, "connection-id", connectionId.id());
        Element operationsElement = newElement(element, "operations");
        for (Operation operation : operations) {
            Element operationElement = newElement(operationsElement, "operation");
            operation.writeState(operationElement);
        }
    }
}
