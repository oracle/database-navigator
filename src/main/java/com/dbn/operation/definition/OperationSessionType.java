package com.dbn.operation.definition;

import lombok.Getter;

@Getter
public class OperationSessionType {
    private final String id;
    private final String name;
    private final String description;


    public OperationSessionType(String id, String name, String description, String action) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}
