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

package com.dbn.event.registration;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ResultSets;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.event.registration.model.DataChangeRegistration;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.nls.NlsResources.txt;

@UtilityClass
public class EventRegistrationUtil {

    private static @NonNls DataChangeRegistration createRegistration(Project project, ResultSet rs) throws SQLException {
        return new DataChangeRegistration(
                project,
                rs.getString("USER_NAME"),
                rs.getLong("REG_ID"),
                rs.getInt("REG_FLAGS"),
                rs.getString("CALLBACK"),
                rs.getInt("OPERATIONS"),
                rs.getInt("CHANGE_LAG"),
                rs.getLong("TIMEOUT"),
                rs.getString("TABLE_NAME")
        );
    }

    public static List<DataChangeRegistration> fetchRegistrations(ConnectionHandler connection) throws SQLException {
        Project project = connection.getProject();
        return DatabaseInterfaceInvoker.load(
                HIGH,
                txt("prc.events.title.LoadingDcnRegistrations"),
                txt("prc.events.text.FetchingDcnRegistrations"),
                project,
                connection.getConnectionId(),
                conn -> {
                    DatabaseMetadataInterface metadataInterface = connection.getMetadataInterface();
                    ResultSet resultSet = metadataInterface.loadDataEventRegistrations(conn);
                    return ResultSets.convert(resultSet, rs -> createRegistration(project, rs));
                }
        );
    }
}
