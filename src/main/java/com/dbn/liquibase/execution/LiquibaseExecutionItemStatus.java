package com.dbn.liquibase.execution;

import static com.dbn.nls.NlsResources.txt;

/** Status of a database object processed by a Liquibase operation. */
public enum LiquibaseExecutionItemStatus {
    PROCESSING,
    DISCOVERED;

    public String getName() {
        return txt("cfg.liquibase.const.ExecutionItemStatus_" + name());
    }
}
