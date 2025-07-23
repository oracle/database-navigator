/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.prerequisite.resolution;

import com.dbn.connection.context.DatabaseContext;

import java.sql.SQLException;

public interface PrerequisiteResolver {
    /**
     * Component that attempts to resolve a not yet fulfilled prerequisite.
     * (is allowed to fail if the user does not have the necessary privileges)
     *
     * @param context the database context that encapsulates the database connection, session, schema, and other
     *                information needed for resolving the prerequisite.
     * @throws SQLException if an error occurs while resolving prerequisite in the database context.
     */
    void resolve(DatabaseContext context) throws SQLException;
}
