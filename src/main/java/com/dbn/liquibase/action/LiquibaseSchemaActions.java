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
import com.dbn.object.DBSchema;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

/** Liquibase operations available for a single database schema. */
public class LiquibaseSchemaActions extends DefaultActionGroup {
    public LiquibaseSchemaActions(@NotNull DBSchema schema) {
        super(txt("app.liquibase.action.Liquibase"), true);
        getTemplatePresentation().setIcon(Icons.DB_LIQUIBASE);

        DefaultActionGroup changelogActions = new DefaultActionGroup(txt("app.liquibase.group.Changelog"), true);
        changelogActions.add(new GenerateChangelogAction(schema));
        changelogActions.add(new GenerateDiffChangelogAction(schema));
        changelogActions.add(new ValidateChangelogAction(schema));
        changelogActions.add(new ShowChangelogStatusAction(schema));
        add(changelogActions);

        DefaultActionGroup databaseActions = new DefaultActionGroup(txt("app.liquibase.group.Database"), true);
        databaseActions.add(new UpdateDatabaseAction(schema));
        databaseActions.add(new RollbackDatabaseAction(schema));
        databaseActions.addSeparator();
        databaseActions.add(new SynchronizeChangelogAction(schema));
        databaseActions.add(new TagDatabaseAction(schema));
        add(databaseActions);

        DefaultActionGroup sqlPreviewActions = new DefaultActionGroup(txt("app.liquibase.group.PreviewSql"), true);
        sqlPreviewActions.add(new UpdateSqlAction(schema));
        sqlPreviewActions.add(new RollbackSqlAction(schema));
        add(sqlPreviewActions);

        addSeparator();
        add(new CompareSchemasAction(schema));
    }
}
