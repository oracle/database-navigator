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

package com.dbn.liquibase;

import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.workflow.LiquibaseWorkflow;
import com.dbn.migration.engine.DatabaseMigrationEngineDescriptor;
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
        implements DatabaseMigrationEngineDescriptor<LiquibaseOperation, LiquibaseWorkflow> {
    private static final List<LiquibaseOperation> OPERATIONS = List.of(LiquibaseOperation.values());
    private static final List<LiquibaseWorkflow> WORKFLOWS = List.of(LiquibaseWorkflow.values());

    public LiquibaseEngineDescriptor() {
    }

    @Override
    public @NotNull String getId() {
        return "liquibase";
    }

    @Override
    public @NotNull String getName() {
        return txt("app.liquibase.action.Liquibase");
    }

    @Override
    public @NotNull List<LiquibaseOperation> getOperations() {
        return OPERATIONS;
    }

    @Override
    public @NotNull List<LiquibaseWorkflow> getWorkflows() {
        return WORKFLOWS;
    }
}
