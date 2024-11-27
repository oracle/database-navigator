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

import com.dbn.common.list.FilteredList;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.data.model.DataModelState;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.data.model.sortable.SortableDataModelState;
import com.dbn.editor.session.SessionBrowserFilter;
import com.dbn.editor.session.SessionBrowserFilterType;
import com.dbn.editor.session.SessionBrowserState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SessionBrowserModel extends ResultSetDataModel<SessionBrowserModelRow, SessionBrowserModelCell>{
    private final long timestamp = System.currentTimeMillis();
    private String loadError;

    public SessionBrowserModel(ConnectionHandler connection) {
        super(connection);
        setHeader(new SessionBrowserModelHeader());
    }

    public SessionBrowserModel(ConnectionHandler connection, DBNResultSet resultSet) throws SQLException {
        super(connection);
        setHeader(new SessionBrowserModelHeader(connection, resultSet));
        checkDisposed();
        setResultSet(resultSet);
        setResultSetExhausted(false);
        fetchNextRecords(10000, true);
    }

    public String getLoadError() {
        return loadError;
    }

    public void setLoadError(String loadError) {
        this.loadError = loadError;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Nullable
    @Override
    public SessionBrowserFilter getFilter() {
        return (SessionBrowserFilter) super.getFilter();
    }

    @Override
    protected SortableDataModelState createState() {
        return new SessionBrowserState();
    }

    @NotNull
    @Override
    public SessionBrowserState getState() {
        return (SessionBrowserState) super.getState();
    }

    @Override
    public void setState(DataModelState state) {
        super.setState(state);
        if (state instanceof SessionBrowserState) {
            SessionBrowserState sessionBrowserState = (SessionBrowserState) state;
            setFilter(sessionBrowserState.getFilterState());
        }
        sort();
    }

    @NotNull
    @Override
    public SessionBrowserModelHeader getHeader() {
        return (SessionBrowserModelHeader) super.getHeader();
    }

    @Override
    protected SessionBrowserModelRow createRow(int resultSetRowIndex) throws SQLException {
        return new SessionBrowserModelRow(this, getResultSet(), resultSetRowIndex);
    }

    public List<String> getDistinctValues(SessionBrowserFilterType filterType, String selectedValue) {
        switch (filterType) {
            case USER: return getDistinctValues("USER", selectedValue);
            case HOST: return getDistinctValues("HOST", selectedValue);
            case STATUS: return getDistinctValues("STATUS", selectedValue);
        }
        return null;
    }

    private List<String> getDistinctValues(String columnName, String selectedValue) {
        ArrayList<String> values = new ArrayList<>();
        List<SessionBrowserModelRow> rows = FilteredList.unwrap(getRows());
        for (SessionBrowserModelRow row : rows) {
            String value = (String) row.getCellValue(columnName);
            if (Strings.isNotEmpty(value) && !values.contains(value)) {
                values.add(value);
            }
        }
        if (Strings.isNotEmpty(selectedValue) && !values.contains(selectedValue)) {
            values.add(selectedValue);
        }
        Collections.sort(values);
        return values;
    }


    /*********************************************************
     *                      DataModel                       *
     *********************************************************/
    @Override
    public SessionBrowserModelCell getCellAt(int rowIndex, int columnIndex) {
        return super.getCellAt(rowIndex, columnIndex);
    }
}
