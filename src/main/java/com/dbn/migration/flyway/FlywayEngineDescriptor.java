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

package com.dbn.migration.flyway;

import com.dbn.migration.flyway.operation.FlywayOperation;
import com.dbn.migration.flyway.ui.FlywayTaskStarter;
import com.dbn.migration.flyway.workflow.FlywayWorkflow;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineDescriptor;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Descriptor for DBN's Flyway integration.
 *
 * <p>Provides Flyway's operation and workflow catalogues together with the display name
 * used by dashboard views and the project-specific task starter.</p>
 */
public final class FlywayEngineDescriptor
        extends DatabaseMigrationEngineDescriptor<FlywayOperation, FlywayWorkflow> {

    public FlywayEngineDescriptor() {
        super(
                DatabaseMigrationEngineType.FLYWAY,
                List.of(FlywayOperation.values()),
                List.of(FlywayWorkflow.values()));
    }

    @Override
    public @NotNull String getName() {
        return txt("app.flyway.action.Flyway");
    }

    @Override
    public @NotNull FlywayTaskStarter getTaskStarter(@NotNull Project project) {
        DatabaseFlywayManager flywayManager = DatabaseFlywayManager.getInstance(project);
        return flywayManager.getTaskStarter();
    }
}
