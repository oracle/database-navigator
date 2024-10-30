package com.dbn.database.common;

import com.dbn.common.latent.Latent;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.logging.ExecutionLogOutput;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public abstract class DatabaseMetadataInterfaceImpl extends DatabaseInterfaceBase implements DatabaseMetadataInterface {
    protected static final Latent<SimpleDateFormat> META_DATE_FORMAT = Latent.thread(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    public DatabaseMetadataInterfaceImpl(String fileName, DatabaseInterfaces interfaces) {
        super(fileName, interfaces);
    }

    @Override
    public ResultSet getDistinctValues(String ownerName, String datasetName, String columnName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "load-distinct-values", ownerName, datasetName, columnName);
    }

    @Override
    public void setCurrentSchema(String schemaName, DBNConnection connection) throws SQLException {
        executeQuery(connection, "set-current-schema", schemaName);
    }

    @Override
    public ResultSet loadUsers(DBNConnection connection) throws SQLException {
        return executeQuery(connection, "users");
    }

    @Override
    public ResultSet loadCharsets(DBNConnection connection) throws SQLException {
        return executeQuery(connection, "charsets");
    }

    @Override
    public ResultSet loadRoles(DBNConnection connection) throws SQLException {
        return executeQuery(connection, "roles");
    }

    @Override
    public ResultSet loadAllUserRoles(DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-user-roles");
    }


    @Override
    public ResultSet loadSystemPrivileges(DBNConnection connection) throws SQLException {
        return executeQuery(connection, "system-privileges");
    }

    @Override
    public ResultSet loadObjectPrivileges(DBNConnection connection) throws SQLException {
        return executeQuery(connection, "object-privileges");
    }

    @Override
    public ResultSet loadAllUserPrivileges(DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-user-privileges");
    }

    @Override
    public ResultSet loadAllRolePrivileges(DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-role-privileges");
    }

    @Override
    public ResultSet loadAllRoleRoles(DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-role-roles");
    }

    @Override
    public ResultSet loadSchemas(DBNConnection connection) throws SQLException {
        return executeQuery(connection, "schemas");
    }

    @Override
    public ResultSet loadClusters(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "clusters", ownerName);
    }

    @Override
    public ResultSet loadTables(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "tables", ownerName);
    }

    @Override
    public ResultSet loadViews(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "views", ownerName);
    }

    @Override
    public ResultSet loadJavaObjects(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "java-objects", ownerName);
    }

    @Override
    public ResultSet loadMaterializedViews(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "materialized-views", ownerName);
    }

    @Override
    public ResultSet loadColumns(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "dataset-columns", ownerName, datasetName);
    }

    @Override
    public ResultSet loadAllColumns(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-dataset-columns", ownerName);
    }

    @Override
    public ResultSet loadConstraintRelations(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "column-constraint-relations", ownerName, datasetName);
    }

    @Override
    public ResultSet loadAllConstraintRelations(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-column-constraint-relations", ownerName);
    }

    @Override
    public ResultSet loadIndexRelations(String ownerName, String tableName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "column-index-relations", ownerName, tableName);
    }

    @Override
    public ResultSet loadAllIndexRelations(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-column-index-relations", ownerName);
    }

    @Override
    public ResultSet loadConstraints(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "constraints", ownerName, datasetName);
    }

    @Override
    public ResultSet loadAllConstraints(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-constraints", ownerName);
    }

    @Override
    public ResultSet loadIndexes(String ownerName, String tableName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "indexes", ownerName, tableName);
    }

    @Override
    public ResultSet loadAllIndexes(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-indexes", ownerName);
    }

    @Override
    public ResultSet loadNestedTables(String ownerName, String tableName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "nested-tables", ownerName, tableName);
    }

    @Override
    public ResultSet loadAllNestedTables(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-nested-tables", ownerName);
    }

    @Override
    public ResultSet loadDatabaseTriggers(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "database-triggers", ownerName);
    }

    @Override
    public ResultSet loadDatasetTriggers(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "dataset-triggers", ownerName, datasetName);
    }

    @Override
    public ResultSet loadAllDatasetTriggers(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-dataset-triggers", ownerName);
    }

    @Override
    public ResultSet loadFunctions(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "functions", ownerName);
    }

    @Override
    public ResultSet loadProcedures(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "procedures", ownerName);
    }

    @Override
    public ResultSet loadDimensions(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "dimensions", ownerName);
    }


   /*********************************************************
    *                        PACKAGES                       *
    *********************************************************/
    @Override
    public ResultSet loadPackages(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "packages", ownerName);
    }

    @Override
    public ResultSet loadPackageFunctions(String ownerName, String packageName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "package-functions", ownerName, packageName);
    }

    @Override
    public ResultSet loadAllPackageFunctions(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-package-functions", ownerName);
    }

    @Override
    public ResultSet loadPackageProcedures(String ownerName, String packageName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "package-procedures", ownerName, packageName);
    }

    @Override
    public ResultSet loadAllPackageProcedures(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-package-procedures", ownerName);
    }

    @Override
    public ResultSet loadPackageTypes(String ownerName, String packageName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "package-types", ownerName, packageName);
    }

    @Override
    public ResultSet loadAllPackageTypes(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-package-types", ownerName);
    }

    /*********************************************************
     *                        TYPES                          *
     *********************************************************/
    @Override
    public ResultSet loadTypes(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "types", ownerName);
    }

    @Override
    public ResultSet loadTypeAttributes(String ownerName, String typeName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "type-attributes", ownerName, typeName);
    }

    @Override
    public ResultSet loadAllTypeAttributes(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-type-attributes", ownerName);
    }

    @Override
    public ResultSet loadProgramTypeAttributes(String ownerName, String programName, String typeName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "program-type-attributes", ownerName, programName, typeName);
    }


    @Override
    public ResultSet loadTypeFunctions(String ownerName, String typeName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "type-functions", ownerName, typeName);
    }

    @Override
    public ResultSet loadAllTypeFunctions(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-type-functions", ownerName);
    }

    @Override
    public ResultSet loadTypeProcedures(String ownerName, String typeName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "type-procedures", ownerName, typeName);
    }

    @Override
    public ResultSet loadAllTypeProcedures(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-type-procedures", ownerName);
    }

    @Override
    public ResultSet loadProgramMethodArguments(String ownerName, String programName, String methodName, short overload, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "program-method-arguments", ownerName, programName, methodName, overload);
    }

    @Override
    public ResultSet loadMethodArguments(String ownerName, String methodName, String methodType, short overload, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "method-arguments", ownerName, methodName, methodType, overload);
    }

    @Override
    public ResultSet loadAllMethodArguments(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "all-method-arguments", ownerName);
    }


   /*********************************************************
    *                   DATABASE LINKS                      *
    *********************************************************/

    @Override
    public ResultSet loadDatabaseLinks(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "database-links", ownerName);
    }

   /*********************************************************
    *                      SEQUENCES                        *
    *********************************************************/

    @Override
    public ResultSet loadSequences(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "sequences", ownerName);
    }

    /*********************************************************
     *                       SYNONYMS                        *
     *********************************************************/

    @Override
    public ResultSet loadSynonyms(final String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "synonyms", ownerName);
    }

    /*********************************************************
     *                      CREDENTIALS                      *
     *********************************************************/
    @Override
    public ResultSet loadCredentials(final String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "credentials", ownerName);
    }

    /*********************************************************
     *                      REFERENCES                       *
     *********************************************************/
    @Override
    public ResultSet loadReferencedObjects(String ownerName, String objectName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "referenced-objects", ownerName, objectName);
    }

    @Override
    public ResultSet loadReferencingObjects(String ownerName, String objectName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "referencing-objects", ownerName, objectName);
    }

    @Override
    public ResultSet loadReferencingSchemas(String ownerName, String objectName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "referencing-schemas", ownerName, objectName);
    }


   /*********************************************************
    *                     SOURCE CODE                       *
    *********************************************************/
     @Override
     public ResultSet loadViewSourceCode(String ownerName, String viewName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "view-source-code", ownerName, viewName);
    }

    @Override
    public ResultSet loadMaterializedViewSourceCode(String ownerName, String viewName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "materialized-view-source-code", ownerName, viewName);
   }

    @Override
    public ResultSet loadDatabaseTriggerSourceCode(String ownerName, String triggerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "database-trigger-source-code", ownerName, triggerName);
    }

    @Override
    public ResultSet loadDatasetTriggerSourceCode(String tableOwner, String tableName, String ownerName, String triggerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "dataset-trigger-source-code", tableOwner, tableName, ownerName, triggerName);
    }

    @Override
    public ResultSet loadObjectSourceCode(String ownerName, String objectName, String objectType, DBNConnection connection) throws SQLException {
        return loadObjectSourceCode(ownerName, objectName, objectType, (short) 0, connection);
    }
    @Override
    public ResultSet loadObjectSourceCode(String ownerName, String objectName, String objectType, short overload, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "object-source-code", ownerName, objectName, objectType, overload);
    }

   /*********************************************************
    *                   MISCELLANEOUS                       *
    *********************************************************/

    @Override
    public ResultSet loadObjectChangeTimestamp(String ownerName, String objectName, String objectType, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "object-change-timestamp", ownerName, objectName, objectType);
    }

    @Override
    public ResultSet loadInvalidObjects(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "invalid-objects", ownerName);
    }

    @Override
    public ResultSet loadDebugObjects(String ownerName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "debug-objects", ownerName);
    }

    @Override
    public ResultSet loadCompileObjectErrors(String ownerName, String objectName, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "object-compile-errors", ownerName, objectName);
    }


    @Override
    public void compileObject(String ownerName, String objectName, String objectType, boolean debug, DBNConnection connection) throws SQLException {
        executeStatement(connection, "compile-object", ownerName, objectName, objectType, debug ? "DEBUG" : "");
    }

    @Override
    public void compileObjectBody(String ownerName, String objectName, String objectType, boolean debug, DBNConnection connection) throws SQLException {
        executeStatement(connection, "compile-object-body", ownerName, objectName, objectType, debug ? "DEBUG" : "");
    }

    @Override
    public void enableTrigger(String ownerName, String triggerName, DBNConnection connection) throws SQLException {
        executeQuery(connection, "enable-trigger", ownerName, triggerName);
    }

    @Override
    public void disableTrigger(String ownerName, String triggerName, DBNConnection connection) throws SQLException {
        executeQuery(connection, "disable-trigger", ownerName, triggerName);
    }

    @Override
    public void enableConstraint(String ownerName, String tableName, String constraintName, DBNConnection connection) throws SQLException {
        executeQuery(connection, "enable-constraint", ownerName, tableName, constraintName);
    }

    @Override
    public void disableConstraint(String ownerName, String tableName, String constraintName, DBNConnection connection) throws SQLException {
        executeQuery(connection, "disable-constraint", ownerName, tableName, constraintName);
    }

    @Override
    public ResultSet loadSessions(DBNConnection connection) throws SQLException {
        return executeQuery(connection, "sessions");
    }

    @Override
    public ResultSet loadSessionCurrentSql(Object sessionId, DBNConnection connection) throws SQLException {
        return executeQuery(connection, "session-sql", sessionId);
    }

    @Override
    public void terminateSession(Object sessionId, Object serialNumber, boolean immediate, DBNConnection connection) throws SQLException {
        String loaderId = immediate ? "kill-session-immediate" : "kill-session";
        executeStatement(connection, loaderId, sessionId, serialNumber);
    }

    @Override
    public void disconnectSession(Object sessionId, Object serialNumber, boolean postTransaction, boolean immediate, DBNConnection connection) throws SQLException {
        String loaderId =
                postTransaction ? "disconnect-session-post-transaction" :
                immediate ? "disconnect-session-immediate" : "disconnect-session";
        executeStatement(connection, loaderId, sessionId, serialNumber);
    }

    @Override
    public ResultSet loadExplainPlan(DBNConnection connection) throws SQLException {
        return executeQuery(connection, "explain-plan-result");
    }

    @Override
    public void clearExplainPlanData(DBNConnection connection) throws SQLException {
        executeUpdate(connection, "clear-explain-plan-data");
    }

    @Override
    public void enableLogger(DBNConnection connection) throws SQLException {
        executeCall(connection, null, "enable-log-output");
    }

    @Override
    public void disableLogger(DBNConnection connection) throws SQLException {
        executeCall(connection, null, "disable-log-output");
    }

    @Override
    public String readLoggerOutput(DBNConnection connection) throws SQLException {
        ExecutionLogOutput outputReader = new ExecutionLogOutput();
        executeCall(connection, outputReader, "read-log-output");
        return outputReader.getLog();
    }

    @Override
    public boolean isValid(DBNConnection connection) {
        ResultSet resultSet = null;
        try {
            if (connection == null || connection.isClosed()) return false;
            resultSet = executeQuery(connection, true, "validate-connection");
        } catch (SQLException e) {
            conditionallyLog(e);
            return false;
        } finally {
            Resources.close(resultSet);
        }
        return true;
    }

    @Override
    public boolean hasPendingTransactions(@NotNull DBNConnection connection) {
        ResultSet resultSet = null;
        try {
            resultSet = executeQuery(connection, true, "count-pending-transactions");
            resultSet.next();
            int count = resultSet.getInt("COUNT");
            return count > 0;
        } catch (SQLException e) {
            conditionallyLog(e);
            return isValid(connection);
        } finally {
            Resources.close(resultSet);
        }
    }
}
