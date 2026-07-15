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
public class LiquibaseValidationProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.VALIDATE_CHANGELOG;
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

        validateChangelog(context, paths, paths.getRelativePath(changelogFile), result);
        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogValidated", changelogFile));
    }

    private void validateChangelog(
            @NotNull LiquibaseExecutionContext context,
            @NotNull LiquibaseWorkspacePaths paths,
            @NotNull String changelogFile,
            @NotNull LiquibaseExecutionResult result) throws Exception {
        DBSchema targetSchema = required("Target schema", context.getInput().getTargetSchema());

        withLiquibaseDatabase(context, true, targetSchema, database -> {
            checkCanceled(context);

            Path rootPath = paths.getContentRootPath();
            withLiquibaseScope(context, rootPath, output ->
                    executeCommand(VALIDATE_CHANGELOG, output, Map.of(
                            "database", database,
                            "changelogFile", changelogFile)));
            checkCanceled(context);
            return null;
        });
    }

}
