package com.dbn.liquibase.execution;

/** Liquibase operation represented in the DBN execution console. */
public enum LiquibaseOperation {
    GENERATE_INITIAL_CHANGELOG,
    VALIDATE,
    STATUS,
    UPDATE,
    ROLLBACK
}
