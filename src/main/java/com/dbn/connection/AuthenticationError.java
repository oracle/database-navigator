package com.dbn.connection;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.util.TimeUtil;
import lombok.Getter;

import java.sql.SQLException;

@Getter
public class AuthenticationError {
    public static final long THREE_MINUTES = TimeUtil.Millis.ONE_MINUTE * 3;
    private final AuthenticationInfo authenticationInfo;
    private final SQLException exception;
    private final long timestamp;

    public AuthenticationError(AuthenticationInfo authenticationInfo, SQLException exception) {
        this.authenticationInfo = authenticationInfo.clone();
        this.exception = exception;
        timestamp = System.currentTimeMillis();
    }

    public boolean isObsolete(AuthenticationInfo authenticationInfo){
        return !this.authenticationInfo.isSame(authenticationInfo);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > THREE_MINUTES;
    }
}
