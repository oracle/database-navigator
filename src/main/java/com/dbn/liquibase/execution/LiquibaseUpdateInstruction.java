/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.execution;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import static com.dbn.liquibase.execution.LiquibaseCommands.NULL_ARGUMENT;
import static com.dbn.liquibase.execution.LiquibaseCommands.UPDATE_COUNT;
import static com.dbn.liquibase.execution.LiquibaseCommands.UPDATE_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseCommands.UPDATE_TO_TAG;

/** User-selected criterion and value for a Liquibase update operation. */
@Getter
@Setter
public class LiquibaseUpdateInstruction {
    private LiquibaseUpdateType type = LiquibaseUpdateType.ALL;
    private int count = 1;
    private String tag;

    @NotNull
    public String getCommand() {
        return switch (type) {
            case ALL -> UPDATE_DATABASE;
            case COUNT -> UPDATE_COUNT;
            case TAG -> UPDATE_TO_TAG;
        };
    }

    @NotNull
    public String getParameter() {
        return switch (type) {
            case ALL, COUNT -> "count";
            case TAG -> "tag";
        };
    }

    @NotNull
    public Object getValue() {
        return switch (type) {
            case ALL -> NULL_ARGUMENT;
            case COUNT -> count;
            case TAG -> tag;
        };
    }

    public void copyFrom(@NotNull LiquibaseUpdateInstruction instruction) {
        type = instruction.type;
        count = instruction.count;
        tag = instruction.tag;
    }
}
