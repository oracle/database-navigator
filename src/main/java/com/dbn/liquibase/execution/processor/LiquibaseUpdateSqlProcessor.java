package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.UPDATE_SQL;

/** Generates the SQL for pending Liquibase changesets without modifying the target schema. */
public class LiquibaseUpdateSqlProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.UPDATE_SQL;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        prepareChangelogContext(context, true);

        LiquibaseExecutionInput input = context.getInput();
        LiquibaseExecutionResult result = context.getResult();
        LiquibaseWorkspacePaths paths = input.getWorkspacePaths();
        Path changelogFile = paths.getMasterChangelogPath();

        withLiquibaseDatabase(context, true, context.getTargetSchema(), database -> {
            checkCanceled(context);
            return withLiquibaseScope(context, paths.getContentRootPath(), output ->
                    executeCommand(UPDATE_SQL, output, Map.of(
                            "database", database,
                            "changelogFile", paths.getRelativePath(changelogFile),
                            "changeExecListener", new LiquibaseChangeSetRunListener(result))));
        });
    }

}
