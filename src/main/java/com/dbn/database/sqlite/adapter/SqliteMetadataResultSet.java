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

package com.dbn.database.sqlite.adapter;

import com.dbn.common.cache.Cache;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.util.ResultSetStub;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Deprecated // replace with com.dbn.database.common.util.CachedResultSet
public class SqliteMetadataResultSet<T extends SqliteMetadataResultSetRow>
        extends StatefulDisposableBase
        implements ResultSetStub {

    private final List<T> rows = new ArrayList<>();
    private int cursor = -1;

    @Override
    public boolean next() throws SQLException {
        cursor++;
        return cursor < rows.size();
    }

    protected T current() {
        return rows.get(cursor);
    }

    public void add(T element) {
        rows.add(element);
        rows.sort(null);
    }

    protected T row(String name) {
        for (T element : rows) {
            if (Strings.equalsIgnoreCase(element.identifier(), name)) {
                return element;
            }
        }
        return null;
    }

    protected static String toFlag(boolean value) {
        return value ? "Y" : "N";
    }

    @Override
    public void close() throws SQLException {
        // nothing to close
    }

    @Override
    public boolean isClosed() throws SQLException {
        return false;
    }

    protected static Cache cache() {
        return ConnectionHandler.local().getMetaDataCache();
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
