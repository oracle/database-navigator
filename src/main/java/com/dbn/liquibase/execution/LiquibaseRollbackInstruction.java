package com.dbn.liquibase.execution;

import org.jetbrains.annotations.NotNull;

/** Describes the Liquibase rollback command and its identifying parameter. */
public record LiquibaseRollbackInstruction(
        @NotNull String command,
        @NotNull String parameter,
        @NotNull Object value) {
}
