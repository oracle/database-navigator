package com.dbn.liquibase.execution;

import static com.dbn.nls.NlsResources.txt;

/** Liquibase operation represented in the DBN execution console. */
public enum LiquibaseOperation {
    INITIALIZE,
    VALIDATE,
    STATUS,
    UPDATE,
    ROLLBACK;

    public String getName() {
        return txt("cfg.liquibase.const.Operation_" + name());
    }

    public String getDescription() {
        return txt("cfg.liquibase.text.OperationDescription_" + name());
    }
}
