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

package com.dbn.database.interfaces;

import com.dbn.database.DatabaseMessage;
import com.dbn.database.DatabaseObjectIdentifier;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

/**
 * Classifies and normalizes vendor-specific database messages and SQL exceptions.
 */
public interface DatabaseMessageParserInterface extends DatabaseInterface {
    @Override
    default DatabaseInterfaceType getInterfaceType() {
        return DatabaseInterfaceType.MESSAGE_PARSER;
    }

    @Nullable
    DatabaseObjectIdentifier identifyObject(SQLException exception);

    boolean isTimeoutException(SQLException e);

    boolean isModelException(SQLException e);

    boolean isAuthenticationException(SQLException e);

    default boolean isPasswordExpiredException(SQLException e) {
        return false;
    }

    boolean isSuccessException(SQLException exception);

    default DatabaseMessage parseExceptionMessage(SQLException exception) {
        return new DatabaseMessage(exception.getMessage(), null);
    }

    default String convertToPresentable(String message) {
        return message;
    }

    /**
     * Formats a database-specific error number for display.
     */
    @NonNls
    default String formatErrorCode(@NotNull String errorCode) {
        return "error code " + errorCode;
    }
}
