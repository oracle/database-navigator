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

package com.dbn.editor.session.details;

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.ui.table.DBNReadonlyTableModel;
import com.dbn.editor.session.model.SessionBrowserModelRow;
import org.jetbrains.annotations.Nullable;

public class SessionDetailsTableModel extends StatefulDisposableBase implements DBNReadonlyTableModel {
    private String sessionId = "";
    private String user = "";
    private String schema = "";
    private String host = "";
    private String status = "";

    public SessionDetailsTableModel() {
        this(null);
    }

    public SessionDetailsTableModel(@Nullable SessionBrowserModelRow row) {
        if (row == null) {
            sessionId = "";
            user = "";
            schema = "";
            host = "";
            status = "";
        } else {
            sessionId = String.valueOf(row.getSessionId());
            user = row.getUser();
            schema = row.getSchema();
            host = row.getHost();
            status = row.getStatus();
        }
    }

    @Override
    public int getRowCount() {
        return 4;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0: return "Attribute";
            case 1: return "Value";
        }
        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            switch (rowIndex) {
                case 0: return "Session Id";
                case 1: return "User";
                case 2: return "Schema";
                case 3: return "Host";
                case 4: return "Status";
            }
        } else if (columnIndex == 1) {
            switch (rowIndex) {
                case 0: return sessionId;
                case 1: return user;
                case 2: return schema;
                case 3: return host;
                case 4: return status;
            }
        }
        return null;
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
