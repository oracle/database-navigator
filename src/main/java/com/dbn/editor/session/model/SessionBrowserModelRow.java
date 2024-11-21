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

package com.dbn.editor.session.model;

import com.dbn.connection.ConnectionHandler;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.resultSet.ResultSetColumnInfo;
import com.dbn.data.model.resultSet.ResultSetDataModelRow;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.editor.session.SessionIdentifier;
import com.dbn.editor.session.SessionStatus;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SessionBrowserModelRow
        extends ResultSetDataModelRow<SessionBrowserModel, SessionBrowserModelCell> {

    public SessionBrowserModelRow(SessionBrowserModel model, ResultSet resultSet, int resultSetRowIndex) throws SQLException {
        super(model, resultSet, resultSetRowIndex);
    }

    @NotNull
    @Override
    protected SessionBrowserModelCell createCell(ResultSet resultSet, ColumnInfo columnInfo) throws SQLException {
        return new SessionBrowserModelCell(this, resultSet, (ResultSetColumnInfo) columnInfo);
    }

    @NotNull
    @Override
    public SessionBrowserModel getModel() {
        return super.getModel();
    }

    public String getUser() {
        return (String) getCellValue("USER");
    }

    public String getHost() {
        return (String) getCellValue("HOST");
    }

    public String getStatus() {
        return (String) getCellValue("STATUS");
    }

    public Object getSessionId() {
        return getCellValue("SESSION_ID");
    }

    public Object getSerialNumber() {
        return getCellValue("SERIAL");
    }

    public SessionIdentifier getSessionIdentifier() {
        return new SessionIdentifier(getSessionId(), getSerialNumber());
    }

    public String getSchema() {
        return (String) getCellValue("SCHEMA");
    }

    public SessionStatus getSessionStatus() {
        ConnectionHandler connection = getModel().getConnection();
        DatabaseCompatibilityInterface compatibility = connection.getCompatibilityInterface();
        return compatibility.getSessionStatus(getStatus());
    }

}
