/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.migration.shared.operation.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.migration.shared.operation.DatabaseMigrationOperation;
import com.dbn.migration.shared.operation.DatabaseMigrationOperationCategory;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Shared project-level dialog for grouped database migration operations.
 *
 * <p>The optional schema is only an initial context for the engine-specific input
 * dialog. The dashboard itself remains independent of a database connection.</p>
 */
public abstract class DatabaseMigrationOperationDashboardDialog<
        O extends DatabaseMigrationOperation,
        C extends DatabaseMigrationOperationCategory>
        extends DBNDialog<DatabaseMigrationOperationDashboardForm<O, C>> {
    private final DBObjectRef<DBSchema> initialSchema;

    protected DatabaseMigrationOperationDashboardDialog(
            @NotNull Project project,
            @Nullable DBSchema initialSchema,
            @NotNull String title,
            int height) {
        super(project, title, false);
        this.initialSchema = DBObjectRef.of(initialSchema);
        setDefaultSize(640, height);
        setModal(false);
        init();
    }

    @NotNull
    public abstract List<O> getOperations();

    @NotNull
    public abstract List<C> getCategories();

    @NotNull
    public abstract String getDashboardHint();

    @NotNull
    public abstract String getDocumentationLabel();

    @NotNull
    public abstract String getDocumentationUrl();

    public abstract void startOperation(@NotNull O operation);

    @Nullable
    public DBSchema getInitialSchema() {
        return DBObjectRef.get(initialSchema);
    }

    @NotNull
    @Override
    protected DatabaseMigrationOperationDashboardForm<O, C> createForm() {
        return new DatabaseMigrationOperationDashboardForm<>(this);
    }

    @Override
    @NotNull
    protected Action[] initializeActions() {
        renameAction(getCancelAction(), txt("msg.shared.button.Close"));
        return actions(getCancelAction());
    }
}
