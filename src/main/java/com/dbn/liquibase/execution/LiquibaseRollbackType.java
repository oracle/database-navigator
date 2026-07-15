package com.dbn.liquibase.execution;

import com.dbn.common.constant.Constant;
import com.dbn.common.ui.Presentable;

import static com.dbn.nls.NlsResources.txt;

/** Selects the Liquibase criterion used to identify the changesets to roll back. */
public enum LiquibaseRollbackType implements Constant<LiquibaseRollbackType>, Presentable {
    COUNT,
    TAG,
    DATE;

    public String getName() {
        return txt("cfg.liquibase.const.RollbackType_" + name());
    }
}
