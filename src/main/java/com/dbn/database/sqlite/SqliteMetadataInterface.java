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

package com.dbn.database.sqlite;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.DatabaseMetadataInterfaceImpl;
import com.dbn.database.common.metadata.impl.DBSchemaMetadataImpl;
import com.dbn.database.common.util.WrappedResultSet;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.sqlite.adapter.rs.SqliteColumnConstraintsResultSet;
import com.dbn.database.sqlite.adapter.rs.SqliteColumnIndexesResultSet;
import com.dbn.database.sqlite.adapter.rs.SqliteColumnsResultSet;
import com.dbn.database.sqlite.adapter.rs.SqliteConstraintsResultSet;
import com.dbn.database.sqlite.adapter.rs.SqliteDatasetNamesResultSet;
import com.dbn.database.sqlite.adapter.rs.SqliteIndexesResultSet;
import com.dbn.database.sqlite.adapter.rs.SqliteTriggerSourceResultSet;
import com.dbn.database.sqlite.adapter.rs.SqliteTriggersResultSet;
import com.dbn.database.sqlite.adapter.rs.SqliteViewSourceResultSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;


@NonNls
class SqliteMetadataInterface extends DatabaseMetadataInterfaceImpl {

    SqliteMetadataInterface(DatabaseInterfaces provider) {
        super("sqlite_metadata_interface.xml", provider);
    }

    @Override
    public ResultSet loadCompileObjectErrors(String ownerName, String objectName, DBNConnection connection) throws SQLException {
        return null;
    }

    @Override
    public ResultSet loadSchemas(DBNConnection connection) throws SQLException {
        ResultSet resultSet = executeQuery(connection, "schemas");
        return new WrappedResultSet(resultSet) {
            /**
             * Metadata translation for SCHEMAS
             * comply with {@link DBSchemaMetadataImpl}
             */
            @Override
            public String getString(@NonNls String columnLabel) throws SQLException {
                switch (columnLabel) {
                    case "SCHEMA_NAME": return inner.getString("name");

                    case "IS_PUBLIC": return "N";
                    case "IS_SYSTEM": return "N";
                    case "IS_EMPTY": return "N";
                    default: return null;
                }
            }
        };
    }

    @Override
    public ResultSet loadColumns(final String ownerName, String datasetName, final DBNConnection connection) throws SQLException {
        return new SqliteColumnsResultSet(ownerName, datasetName, connection) {
            @Override
            protected ResultSet loadTableInfo(String datasetName) throws SQLException {
                return SqliteMetadataInterface.this.loadTableInfo(ownerName, datasetName, connection);
            }

            @Override
            protected ResultSet loadForeignKeyInfo(String datasetName) throws SQLException {
                return SqliteMetadataInterface.this.loadForeignKeyInfo(ownerName, datasetName, connection);
            }
        };
    }

    @Override
    public ResultSet loadAllColumns(final String ownerName, final DBNConnection connection) throws SQLException {
        return new SqliteColumnsResultSet(ownerName, getDatasetNames(ownerName, connection), connection) {
            @Override
            protected ResultSet loadTableInfo(String datasetName) throws SQLException {
                return SqliteMetadataInterface.this.loadTableInfo(ownerName, datasetName, connection);
            }

            @Override
            protected ResultSet loadForeignKeyInfo(String datasetName) throws SQLException {
                return SqliteMetadataInterface.this.loadForeignKeyInfo(ownerName, datasetName, connection);
            }
        };
    }

    @Override
    public ResultSet loadIndexes(final String ownerName, String tableName, final DBNConnection connection) throws SQLException {
        return new SqliteIndexesResultSet(ownerName, tableName, connection) {
            @Override
            protected ResultSet loadIndexInfo(String tableName) throws SQLException {
                return SqliteMetadataInterface.this.loadIndexInfo(ownerName, tableName, connection);
            }
        };
    }

    @Override
    public ResultSet loadAllIndexes(final String ownerName, final DBNConnection connection) throws SQLException {
        return new SqliteIndexesResultSet(ownerName, getDatasetNames(ownerName, connection), connection) {
            @Override
            protected ResultSet loadIndexInfo(String tableName) throws SQLException {
                return SqliteMetadataInterface.this.loadIndexInfo(ownerName, tableName, connection);
            }
        };
    }

    @Override
    public ResultSet loadDatasetTriggers(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        ResultSet resultSet = executeQuery(connection, "dataset-triggers", ownerName, datasetName);
        return new SqliteTriggersResultSet(resultSet);
    }

    @Override
    public ResultSet loadAllDatasetTriggers(String ownerName, DBNConnection connection) throws SQLException {
        ResultSet resultSet = executeQuery(connection, "all-dataset-triggers", ownerName);
        return new SqliteTriggersResultSet(resultSet);
    }

    @Override
    public ResultSet loadIndexRelations(final String ownerName, String tableName, final DBNConnection connection) throws SQLException {
        return new SqliteColumnIndexesResultSet(ownerName, tableName, connection) {
            @Override
            protected ResultSet loadIndexInfo(String tableName) throws SQLException {
                return SqliteMetadataInterface.this.loadIndexInfo(ownerName, tableName, connection);
            }

            @Override
            protected ResultSet loadIndexDetailInfo(String indexName) throws SQLException {
                return SqliteMetadataInterface.this.loadIndexDetailInfo(ownerName, indexName, connection);
            }
        };
    }

    @Override
    public ResultSet loadAllIndexRelations(final String ownerName, final DBNConnection connection) throws SQLException {
        return new SqliteColumnIndexesResultSet(ownerName, getDatasetNames(ownerName, connection), connection) {
            @Override
            protected ResultSet loadIndexInfo(String tableName) throws SQLException {
                return SqliteMetadataInterface.this.loadIndexInfo(ownerName, tableName, connection);
            }

            @Override
            protected ResultSet loadIndexDetailInfo(String indexName) throws SQLException {
                return SqliteMetadataInterface.this.loadIndexDetailInfo(ownerName, indexName, connection);
            }
        };
    }

    @Override
    public ResultSet loadConstraints(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        return new ConstraintsResultSet(ownerName, datasetName, connection);
    }

    @Override
    public ResultSet loadAllConstraints(String ownerName, DBNConnection connection) throws SQLException {
        return new ConstraintsResultSet(ownerName, getDatasetNames(ownerName, connection), connection);
    }

    @Override
    public ResultSet loadConstraintRelations(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        return new ColumnConstraintsResultSet(ownerName, datasetName, connection);
    }

    @Override
    public ResultSet loadAllConstraintRelations(String ownerName, DBNConnection connection) throws SQLException {
        return new ColumnConstraintsResultSet(ownerName, getDatasetNames(ownerName, connection), connection);
    }

    private class ConstraintsResultSet extends SqliteConstraintsResultSet {

        ConstraintsResultSet(String ownerName, SqliteDatasetNamesResultSet datasetNames, DBNConnection connection) throws SQLException {
            super(ownerName, datasetNames, connection);
        }

        ConstraintsResultSet(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
            super(ownerName, datasetName, connection);
        }

        @Override
        protected ResultSet loadTableInfo(String ownerName, String datasetName) throws SQLException {
            return SqliteMetadataInterface.this.loadTableInfo(ownerName, datasetName, getConnection());
        }

        @Override
        protected ResultSet loadForeignKeyInfo(String ownerName, String datasetName) throws SQLException {
            return SqliteMetadataInterface.this.loadForeignKeyInfo(ownerName, datasetName, getConnection());
        }

        @Override
        protected ResultSet loadIndexInfo(String ownerName, String tableName) throws SQLException {
            return SqliteMetadataInterface.this.loadIndexInfo(ownerName, tableName, getConnection());
        }

        @Override
        protected ResultSet loadIndexDetailInfo(String ownerName, String indexName) throws SQLException {
            return SqliteMetadataInterface.this.loadIndexDetailInfo(ownerName, indexName, getConnection());
        }
    }

    private class ColumnConstraintsResultSet extends SqliteColumnConstraintsResultSet {

        ColumnConstraintsResultSet(String ownerName, SqliteDatasetNamesResultSet datasetNames, DBNConnection connection) throws SQLException {
            super(ownerName, datasetNames, connection);
        }

        ColumnConstraintsResultSet(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
            super(ownerName, datasetName, connection);
        }

        @Override
        protected ResultSet loadTableInfo(String ownerName, String datasetName) throws SQLException {
            return SqliteMetadataInterface.this.loadTableInfo(ownerName, datasetName, getConnection());
        }

        @Override
        protected ResultSet loadForeignKeyInfo(String ownerName, String datasetName) throws SQLException {
            return SqliteMetadataInterface.this.loadForeignKeyInfo(ownerName, datasetName, getConnection());
        }

        @Override
        protected ResultSet loadIndexInfo(String ownerName, String tableName) throws SQLException {
            return SqliteMetadataInterface.this.loadIndexInfo(ownerName, tableName, getConnection());
        }

        @Override
        protected ResultSet loadIndexDetailInfo(String ownerName, String indexName) throws SQLException {
            return SqliteMetadataInterface.this.loadIndexDetailInfo(ownerName, indexName, getConnection());
        }
    }

    private ResultSet loadTableInfo(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "dataset-columns", ownerName, datasetName);
    }

    private ResultSet loadForeignKeyInfo(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "foreign-key-constraints", ownerName, datasetName);
    }

    private ResultSet loadIndexInfo(String ownerName, String tableName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "indexes", ownerName, tableName);
    }

    private ResultSet loadIndexDetailInfo(String ownerName, String indexName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "index-info", ownerName, indexName);
    }

    @NotNull
    private SqliteDatasetNamesResultSet getDatasetNames(final String ownerName, final DBNConnection connection) throws SQLException {
        return new SqliteDatasetNamesResultSet(ownerName) {
            @Override
            protected ResultSet loadTableNames() throws SQLException {
                return executeQuery(connection, "dataset-names", ownerName);
            }
        };
    }

    @Override
    public ResultSet loadViewSourceCode(String ownerName, String viewName, DBNConnection connection) throws SQLException {
        ResultSet resultSet = executeQuery(connection, "view-source-code", ownerName, viewName);
        return new SqliteViewSourceResultSet(resultSet);
    }

    @Override
    public ResultSet loadDatasetTriggerSourceCode(String tableOwner, String tableName, String ownerName, String triggerName, DBNConnection connection) throws SQLException {
        ResultSet resultSet = executeQuery(connection, "dataset-trigger-source-code", ownerName, tableName, triggerName);
        return new SqliteTriggerSourceResultSet(resultSet);
    }

    @Override
    public String createDateString(Date date) {
        String dateString = META_DATE_FORMAT.get().format(date);
        return "str_to_date('" + dateString + "', '%Y-%m-%d %T')";
    }

    @Override
    public void terminateSession(Object sessionId, Object serialNumber, boolean immediate, DBNConnection connection) throws SQLException {
        executeStatement(connection, "kill-session", sessionId);
    }

    @Override
    public String loadSessionUser(DBNConnection connection) {
        // no session user concept in sqlite
        return null;
    }
}