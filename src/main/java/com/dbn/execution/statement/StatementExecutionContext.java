package com.dbn.execution.statement;

import com.dbn.common.interceptor.InterceptorContext;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.execution.ExecutionContext;
import com.dbn.language.common.psi.ExecutablePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StatementExecutionContext extends ExecutionContext<StatementExecutionInput> implements InterceptorContext {
    public StatementExecutionContext(StatementExecutionInput input) {
        super(input);
    }

    @NotNull
    @Override
    public String getTargetName() {
        ExecutablePsiElement executablePsiElement = getInput().getExecutablePsiElement();
        return Commons.nvl(executablePsiElement == null ? null : executablePsiElement.getPresentableText(), "Statement");
    }

    @Nullable
    @Override
    public ConnectionHandler getTargetConnection() {
        return getInput().getConnection();
    }

    @Nullable
    @Override
    public SchemaId getTargetSchema() {
        return getInput().getTargetSchemaId();
    }
}
