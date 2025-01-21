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

package com.dbn.database.generic;

import com.dbn.common.cache.CacheKey;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseCompatibility;
import com.dbn.database.JdbcProperty;
import com.dbn.database.common.util.CachedResultSet;
import com.dbn.database.common.util.CachedResultSetRow;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.database.interfaces.DatabaseInterface.Callable;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import org.jetbrains.annotations.NonNls;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static com.dbn.common.cache.CacheKey.key;

@NonNls
public interface GenericMetadataLoaders {
    CachedResultSet.Mapper<String> METHOD_COLUMNS = original -> {
        switch (original) {
            case "FUNCTION_CAT":
            case "PROCEDURE_CAT": return "METHOD_CAT";
            case "FUNCTION_SCHEM":
            case "PROCEDURE_SCHEM": return "METHOD_SCHEM";
            case "FUNCTION_NAME":
            case "PROCEDURE_NAME": return "METHOD_NAME";
            case "FUNCTION_TYPE":
            case "PROCEDURE_TYPE": return "METHOD_TYPE";
            default: return null;
        }
    };

    /**************************************************************
     *                     Raw cached meta-data                   *
     **************************************************************/
    static CachedResultSet loadCatalogsRaw(DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_CATALOGS,
                key("CATALOGS"),
                () -> {
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getCatalogs();
                    return CachedResultSet.create(resultSet);
                });
    }

    static CachedResultSet loadSchemasRaw(DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_SCHEMAS,
                key("SCHEMAS"),
                () -> {
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getSchemas();
                    return CachedResultSet.create(resultSet);
                });
    }

    static CachedResultSet loadTablesRaw(String ownerName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_TABLES,
                key("TABLES", ownerName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getTables(owner[0], owner[1], null, new String[]{"TABLE", "SYSTEM TABLE"});
                    return CachedResultSet.create(resultSet);
                });
    }

    static CachedResultSet loadViewsRaw(String ownerName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_VIEWS,
                key("VIEWS", ownerName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getTables(owner[0], owner[1], null, new String[]{"VIEW", "SYSTEM VIEW"});
                    return CachedResultSet.create(resultSet);
                });
    }

    static CachedResultSet loadColumnsRaw(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_COLUMNS,
                key("COLUMNS", ownerName, datasetName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getColumns(owner[0], owner[1], datasetName, null);
                    return CachedResultSet.create(resultSet);
                });
    }

    static CachedResultSet loadPseudoColumnsRaw(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_PSEUDO_COLUMNS,
                key("PSEUDO_COLUMNS", ownerName, datasetName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getPseudoColumns(owner[0], owner[1], datasetName, null);
                    return CachedResultSet.create(resultSet);
                });
    }

    static CachedResultSet loadAllColumnsRaw(String ownerName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_COLUMNS,
                key("ALL_COLUMNS", ownerName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getColumns(owner[0], owner[1], null, null);
                    return CachedResultSet.create(resultSet);
                });
    }

    static CachedResultSet loadAllPseudoColumnsRaw(String ownerName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_PSEUDO_COLUMNS,
                key("ALL_PSEUDO_COLUMNS", ownerName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getPseudoColumns(owner[0], owner[1], null, null);
                    return CachedResultSet.create(resultSet);
                });
    }

    static CachedResultSet loadIndexesRaw(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_INDEXES,
                key("INDEXES", ownerName, datasetName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getIndexInfo(owner[0], owner[1], datasetName, false, true);
                    return CachedResultSet.create(resultSet);
                });
    }

    static CachedResultSet loadPrimaryKeysRaw(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_PRIMARY_KEYS,
                key("PRIMARY_KEYS", ownerName, datasetName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getPrimaryKeys(owner[0], owner[1], datasetName);
                    return CachedResultSet.create(resultSet);
                });
    }

    static CachedResultSet loadForeignKeysRaw(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_IMPORTED_KEYS,
                key("FOREIGN_KEYS", ownerName, datasetName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getImportedKeys(owner[0], owner[1], datasetName);
                    return CachedResultSet.create(resultSet);
                });
    }

    static CachedResultSet loadFunctionsRaw(String ownerName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_FUNCTIONS,
                key("FUNCTIONS", ownerName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getFunctions(owner[0], owner[1], null);
                    return CachedResultSet.create(resultSet, rs -> isDeclaredMethod(rs, connection)).normalize(METHOD_COLUMNS);
                });
    }

    static CachedResultSet loadFunctionArgumentsRaw(String ownerName, String functionName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_FUNCTION_COLUMNS,
                key("FUNCTION_ARGUMENTS", ownerName, functionName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getFunctionColumns(owner[0], owner[1], functionName, null);
                    return CachedResultSet.create(resultSet, rs -> isDeclaredMethod(rs, connection)).normalize(METHOD_COLUMNS);
                });
    }

    static CachedResultSet loadAllFunctionArgumentsRaw(String ownerName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_FUNCTION_COLUMNS,
                key("ALL_FUNCTION_ARGUMENTS", ownerName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getFunctionColumns(owner[0], owner[1], null, null);
                    return CachedResultSet.create(resultSet, rs -> isDeclaredMethod(rs, connection)).normalize(METHOD_COLUMNS);
                });
    }

    static CachedResultSet loadProceduresRaw(String ownerName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_PROCEDURES,
                key("PROCEDURES", ownerName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getProcedures(owner[0], owner[1], null);
                    return CachedResultSet.create(resultSet, rs -> isDeclaredMethod(rs, connection)).normalize(METHOD_COLUMNS);
                });
    }

    static CachedResultSet loadProcedureArgumentsRaw(String ownerName, String procedureName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_PROCEDURE_COLUMNS,
                key("PROCEDURE_ARGUMENTS", ownerName, procedureName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getProcedureColumns(owner[0], owner[1], procedureName, null);
                    return CachedResultSet.create(resultSet, rs -> isDeclaredMethod(rs, connection)).normalize(METHOD_COLUMNS);
                });
    }

    static CachedResultSet loadAllProcedureArgumentsRaw(String ownerName, DBNConnection connection) throws SQLException {
        return attemptCached(
                JdbcProperty.MD_PROCEDURE_COLUMNS,
                key("ALL_PROCEDURE_ARGUMENTS", ownerName),
                () -> {
                    String[] owner = lookupOwner(ownerName, connection);
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getProcedureColumns(owner[0], owner[1], null, null);
                    return CachedResultSet.create(resultSet, rs -> isDeclaredMethod(rs, connection)).normalize(METHOD_COLUMNS);
                });
    }


    static String[] lookupOwner(String ownerName, DBNConnection connection) throws SQLException {
        return DatabaseInterfaceInvoker.cached(
                key("CATALOG_SCHEMA", ownerName),
                () -> {
                    if (is(JdbcProperty.CATALOG_AS_OWNER)) {
                        return new String[]{ownerName, null};
                    } else {
                        CachedResultSet schemasRs = loadSchemasRaw(connection);
                        CachedResultSetRow schemaRow = schemasRs.first(row -> Objects.equals(ownerName, row.get("TABLE_SCHEM")));
                        String catalogName = schemaRow == null ? null : (String) schemaRow.get("TABLE_CATALOG");
                        return new String[]{catalogName, ownerName};
                    }
                });
    }

    static CachedResultSet attemptCached(JdbcProperty feature, CacheKey<CachedResultSet> key, Callable<CachedResultSet> loader) throws SQLException{
        return DatabaseInterfaceInvoker.cached(key, () -> {
            ConnectionHandler connection = ConnectionHandler.local();
            DatabaseCompatibilityInterface compatibilityInterface = connection.getCompatibilityInterface();
            CachedResultSet resultSet = compatibilityInterface.attemptFeatureInvocation(feature, loader);
            return Commons.nvl(resultSet, CachedResultSet.EMPTY);
        });
    }

    static DatabaseCompatibility getCompatibility() {
        return ConnectionHandler.local().getCompatibility();
    }

    static boolean is(JdbcProperty property) {
        return getCompatibility().is(property);
    }

    static boolean isDeclaredMethod(ResultSet rs, DBNConnection connection) throws SQLException {
        if (is(JdbcProperty.CATALOG_AS_OWNER)) {
            return true;
        } else {
            String catalog = GenericMetadataTranslators.resolve(
                    () -> rs.getString("FUNCTION_CAT"),
                    () -> rs.getString("PROCEDURE_CAT"),
                    () -> null);
            if (Strings.isEmpty(catalog)) {
                return true;
            } else {
                CachedResultSet catalogsRs = loadCatalogsRaw(connection);
                return catalogsRs.exists(row -> Objects.equals(catalog, row.get("TABLE_CAT")));
            }
        }
    }

}
