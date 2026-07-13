package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import liquibase.command.CommandScope;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.dbn.nls.NlsResources.txt;

/** Processor for validating a Liquibase changelog. */
public class LiquibaseValidationProcessor extends LiquibaseExecutionProcessor {
    public LiquibaseValidationProcessor(@NotNull LiquibaseExecutionInput input) {
        super(input);
    }

    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.VALIDATE;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionResult result) throws Exception {
        LiquibaseWorkspacePaths paths = getInput().getWorkspacePaths();
        Path changelogFile = paths.getMasterChangelogPath();
        result.setChangelogPath(changelogFile);
        if (!Files.isRegularFile(changelogFile)) {
            throw new IllegalStateException("Changelog file does not exist: " + changelogFile);
        }

        validateChangelog(paths, paths.getRelativePath(changelogFile), result);
        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogValidated", changelogFile));
    }

    private void validateChangelog(
            @NotNull LiquibaseWorkspacePaths paths,
            @NotNull String changelogFile,
            @NotNull LiquibaseExecutionResult result) throws Exception {
        withLiquibaseDatabase(true, database -> {
            checkCanceled();
            withLiquibaseScope(paths.getContentRootPath(), result, output -> {
                new CommandScope("validate")
                        .addArgumentValue("database", database)
                        .addArgumentValue("changelogFile", changelogFile)
                        .setOutput(output)
                        .execute();
                return null;
            });
            checkCanceled();
            return null;
        });
    }

}
