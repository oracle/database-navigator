package com.dbn.debugger.jdwp.process;

import com.dbn.connection.ConnectionHandler;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.NotNull;

public class DBJavaJdwpCloudProcessStarter extends DBJdwpCloudProcessStarter {
    DBJavaJdwpCloudProcessStarter(ConnectionHandler connection) {
        super(connection);
    }

    @Override
    protected DBJdwpDebugProcess createDebugProcess(@NotNull XDebugSession session, DebuggerSession debuggerSession, DBJdwpTcpConfig tcpConfig) {
        return new DBJavaJdwpDebugProcess(session, debuggerSession, getConnection(), tcpConfig);
    }
}
