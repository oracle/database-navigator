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

package com.dbn.migration.liquibase.execution.processor;

import com.dbn.migration.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.migration.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.migration.liquibase.operation.LiquibaseOperation;
import com.dbn.migration.liquibase.operation.LiquibaseOperationContext;
import com.dbn.migration.liquibase.workspace.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import liquibase.CatalogAndSchema;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ResourceAccessor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.dbn.migration.liquibase.execution.LiquibaseCommands.GENERATE_DATABASE_DOCUMENTATION;
import static com.dbn.nls.NlsResources.txt;
import static liquibase.command.core.DbDocCommandStep.CATALOG_AND_SCHEMAS_ARG;
import static liquibase.command.core.DbDocCommandStep.OUTPUT_DIRECTORY_ARG;
import static liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep.DATABASE_ARG;

/** Generates Liquibase HTML documentation for the selected target schema and workspace changelog. */
public class LiquibaseGenerateDatabaseDocumentationProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.GENERATE_DATABASE_DOCUMENTATION;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        Path documentationDirectory = paths.getDocumentationDirectoryPath();
        context.getResult().setDocumentationPath(documentationDirectory.resolve("index.html"));
        Files.createDirectories(documentationDirectory);

        DBSchema targetSchema = context.getTargetSchema();
        withLiquibaseDatabase(context, true, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeDocumentation(context, database, output, documentationDirectory)));

        context.getResult().appendConsoleOutput(
                txt("log.liquibase.info.DatabaseDocumentationGenerated", documentationDirectory));
    }

    private void executeDocumentation(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output,
            @NotNull Path documentationDirectory) throws Exception {
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        ResourceAccessor resourceAccessor = contentRootAccessor(context);
        String changelogPath = paths.getMasterChangelogRelativePath();
        DatabaseChangeLog changeLog = ChangeLogParserFactory.getInstance()
                .getParser(changelogPath, resourceAccessor)
                .parse(changelogPath, new ChangeLogParameters(database), resourceAccessor);

        CatalogAndSchema catalogAndSchema = new CatalogAndSchema(
                database.getDefaultCatalogName(),
                context.getTargetSchema().getName());

        var arguments = arguments(
                "changelogFile", paths.getMasterChangelogRelativePath(),
                DATABASE_ARG.getName(), database,
                OUTPUT_DIRECTORY_ARG.getName(), documentationDirectory.toString(),
                CATALOG_AND_SCHEMAS_ARG.getName(), new CatalogAndSchema[]{catalogAndSchema});
        Map<Class<?>, Object> dependencies = Map.of(DatabaseChangeLog.class, changeLog);

        executeCommand(
                GENERATE_DATABASE_DOCUMENTATION,
                context,
                output,
                arguments,
                dependencies);
    }
}
