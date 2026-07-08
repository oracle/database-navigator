/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.database.oracle;

import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.DatabaseInterfaceBase;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.interfaces.DatabaseSchedulerInterface;
import com.dbn.scheduler.SchedulerJobSnapshot;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OracleSchedulerInterface extends DatabaseInterfaceBase implements DatabaseSchedulerInterface {
    public OracleSchedulerInterface(DatabaseInterfaces provider) {
        super("oracle_scheduler_interface.xml", provider);
    }

    @Override
    public void createJob(@NotNull DBNConnection connection, @NotNull String jobName, @NotNull String jobAction) throws SQLException {
        executeUpdate(connection, "create-job", jobName, jobAction);
    }

    @NotNull
    @Override
    public SchedulerJobSnapshot loadJobSnapshot(@NotNull DBNConnection connection, @NotNull String jobName) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = executeQuery(connection, "load-job-snapshot", jobName);
            if (!resultSet.next()) return SchedulerJobSnapshot.notFound();

            return new SchedulerJobSnapshot(
                    resultSet.getString("state"),
                    resultSet.getString("run_status"),
                    resultSet.getString("error_number"),
                    resultSet.getString("additional_info"));
        } finally {
            Resources.close(resultSet);
        }
    }

    @Override
    public void stopJob(@NotNull DBNConnection connection, @NotNull String jobName) throws SQLException {
        executeUpdate(connection, "stop-job", jobName);
    }

    @Override
    public void dropJob(@NotNull DBNConnection connection, @NotNull String jobName, boolean force) throws SQLException {
        executeUpdate(connection, force ? "force-drop-job" : "drop-job", jobName);
    }
}
