package com.dbn.connection;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.exception.Exceptions;
import lombok.Getter;

/**
 * Wraps the information needed to handle a connection failure exception
 */
@Getter
public class ConnectionExceptionInfo {
    private final Throwable caughtException;
    private final ClassLoader classLoader;
    private final AuthenticationInfo authenticationInfo;

    public ConnectionExceptionInfo(Throwable caughtException, ClassLoader classLoader, AuthenticationInfo authenticationInfo) {
        this.caughtException = caughtException;
        this.classLoader = classLoader;
        this.authenticationInfo = authenticationInfo;
    }

    public void accept(ConnectionExceptionVisitor visitor) {
        Exceptions.accept(visitor, caughtException);
    }

}
