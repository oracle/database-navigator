package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

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

        withLiquibaseDatabase(context, true, context.getTargetSchema(), database ->
                withLiquibaseScope(context, contentRootAccessor(context), sqlOutputBuilder(context),
                        output -> executeUpdateSql(
                                context,
                                database,
                                output)));
    }

    private void executeUpdateSql(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        var result = context.getResult();
        var paths = context.getInput().getWorkspacePaths();

        executeCommand(UPDATE_SQL, output, Map.of(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                "changeExecListener", new LiquibaseChangeSetRunListener(result)));
    }

}
