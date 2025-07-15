package com.dbn.debugger.jdwp.process;

import com.dbn.connection.ConnectionHandler;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.ExecutionTarget;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.common.DBSchemaObject;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;

public class DBJavaJdwpDebugProcess extends DBJdwpDebugProcess<JavaExecutionInput> {
    DBJavaJdwpDebugProcess(@NotNull XDebugSession session, @NotNull DebuggerSession debuggerSession, ConnectionHandler connection, DBJdwpTcpConfig tcpConfig) {
        super(session, debuggerSession, connection, tcpConfig);
    }

    @NotNull
    @Override
    public String getName() {
        JavaExecutionInput executionInput = getExecutionInput();
        if (executionInput != null) {
            DBJavaMethod method = executionInput.getMethod();
            DBSchemaObject object = getMainDatabaseObject(method);
            if (object != null) {
                return object.getQualifiedName();
            }
        }
        return "Debug Process";
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        JavaExecutionInput executionInput = getExecutionInput();
        if (executionInput != null) {
            DBJavaMethod method = executionInput.getMethod();
            DBSchemaObject object = getMainDatabaseObject(method);
            if (object != null) {
                return object.getIcon();
            }
        }
        return null;
    }

    @Nullable
    protected DBSchemaObject getMainDatabaseObject(DBJavaMethod method) {
        return method.getOwnerClass();
    }

    @Override
    protected void executeTarget() throws SQLException {
        JavaExecutionInput executionInput = getExecutionInput();
        if (executionInput != null) {
            JavaExecutionManager javaExecutionManager = JavaExecutionManager.getInstance(getProject());
            javaExecutionManager.debugExecute(executionInput, getTargetConnection(), DBDebuggerType.JDWP);
        }
    }

    @Override
    protected void releaseTargetConnection() {
        // method execution processor is responsible for closing
        // the connection after the result is read
        targetConnection = null;
    }

    @Override
    public ExecutionTarget getExecutionTarget() {
        return ExecutionTarget.JAVA;
    }
}
