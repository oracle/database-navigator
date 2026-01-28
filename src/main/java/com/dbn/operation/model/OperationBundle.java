package com.dbn.operation.model;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.connection.ConnectionId;
import org.jdom.Element;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.options.setting.Settings.*;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;

public class OperationBundle implements PersistentStateElement {
    private final Map<String , OperationSession> operationChains = new ConcurrentHashMap<>();
    private ConnectionId connectionId;
    public void addOperationChain(OperationSession operationSession){
        operationChains.put(operationSession.getSessionId(), operationSession);
    }
    public OperationSession getOperationChain(String sessionId){
        return operationChains.get(sessionId);
    }


    @Override
    public void readState(Element element) {
        connectionId = connectionIdAttribute(element, "connection-id");
        Element chainsElement = element.getChild("chains");
        List <Element> chainElements = chainsElement.getChildren("chain");
        for (Element chainElement : chainElements) {
            OperationSession chain = new OperationSession();
            chain.readState(chainElement);
        }
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "connection-id", connectionId.id());
        Element chainsElement = newElement(element, "chains");
        for (OperationSession chain : operationChains.values()) {
            Element chainElement = newElement(chainsElement, "chain");
            chain.writeState(chainElement);
        }
    }
}
