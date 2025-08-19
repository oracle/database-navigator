package com.dbn.connection;

import com.dbn.common.database.AuthenticationInfo;
import lombok.Getter;

/**
 * Wraps the information needed to handle a connection failure exception
 * Mainly, this exists to support workarounds to Bug_38087045 that are
 * largely due to defects in the OCI provider implementation.
 */
@Getter
public class ConnectionExceptionInfo {
    private final Throwable caughtException;
    private final ClassLoader classLoader;
    private final AuthenticationInfo authenticationInfo;

    /**
     *
     * @param caughtException
     * @param classLoader The classloader of the driver that generated the connection
     *        exception.
     * @param authenticationInfo  Used to distinguish the connection info that failed.
     */
    public ConnectionExceptionInfo(Throwable caughtException, ClassLoader classLoader, AuthenticationInfo authenticationInfo) {
        this.caughtException = caughtException;
        this.classLoader = classLoader;
        this.authenticationInfo = authenticationInfo;
    }
}
