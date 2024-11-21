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

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.routine.Consumer;
import com.dbn.database.interfaces.DatabaseInterface.Runnable;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class ResultSets extends StatefulDisposableBase {
    public static void insertRow(ResultSet resultSet) throws SQLException {
        try {
            resultSet.insertRow();
        } catch (Throwable e) {
            conditionallyLog(e);
            throw Exceptions.toSqlException(e, "Error inserting row");
        }
    }

    public static void moveToInsertRow(ResultSet resultSet) throws SQLException {
        try {
            resultSet.moveToInsertRow();
        } catch (Throwable e) {
            conditionallyLog(e);
            throw Exceptions.toSqlException(e, "Error selecting insert row");
        }
    }
    public static void moveToCurrentRow(ResultSet resultSet) throws SQLException {
        try {
            resultSet.moveToCurrentRow();
        } catch (Throwable e) {
            conditionallyLog(e);
            throw Exceptions.toSqlException(e, "Error selecting current row");
        }
    }

    public static void deleteRow(final ResultSet resultSet) throws SQLException {
        try {
            resultSet.deleteRow();
        } catch (Throwable e) {
            conditionallyLog(e);
            throw Exceptions.toSqlException(e, "Error deleting row");
        }
    }


    public static void refreshRow(final ResultSet resultSet) throws SQLException {
        try {
            resultSet.refreshRow();
        } catch (Throwable e) {
            conditionallyLog(e);
            throw Exceptions.toSqlException(e, "Error refreshing row");
        }
    }

    public static void updateRow(final ResultSet resultSet) throws SQLException {
        try {
            resultSet.updateRow();
        } catch (Throwable e) {
            conditionallyLog(e);
            throw Exceptions.toSqlException(e, "Error updating row");
        }
    }

    public static void absolute(ResultSet resultSet, int row) throws SQLException {
        try {
            resultSet.absolute(row);
        } catch (Throwable e) {
            conditionallyLog(e);
            throw Exceptions.toSqlException(e, "Error selecting row");
        }
    }

    public static List<String> getColumnNames(ResultSet resultSet) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i=0; i<columnCount; i++) {
            columnNames.add(metaData.getColumnName(i+1).intern());
        }
        return columnNames;
    }

    public static void forEachRow(ResultSet resultSet, Runnable consumer) throws SQLException {
        try {
            if (resultSet != null && !Resources.isClosed(resultSet)) {
                while (resultSet.next()) {
                    consumer.run();
                }
            }
        } finally {
            Resources.close(resultSet);
        }
    }

    public static <T> void forEachRow(ResultSet resultSet, String columnName, Class<T> columnType, Consumer<T> consumer) throws SQLException {
        forEachRow(resultSet, () -> {
            Object object = null;
            if (CharSequence.class.isAssignableFrom(columnType)) {
                object = resultSet.getString(columnName);

            } else if (Integer.class.isAssignableFrom(columnType)) {
                object = resultSet.getInt(columnName);

            } else {
                throw new UnsupportedOperationException("Lookup not implemented. Add more handlers here");
            }

            consumer.accept((T) object);
        });
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
