package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.operation.LiquibaseOperationContext;
import com.dbn.liquibase.workspace.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import static com.dbn.liquibase.execution.LiquibaseCommands.VALIDATE_CHANGELOG;
import static com.dbn.nls.NlsResources.txt;

/**
 * Validates the structure and references of the workspace's existing Liquibase changelog.
 *
 * <p>The processor requires a target schema and an existing master changelog, then executes
 * Liquibase's {@code validate} command in the workspace content root. Validation checks that the
 * changelog can be parsed and that its changesets and referenced resources are internally valid;
 * it does not apply changes to the database or compare the database schema with the changelog.</p>
 *
 * <p>Liquibase output is forwarded to the execution result console, while failures are reported by
 * the common execution processor as a failed operation.</p>
 */
public class LiquibaseValidateChangelogProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.VALIDATE_CHANGELOG;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        var result = context.getResult();
        var paths = context.getInput().getWorkspacePaths();

        Path changelogFile = paths.getMasterChangelogPath();
        validateChangelog(context);
        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogValidated", changelogFile));
    }

    private void validateChangelog(@NotNull LiquibaseOperationContext context) throws Exception {
        DBSchema targetSchema = context.getTargetSchema();
        withLiquibaseDatabase(context, true, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeValidation(
                                context,
                                database,
                                output)));
    }

    private void executeValidation(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();

        var arguments = arguments(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath());
        executeCommand(VALIDATE_CHANGELOG, context, output, arguments);
    }

}
