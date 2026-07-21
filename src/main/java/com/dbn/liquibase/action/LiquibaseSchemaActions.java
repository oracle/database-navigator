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

package com.dbn.liquibase.action;

import com.dbn.common.action.DefaultActionGroup;
import com.dbn.common.icon.Icons;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.menu.action.LiquibaseWorkspacesOpenAction;
import com.dbn.object.DBSchema;
import org.jetbrains.annotations.NotNull;

import static com.dbn.liquibase.execution.LiquibaseOperation.CALCULATE_CHECKSUMS;
import static com.dbn.liquibase.execution.LiquibaseOperation.CLEAR_CHECKSUMS;
import static com.dbn.liquibase.execution.LiquibaseOperation.COMPARE_SCHEMAS;
import static com.dbn.liquibase.execution.LiquibaseOperation.DROP_ALL;
import static com.dbn.liquibase.execution.LiquibaseOperation.FUTURE_ROLLBACK;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_DIFF_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.LIST_LOCKS;
import static com.dbn.liquibase.execution.LiquibaseOperation.MARK_NEXT_CHANGESET_RAN;
import static com.dbn.liquibase.execution.LiquibaseOperation.RELEASE_LOCKS;
import static com.dbn.liquibase.execution.LiquibaseOperation.ROLLBACK_CHANGESETS;
import static com.dbn.liquibase.execution.LiquibaseOperation.ROLLBACK_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.SHOW_CHANGELOG_HISTORY;
import static com.dbn.liquibase.execution.LiquibaseOperation.SHOW_CHANGELOG_STATUS;
import static com.dbn.liquibase.execution.LiquibaseOperation.SNAPSHOT_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.SYNCHRONIZE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.SYNCHRONIZE_CHANGELOG_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.TAG_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.UNEXPECTED_CHANGESETS;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.VALIDATE_CHANGELOG;
import static com.dbn.nls.NlsResources.txt;

/** Liquibase operations available for a single database schema. */
public class LiquibaseSchemaActions extends DefaultActionGroup {
    public LiquibaseSchemaActions(@NotNull DBSchema schema) {
        super(txt("app.liquibase.action.Liquibase"), true);
        getTemplatePresentation().setIcon(Icons.DB_LIQUIBASE);
        add(new LiquibaseDashboardAction(schema));
        add(new LiquibaseWorkspacesOpenAction(txt("app.liquibase.action.Workspaces"), null));
        addSeparator();

        DefaultActionGroup changelogActions = new DefaultActionGroup(txt("app.liquibase.group.Changelog"), true);
        changelogActions.add(action(schema, GENERATE_CHANGELOG));
        changelogActions.add(action(schema, GENERATE_DIFF_CHANGELOG));
        changelogActions.add(action(schema, VALIDATE_CHANGELOG));
        changelogActions.add(action(schema, COMPARE_SCHEMAS));
        add(changelogActions);

        DefaultActionGroup deployActions = new DefaultActionGroup(txt("app.liquibase.group.Deploy"), true);
        deployActions.add(action(schema, UPDATE_DATABASE));
        deployActions.add(action(schema, ROLLBACK_CHANGESETS));
        deployActions.add(action(schema, TAG_DATABASE));
        deployActions.add(action(schema, MARK_NEXT_CHANGESET_RAN));
        add(deployActions);

        DefaultActionGroup inspectActions = new DefaultActionGroup(txt("app.liquibase.group.Inspect"), true);
        inspectActions.add(action(schema, SHOW_CHANGELOG_STATUS));
        inspectActions.add(action(schema, SHOW_CHANGELOG_HISTORY));
        inspectActions.add(action(schema, UNEXPECTED_CHANGESETS));
        inspectActions.add(action(schema, SNAPSHOT_DATABASE));
        add(inspectActions);

        DefaultActionGroup maintenanceActions = new DefaultActionGroup(txt("app.liquibase.group.Maintenance"), true);
        maintenanceActions.add(action(schema, SYNCHRONIZE_CHANGELOG));
        maintenanceActions.add(action(schema, RELEASE_LOCKS));
        maintenanceActions.add(action(schema, LIST_LOCKS));
        maintenanceActions.add(action(schema, CLEAR_CHECKSUMS));
        maintenanceActions.add(action(schema, CALCULATE_CHECKSUMS));
        add(maintenanceActions);

        DefaultActionGroup sqlPreviewActions = new DefaultActionGroup(txt("app.liquibase.group.PreviewSql"), true);
        sqlPreviewActions.add(action(schema, UPDATE_SQL));
        sqlPreviewActions.add(action(schema, ROLLBACK_SQL));
        sqlPreviewActions.add(action(schema, FUTURE_ROLLBACK));
        sqlPreviewActions.add(action(schema, SYNCHRONIZE_CHANGELOG_SQL));
        add(sqlPreviewActions);

        DefaultActionGroup moreActions = new DefaultActionGroup(txt("app.liquibase.group.More"), true);
        moreActions.add(action(schema, DROP_ALL));
        add(moreActions);
    }

    private static LiquibaseOperationAction action(
            @NotNull DBSchema schema,
            @NotNull LiquibaseOperation operation) {
        return new LiquibaseOperationAction(schema, operation);
    }
}
