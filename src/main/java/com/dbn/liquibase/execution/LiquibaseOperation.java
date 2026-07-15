package com.dbn.liquibase.execution;

import com.dbn.common.constant.Constant;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

/** Liquibase operation represented in the DBN execution console. */
@Getter
public enum LiquibaseOperation implements Constant<LiquibaseOperation> {
    GENERATE_CHANGELOG,
    VALIDATE_CHANGELOG,
    COMPARE_SCHEMAS,
    GENERATE_DIFF_CHANGELOG,
    SHOW_CHANGELOG_STATUS,
    SYNCHRONIZE_CHANGELOG,
    SYNCHRONIZE_CHANGELOG_SQL,
    UPDATE_DATABASE,
    UPDATE_SQL,
    TAG_DATABASE,
    ROLLBACK_CHANGESETS,
    ROLLBACK_SQL;

    private final LiquibaseOperationSupport support = new LiquibaseOperationSupport(this);

    public String getName() {
        return txt("cfg.liquibase.const.Operation_" + name());
    }

    public String getDescription() {
        return txt("cfg.liquibase.text.OperationDescription_" + name());
    }

    public String getHint() {
        return /*txt("cfg.liquibase.title.Operation_" + name()) + "\n\n" +*/ txt("cfg.liquibase.hint.Operation_" + name());
    }
}
