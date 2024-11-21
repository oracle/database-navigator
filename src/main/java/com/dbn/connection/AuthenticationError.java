/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
