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
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

/** Non-modal dialog displaying the grouped Liquibase operations for a project. */
public class LiquibaseOperationDashboardDialog extends DBNDialog<LiquibaseOperationDashboardForm> {
    private final DBObjectRef<DBSchema> initialSchema;

    public LiquibaseOperationDashboardDialog(@NotNull Project project) {
        this(project, null);
    }

    public LiquibaseOperationDashboardDialog(@NotNull DBSchema schema) {
        this(schema.getProject(), schema);
    }

    private LiquibaseOperationDashboardDialog(@NotNull Project project, @Nullable DBSchema initialSchema) {
        super(project, txt("msg.liquibase.title.OperationDashboard"), false);
        this.initialSchema = DBObjectRef.of(initialSchema);
        setDefaultSize(640, 860);
        setModal(false);
        init();
    }

    @Nullable
    public DBSchema getInitialSchema() {
        return DBObjectRef.get(initialSchema);
    }

    @NotNull
    @Override
    protected LiquibaseOperationDashboardForm createForm() {
        return new LiquibaseOperationDashboardForm(this);
    }

    @Override
    @NotNull
    protected Action[] initializeActions() {
        renameAction(getCancelAction(), txt("msg.shared.button.Close"));
        return actions(getCancelAction());
    }
}
