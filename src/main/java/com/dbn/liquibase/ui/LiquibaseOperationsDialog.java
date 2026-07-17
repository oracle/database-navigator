/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.object.DBSchema;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

/** Dialog displaying the grouped Liquibase operations for one database schema. */
public class LiquibaseOperationsDialog extends DBNDialog<LiquibaseOperationsForm> {
    @Getter
    private final DBSchema schema;

    public LiquibaseOperationsDialog(@NotNull DBSchema schema) {
        super(schema.getProject(), txt("msg.liquibase.title.Dashboard"), false);
        this.schema = schema;
        setDefaultSize(640, 640);
        setModal(false);
        init();
    }

    @NotNull
    @Override
    protected LiquibaseOperationsForm createForm() {
        return new LiquibaseOperationsForm(this);
    }

    @Override
    @NotNull
    protected Action[] initializeActions() {
        renameAction(getCancelAction(), txt("msg.shared.button.Close"));
        return actions(getCancelAction());
    }
}
