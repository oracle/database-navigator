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

/** Processor for applying pending changesets from a Liquibase changelog. */
public class LiquibaseUpdateProcessor extends LiquibaseExecutionProcessor {
    public LiquibaseUpdateProcessor(@NotNull LiquibaseExecutionInput input) {
        super(input);
    }

    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.UPDATE;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionResult result) throws Exception {
        LiquibaseWorkspacePaths paths = getInput().getWorkspacePaths();
        Path changelogFile = paths.getMasterChangelogPath();
        result.setChangelogPath(changelogFile);
        if (!Files.isRegularFile(changelogFile)) {
            throw new IllegalStateException("Changelog file does not exist: " + changelogFile);
        }

        DBSchema targetSchema = required("Target schema", getInput().getTargetSchema());
        String relativeChangelog = paths.getRelativePath(changelogFile);
        withLiquibaseDatabase(false, targetSchema, database -> {
            checkCanceled();
            Path rootPath = paths.getContentRootPath();
            withLiquibaseScope(rootPath, result, output ->
                    executeCommand("update", output, Map.of(
                            "database", database,
                            "changelogFile", relativeChangelog)));
            checkCanceled();
            return null;
        });
        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogUpdated", changelogFile));
    }
}
