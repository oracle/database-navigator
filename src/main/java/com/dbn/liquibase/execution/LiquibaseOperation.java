package com.dbn.liquibase.execution;

/** Liquibase operation represented in the DBN execution console. */
public enum LiquibaseOperation {
    VALIDATE,
    STATUS,
    UPDATE,
    ROLLBACK
}
