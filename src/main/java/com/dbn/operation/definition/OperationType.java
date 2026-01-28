package com.dbn.operation.definition;

import lombok.Getter;

@Getter
public class OperationType {
    private final String id;
    private final String name;
    private final String description;
    private final String action;


    public OperationType(String id, String name, String description, String action) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.action = action;
    }
}
