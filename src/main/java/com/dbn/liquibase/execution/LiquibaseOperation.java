package com.dbn.liquibase.execution;

import com.dbn.common.constant.Constant;
import com.dbn.common.ui.form.field.FieldState;

import static com.dbn.nls.NlsResources.txt;

/** Liquibase operation represented in the DBN execution console. */
public enum LiquibaseOperation implements Constant<LiquibaseOperation> {
    INITIALIZE,
    VALIDATE,
    COMPARE,
    STATUS,
    UPDATE,
    ROLLBACK;

    public String getName() {
        return txt("cfg.liquibase.const.Operation_" + name());
    }

    public String getDescription() {
        return txt("cfg.liquibase.text.OperationDescription_" + name());
    }

    public String getHint() {
        return /*txt("cfg.liquibase.title.Operation_" + name()) + "\n\n" +*/ txt("cfg.liquibase.hint.Operation_" + name());
    }

    public FieldState getSourceContextState() {
        if (isOneOf(INITIALIZE, COMPARE)) return FieldState.VISIBLE;
        return FieldState.HIDDEN;
    }

    public FieldState getTargetContextState() {
        if (this == COMPARE) return FieldState.EDITABLE;
        if (isOneOf(VALIDATE, STATUS, UPDATE, ROLLBACK)) return FieldState.VISIBLE;
        return FieldState.HIDDEN;
    }

    public boolean requiresSourceSchema() {
        return getSourceContextState().isVisible();
    }

    public boolean requiresTargetSchema() {
        return getTargetContextState().isVisible();
    }

    public boolean supportsSnapshotItems() {
        return this == INITIALIZE;
    }

    public boolean supportsChangeSetItems() {
        return this == UPDATE;
    }
}
