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

package com.dbn.migration.liquibase;

import com.dbn.migration.liquibase.operation.LiquibaseOperation;
import com.dbn.migration.liquibase.ui.LiquibaseTaskStarter;
import com.dbn.migration.liquibase.workflow.LiquibaseWorkflow;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineDescriptor;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Descriptor for DBN's embedded Liquibase integration.
 *
 * <p>Provides Liquibase's operation and workflow catalogues together with the display name
 * used by dashboard views.</p>
 */
public final class LiquibaseEngineDescriptor
        extends DatabaseMigrationEngineDescriptor<LiquibaseOperation, LiquibaseWorkflow> {

    public LiquibaseEngineDescriptor() {
        super(
                DatabaseMigrationEngineType.LIQUIBASE,
                List.of(LiquibaseOperation.values()),
                List.of(LiquibaseWorkflow.values()));
    }

    @Override
    public @NotNull String getName() {
        return txt("app.liquibase.action.Liquibase");
    }

    @Override
    public @NotNull LiquibaseTaskStarter getTaskStarter(@NotNull Project project) {
        DatabaseLiquibaseManager liquibaseManager = DatabaseLiquibaseManager.getInstance(project);
        return liquibaseManager.getTaskStarter();
    }
}
