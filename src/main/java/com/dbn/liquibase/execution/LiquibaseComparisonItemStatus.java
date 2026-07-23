package com.dbn.liquibase.execution;

import static com.dbn.nls.NlsResources.txt;

/** Comparison outcome assigned to an object by a Liquibase schema diff. */
public enum LiquibaseComparisonItemStatus {
    MISSING,
    UNEXPECTED,
    CHANGED;

    public String getName() {
        return txt("app.liquibase.const.ComparisonItemStatus_" + name());
    }
}
