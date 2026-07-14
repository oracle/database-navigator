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

package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.dbn.nls.NlsResources.txt;

/**
 * Reports the pending changesets in the workspace changelog for the selected target schema.
 *
 * <p>The processor executes Liquibase's {@code status} command in read-only mode. It resolves the
 * master changelog relative to the workspace content root and forwards Liquibase's detailed status
 * output to the execution result console.</p>
 *
 * <p>This operation is informational: it does not modify the database, changelog files, or
 * Liquibase tracking tables. The target schema is still required because status is evaluated against
 * the database's changelog history.</p>
 */
public class LiquibaseStatusProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.STATUS;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        LiquibaseExecutionInput input = context.getInput();
        LiquibaseExecutionResult result = context.getResult();
        LiquibaseWorkspacePaths paths = input.getWorkspacePaths();
        Path changelogFile = paths.getMasterChangelogPath();
        result.setChangelogPath(changelogFile);
        if (!Files.isRegularFile(changelogFile)) {
            throw new IllegalStateException("Changelog file does not exist: " + changelogFile);
        }

        DBSchema targetSchema = required("Target schema", input.getTargetSchema());
        String relativeChangelog = paths.getRelativePath(changelogFile);
        withLiquibaseDatabase(context, true, targetSchema, database -> {
            checkCanceled(context);
            withLiquibaseScope(context, paths.getContentRootPath(), output ->
                    executeCommand("status", output, Map.of(
                            "database", database,
                            "changelogFile", relativeChangelog,
                            "verbose", true)));
            checkCanceled(context);
            return null;
        });
        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogStatusDisplayed", changelogFile));
    }
}
