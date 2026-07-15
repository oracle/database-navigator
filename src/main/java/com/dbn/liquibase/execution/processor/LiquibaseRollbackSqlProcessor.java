package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.ROLLBACK_COUNT_SQL;
import static com.dbn.liquibase.execution.LiquibaseCommands.ROLLBACK_DATE_SQL;
import static com.dbn.liquibase.execution.LiquibaseCommands.ROLLBACK_TAG_SQL;

/** Generates rollback SQL without modifying the target schema. */
public class LiquibaseRollbackSqlProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.ROLLBACK_SQL;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        prepareChangelogContext(context, true);

        withLiquibaseDatabase(context, true, context.getTargetSchema(), database ->
                withLiquibaseScope(context, contentRootAccessor(context), sqlOutputBuilder(context),
                        output -> executeRollbackSql(
                                context,
                                database,
                                output)));
    }

    private void executeRollbackSql(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        var input = context.getInput();
        var result = context.getResult();
        var paths = input.getWorkspacePaths();
        var instruction = input.getRollbackInstruction();

        executeCommand(getCommand(input), output, Map.of(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                instruction.parameter(), instruction.value(),
                "changeExecListener", new LiquibaseChangeSetRollbackListener(result, "SQL generated")));
    }

    @NotNull
    private static String getCommand(@NotNull LiquibaseExecutionInput input) {
        return switch (input.getRollbackType()) {
            case COUNT -> ROLLBACK_COUNT_SQL;
            case TAG -> ROLLBACK_TAG_SQL;
            case DATE -> ROLLBACK_DATE_SQL;
        };
    }

}
