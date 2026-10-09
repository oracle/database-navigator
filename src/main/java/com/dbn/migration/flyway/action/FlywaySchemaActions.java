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

package com.dbn.migration.flyway.action;

import com.dbn.common.action.DefaultActionGroup;
import com.dbn.common.icon.Icons;
import com.dbn.migration.flyway.operation.action.FlywayOperationDashboardAction;
import com.dbn.menu.action.FlywayWorkspacesOpenAction;
import com.dbn.object.DBSchema;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

/**
 * Flyway actions available for a single database schema.
 */
public class FlywaySchemaActions extends DefaultActionGroup {
    public FlywaySchemaActions(@NotNull DBSchema schema) {
        super(txt("app.flyway.action.Flyway"), true);
        getTemplatePresentation().setIcon(Icons.DB_FLYWAY);
        add(new FlywayWorkspacesOpenAction(txt("app.flyway.action.Workspaces"), Icons.ACTION_OPTIONS));
        addSeparator();
        add(new FlywayOperationDashboardAction(schema));
    }
}
