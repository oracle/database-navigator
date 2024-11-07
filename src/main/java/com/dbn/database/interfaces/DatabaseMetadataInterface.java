package com.dbn.database.interfaces;

import com.dbn.connection.jdbc.DBNConnection;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public interface DatabaseMetadataInterface extends DatabaseInterface{

    ResultSet getDistinctValues(String ownerName, String datasetName, String columnName, DBNConnection connection) throws SQLException;

    /**
     * Load all database users
     * Column names of the returned ResultSet
     *  <li> USER_NAME (char)
     *  <li> IS_EXPIRED (Y/N)
     *  <li> IS_LOCKED (Y/N)
     */
    ResultSet loadUsers(DBNConnection connection) throws SQLException;

    /**
     * Load all database roles
     * Column names of the returned ResultSet
     *  <li> ROLE_NAME (char)
     */
    ResultSet loadRoles(DBNConnection connection) throws SQLException;

    /**
     * Load all database system privileges
     * Column names of the returned ResultSet
     *  <li> PRIVILEGE_NAME (char)
     */
    ResultSet loadSystemPrivileges(DBNConnection connection) throws SQLException;

    /**
     * Load all database object privileges
     * Column names of the returned ResultSet
     *  <li> PRIVILEGE_NAME (char)
     */
    ResultSet loadObjectPrivileges(DBNConnection connection) throws SQLException;

    /**
     * Load all user roles
     * Column names of the returned ResultSet
     *  <li> USER_NAME (char)
     *  <li> GRANTED_ROLE_NAME (char)
     *  <li> IS_ADMIN_OPTION (char)
     *  <li> IS_DEFAULT_ROLE (char)
     */
    ResultSet loadAllUserRoles(DBNConnection connection) throws SQLException;

    /**
     * Load all user privileges 
     * Column names of the returned ResultSet
     *  <li> USER_NAME (char)
     *  <li> GRANTED_PRIVILEGE_NAME (char)
     *  <li> IS_ADMIN_OPTION (char)
     */
    ResultSet loadAllUserPrivileges(DBNConnection connection) throws SQLException;


    /**
     * Load all role privileges
     * Column names of the returned ResultSet
     *  <li> ROLE_NAME (char)
     *  <li> GRANTED_PRIVILEGE_NAME (char)
     *  <li> IS_ADMIN_OPTION (char)
     * @param connection
     */
    ResultSet loadAllRolePrivileges(DBNConnection connection) throws SQLException;

    /**
     * Load all role privileges
     * Column names of the returned ResultSet
     *  <li> ROLE_NAME (char)
     *  <li> GRANTED_ROLE_NAME (char)
     *  <li> IS_ADMIN_OPTION (char)
     *  <li> IS_DEFAULT_ROLE (char)
     */
    ResultSet loadAllRoleRoles(DBNConnection connection) throws SQLException;


    /**
     * Load all database users
     * Column names of the returned ResultSet
     *  <li> SCHEMA_NAME (char)
     *  <li> IS_PUBLIC (Y/N)
     *  <li> IS_SYSTEM (Y/N)
     */
    ResultSet loadSchemas(DBNConnection connection) throws SQLException;



    /**
     * Loads available character sets for the database
     * Column names of the returned ResultSet:
     *  <li> CHARSET_NAME (char)
     *  <li> MAX_LENGTH (number)
     */
    ResultSet loadCharsets(DBNConnection connection) throws SQLException;


    /**
     * Load clusters for given owner <br>
     * Column names of the returned ResultSet
     *  <li> CLUSTER_NAME (char)
     */
    ResultSet loadClusters(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the tables of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> TABLE_NAME (char)
     *  <li> IS_TEMPORARY (Y/N)
     */
    ResultSet loadTables(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the views of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> VIEW_NAME (char)
     *  <li> IS_EDITABLE (Y/N)
     */
    ResultSet loadViews(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the java objects of the given owner<br>
     * Column names of the returned ResultSet
     * <li> OBJECT_NAME (char)
     * <li> OBJECT_KIND (char)
     * <li> OBJECT_ACCESSIBILITY (char)
     * <li> IS_FINAL (Y/N)
     * <li> IS_ABSTRACT (Y/N)
     * <li> IS_INNER (Y/N)
     * <li> IS_STATIC (Y/N)
     */
    ResultSet loadJavaObjects(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the materialized views of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> VIEW_NAME (char)
     */
    ResultSet loadMaterializedViews(String ownerName, DBNConnection connection) throws SQLException;


    /**
     * Loads the columns of the given dataset (can be a TABLE, VIEW or MATERIALIZED_VIEW)<br>
     * Column names of the returned ResultSet
     *  <li> COLUMN_NAME (char)
     *  <li> DECL_TYPE_NAME (char)
     *  <li> DECL_TYPE_OWNER (char)
     *  <li> DATA_LENGTH (number)
     *  <li> DATA_PRECISION (number)
     *  <li> DATA_SCALE (number)
     *  <li> IS_NULLABLE (Y/N)
     *  <li> IS_HIDDEN (Y/N)
     *
     */
    ResultSet loadColumns(String ownerName, String datasetName, DBNConnection connection) throws SQLException;

    /**
     * Loads the linkage actions between columns and constraints for given owner<br>
     * Column names of the returned ResultSet
     *  <li> DATASET_NAME (char)
     *  <li> COLUMN_NAME (char)
     *  <li> CONSTRAINT_NAME (char)
     *  <li> POSITION (char)
     *
     * Order by DATASET_NAME
     */
    ResultSet loadAllConstraintRelations(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the linkage actions between columns and constraints for given dataset<br>
     * Column names of the returned ResultSet
     *  <li> COLUMN_NAME (char)
     *  <li> CONSTRAINT_NAME (char)
     *  <li> POSITION (char)
     */
    ResultSet loadConstraintRelations(String ownerName, String datasetName, DBNConnection connection) throws SQLException;


    /**
     * Loads the linkage actions between columns and indexes for given schema<br>
     * Column names of the returned ResultSet
     *  <li> TABLE_NAME (char)
     *  <li> COLUMN_NAME (char)
     *  <li> INDEX_NAME (char)
     *
     * Order by TABLE_NAME
     */
    ResultSet loadAllIndexRelations(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the linkage actions between columns and indexes for given table<br>
     * Column names of the returned ResultSet
     *  <li> COLUMN_NAME (char)
     *  <li> INDEX_NAME (char)
     *
     */
    ResultSet loadIndexRelations(String ownerName, String tableName, DBNConnection connection) throws SQLException;

    /**
     * Loads the columns of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> DATASET_NAME (char)
     *  <li> COLUMN_NAME (char)
     *  <li> DECL_TYPE_NAME (char)
     *  <li> DECL_TYPE_OWNER (char)
     *  <li> DATA_LENGTH (number)
     *  <li> DATA_PRECISION (number)
     *  <li> DATA_SCALE (number)
     *  <li> IS_NULLABLE (Y/N)
     *  <li> IS_HIDDEN (Y/N)
     *
     * Sort by DATASET_NAME asc
     */
    ResultSet loadAllColumns(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the constraints of the given schema <br>
     * Column names of the returned ResultSet
     * <li> DATASET_NAME (char)
     * <li> CONSTRAINT_NAME (char)
     * <li> CONSTRAINT_TYPE (CHECK / PRIMARY KEY / FOREIGN KEY / DEFAULT / UNIQUE / VIEW CHECK / VIEW READONLY)
     * <li> CHECK_CONDITION (char - only for CHECK and VIEW CHECK constraints)
     * <li> STATUS (ENABLED/DISABLED)
     * <li> FK_CONSTRAINT_OWNER (char - only for FOREIGN KEY constraints)
     * <li> FK_CONSTRAINT_NAME (char - only for FOREIGN KEY constraints)
     *
     * Sort by DATASET_NAME asc
     */
    ResultSet loadAllConstraints(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the constraints of the given schema and dataset <br>
     * Column names of the returned ResultSet
     * <li> CONSTRAINT_NAME (char)
     * <li> CONSTRAINT_TYPE (CHECK / PRIMARY KEY / FOREIGN KEY / DEFAULT / UNIQUE / VIEW CHECK / VIEW READONLY)
     * <li> CHECK_CONDITION (char - only for CHECK and VIEW CHECK constraints)
     * <li> STATUS (ENABLED/DISABLED)
     * <li> FK_CONSTRAINT_OWNER (char - only for FOREIGN KEY constraints)
     * <li> FK_CONSTRAINT_NAME (char - only for FOREIGN KEY constraints)
     */
    ResultSet loadConstraints(String ownerName, String datasetName, DBNConnection connection) throws SQLException;

    /**
     * Loads the indexes of the given owner <br>
     * Column names of the returned ResultSet
     * <li> INDEX_NAME (char)
     * <li> TABLE_NAME (char)
     * <li> UNIQUENESS (UNIQUE / NONUNIQUE)
     *
     * Sort by TABLE_NAME
     */
    ResultSet loadAllIndexes(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the indexes of the given owner and table <br>
     * Column names of the returned ResultSet
     * <li> INDEX_NAME (char)
     * <li> UNIQUENESS (UNIQUE / NONUNIQUE)
     *
     * Sort by TABLE_NAME
     */
    ResultSet loadIndexes(String ownerName, String tableName, DBNConnection connection) throws SQLException;    

    /**
     * Loads the actions of nested-tables owned by the given table<br>
     * Column names of the returned ResultSet
     *  <li> NESTED_TABLE_NAME (char)
     *  <li> TABLE_COLUMN (char)
     *  <li> TYPE_NAME (char)
     *  <li> TYPE_OWNER (char)
     */
    ResultSet loadNestedTables(String ownerName, String tableName, DBNConnection connection) throws SQLException;

    /**
     * Loads the actions of nested-tables for the given owner<br>
     * Column names of the returned ResultSet
     *  <li> NESTED_TABLE_NAME (char)
     *  <li> TABLE_NAME (char)
     *  <li> TABLE_COLUMN (char)
     *  <li> TYPE_NAME (char)
     *  <li> TYPE_OWNER (char)
     *
     * Sort by TABLE_NAME
     */
    ResultSet loadAllNestedTables(String ownerName, DBNConnection connection) throws SQLException;


    /**
     * Loads the triggers of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> TRIGGER_NAME (char)
     *  <li> TRIGGER_TYPE (char)
     *  <li> TRIGGERING_EVENT (INSERT/DELETE/UPDATE e.g. INSERT or UPDATE)
     *  <li> IS_ENABLED (Y/N)
     *  <li> IS_VALID (Y/N)
     *  <li> IS_FOR_EACH_ROW (Y/N)
     */
    ResultSet loadDatabaseTriggers(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the triggers of the given dataset (can be a TABLE, VIEW or MATERIALIZED_VIEW)<br>
     * Column names of the returned ResultSet
     *  <li> TRIGGER_NAME (char)
     *  <li> TRIGGER_TYPE (char)
     *  <li> TRIGGERING_EVENT (INSERT/DELETE/UPDATE e.g. INSERT or UPDATE)
     *  <li> IS_ENABLED (Y/N)
     *  <li> IS_VALID (Y/N)
     *  <li> IS_FOR_EACH_ROW (Y/N)
     */
    ResultSet loadDatasetTriggers(String ownerName, String datasetName, DBNConnection connection) throws SQLException;

    /**
     * Loads the triggers of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> DATASET_NAME (char)
     *  <li> TRIGGER_NAME (char)
     *  <li> TRIGGER_TYPE (char)
     *  <li> TRIGGERING_EVENT (INSERT/DELETE/UPDATE e.g. INSERT or UPDATE)
     *  <li> IS_ENABLED (Y/N)
     *  <li> IS_VALID (Y/N)
     *  <li> IS_FOR_EACH_ROW (Y/N)
     *
     * Sort by DATASET_NAME
     */
    ResultSet loadAllDatasetTriggers(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the sequences of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> SEQUENCE_NAME (char)
     */
    ResultSet loadSequences(String ownerName, DBNConnection connection) throws SQLException;


    /**
     * Loads the synonyms of the given owner
     * Column names of the returned ResultSet
     *  <li> SYNONYM_NAME (char)
     *  <li> OBJECT_OWNER (char)
     *  <li> OBJECT_NAME (char)
     */
    ResultSet loadSynonyms(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the credentials of the given owner
     * Column names of the returned ResultSet
     *  <li> CREDENTIAL_NAME (char)
     *  <li> USER_NAME (char)
     *  <li> COMMENTS (char)
     *  <li> IS_ENABLED (Y/N)
     */
    ResultSet loadCredentials(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the credentials of the given owner
     * Column names of the returned ResultSet
     *  <li> PROFILE_NAME (char)
     *  <li> DESCRIPTION (char)
     *  <li> PROVIDER (char)
     *  <li> MODEL (char)
     *  <li> TEMPERATURE (number)
     *  <li> CREDENTIAL_NAME (char)
     *  <li> OBJECT_LIST (char)
     *  <li> IS_ENABLED (Y/N)
     */
    ResultSet loadAiProfiles(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the functions of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> FUNCTION_NAME (char)
     *  <li> IS_VALID (Y/N)
     */
    ResultSet loadFunctions(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the procedures of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> PROCEDURE_NAME (char)
     *  <li> IS_VALID (Y/N)
     */
    ResultSet loadProcedures(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the dimensions the given owner<br>
     * Column names of the returned ResultSet
     *  <li> DIMENSION_NAME (char)
     */
    ResultSet loadDimensions(String ownerName, DBNConnection connection) throws SQLException;

   /*********************************************************
    *                        PACKAGES                       *
    *********************************************************/
    /**
     * Loads the package actions of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> PACKAGE_NAME (char)
     *  <li> SPEC_STATUS ('VALID'/'INVALID')
     *  <li> BODY_STATUS ('VALID'/'INVALID')
     */
    ResultSet loadPackages(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the function for the given package<br>
     * Column names of the returned ResultSet
     *  <li> FUNCTION_NAME (char)
     *  <li> OVERLOAD (number: 0 if single instance)
     */
    ResultSet loadPackageFunctions(String ownerName, String packageName, DBNConnection connection) throws SQLException;

    /**
     * Loads all the package functions for the given owner<br>
     * Column names of the returned ResultSet
     *  <li> PACKAGE_NAME (char)
     *  <li> FUNCTION_NAME (char)
     *  <li> OVERLOAD (number: 0 if single instance)
     *
     * Sort by PACKAGE_NAME asc
     */
    ResultSet loadAllPackageFunctions(String ownerName, DBNConnection connection) throws SQLException;


    /**
     * Loads the procedures for the given package<br>
     * Column names of the returned ResultSet
     *  <li> PROCEDURE_NAME (char)
     *  <li> OVERLOAD (number: 0 if single instance)
     */
    ResultSet loadPackageProcedures(String ownerName, String packageName, DBNConnection connection) throws SQLException;

    /**
     * Loads all the package procedures for the given owner<br>
     * Column names of the returned ResultSet
     *  <li> PACKAGE_NAME (char)
     *  <li> PROCEDURE_NAME (char)
     *  <li> OVERLOAD (number: 0 if single instance)
     *
     * Sort by PACKAGE_NAME asc
     */
    ResultSet loadAllPackageProcedures(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the package types for the given owner and package<br>
     * Column names of the returned ResultSet
     *  <li> TYPE_NAME (char)
     *
     * Sort by TYPE_NAME asc
     */
    ResultSet loadPackageTypes(String ownerName, String packageName, DBNConnection connection) throws SQLException;

    /**
     * Loads all package types for the given owner<br>
     * Column names of the returned ResultSet
     *  <li> PACKAGE_NAME (char)
     *  <li> TYPE_NAME (char)
     *
     * Sort by TYPE_NAME asc
     */
    ResultSet loadAllPackageTypes(String ownerName, DBNConnection connection) throws SQLException;


   /*********************************************************
    *                        TYPES                          *
    *********************************************************/

    /**
     * Loads the user defined types (UDT) of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> TYPE_NAME (char)
     *  <li> SPEC_STATUS ('VALID'/'INVALID')
     *  <li> BODY_STATUS ('VALID'/'INVALID')
     *  <li> SUPERTYPE_OWNER (char)
     *  <li> SUPERTYPE_NAME (char)
     *  <li> TYPECODE (char)
     */
    ResultSet loadTypes(String ownerName, DBNConnection connection) throws SQLException;



    /**
     * Loads attributes of the given user defined type <br>
     * Column names of the returned ResultSet
     *  <li> ATTRIBUTE_NAME (char)
     *  <li> ATTRIBUTE_TYPE_OWNER (char)
     *  <li> ATTRIBUTE_TYPE_NAME (char)
     */
    ResultSet loadTypeAttributes(String ownerName, String typeName, DBNConnection connection) throws SQLException;

    /**
     * Loads attributes for all declared types of the given owner <br>
     * Column names of the returned ResultSet
     *  <li> TYPE_NAME (char)
     *  <li> ATTRIBUTE_NAME (char)
     *  <li> ATTRIBUTE_TYPE_OWNER (char)
     *  <li> ATTRIBUTE_TYPE_NAME (char)
     */
    ResultSet loadAllTypeAttributes(String ownerName, DBNConnection connection) throws SQLException;

    public ResultSet loadProgramTypeAttributes(String ownerName, String programName, String typeName, DBNConnection connection) throws SQLException;    

    /**
     * Loads the function for the given type<br>
     * Column names of the returned ResultSet
     *  <li> FUNCTION_NAME (char)
     *  <li> OVERLOAD (number: 0 if single instance)
     */
    ResultSet loadTypeFunctions(String ownerName, String typeName, DBNConnection connection) throws SQLException;

    /**
     * Loads all the type functions for the given owner<br>
     * Column names of the returned ResultSet
     *  <li> TYPE_NAME (char)
     *  <li> FUNCTION_NAME (char)
     *  <li> OVERLOAD (number: 0 if single instance)
     *
     * Sort by TYPE_NAME asc
     */
    ResultSet loadAllTypeFunctions(String ownerName, DBNConnection connection) throws SQLException;


    /**
     * Loads the procedures for the given type<br>
     * Column names of the returned ResultSet
     *  <li> PROCEDURE_NAME (char)
     *  <li> OVERLOAD (number: 0 if single instance)
     */
    ResultSet loadTypeProcedures(String ownerName, String typeName, DBNConnection connection) throws SQLException;

    /**
     * Loads all the type procedures for the given owner<br>
     * Column names of the returned ResultSet
     *  <li> TYPE_NAME (char)
     *  <li> PROCEDURE_NAME (char)
     *  <li> OVERLOAD (number: 0 if single instance)
     *
     * Sort by TYPE_NAME asc
     */
    ResultSet loadAllTypeProcedures(String ownerName, DBNConnection connection) throws SQLException;

    /*********************************************************
     *                     ARGUMENTS                         *
     *********************************************************/

    /**
     * Loads all arguments for a given loose method (not owned by package or type).<br>
     *  <li> ARGUMENT_NAME (char, nullable)
     *  <li> OVERLOAD (number: 0 if single instance)
     *  <li> POSITION (number)
     *  <li> SEQUENCE (number)
     *  <li> IN_OUT ('IN', 'OUT', 'IN/OUT')
     */
    ResultSet loadMethodArguments(String ownerName, String methodName, String methodType, short overload, DBNConnection connection) throws SQLException;

    /**
     * Loads all arguments for a given program method (function or procedure of a package or type).<br>
     *  <li> ARGUMENT_NAME (char, nullable)
     *  <li> OVERLOAD (number: 0 if single instance)
     *  <li> POSITION (number)
     *  <li> SEQUENCE (number)
     *  <li> IN_OUT ('IN', 'OUT', 'IN/OUT')
     */
    ResultSet loadProgramMethodArguments(String ownerName, String programName, String methodName, short overload, DBNConnection connection) throws SQLException;

    /**
     * Loads all method (function/procedure) arguments for a given owner.<br>
     * PACKAGE_NAME may stand for TYPE_NAME as well, or can be null for loose functions and procedures<br>
     *  <li> ARGUMENT_NAME (char, nullable)
     *  <li> PROGRAM_NAME (char, nullable)
     *  <li> METHOD_NAME (char)
     *  <li> OVERLOAD (number: 0 if single instance)
     *  <li> POSITION (number)
     *  <li> SEQUENCE (number)
     *  <li> IN_OUT ('IN', 'OUT', 'IN/OUT')
     */
    ResultSet loadAllMethodArguments(String ownerName, DBNConnection connection) throws SQLException;

   /*********************************************************
    *                   DATABASE LINKS                      *
    *********************************************************/

    /**
     * Loads the database links for the given owner<br>
     * Column names of the returned ResultSet
     *  <li> DBLINK_NAME (char)
     *  <li> USER_NAME (char)
     *  <li> HOST (char)
     */
    ResultSet loadDatabaseLinks(String ownerName, DBNConnection connection) throws SQLException;


    /**
     * Loads the referenced objects for the given object (objects on which it depends)
     * Column names of the returned ResultSet
     *  <li> OBJECT_OWNER (char)
     *  <li> OBJECT_NAME (char)
     */
    ResultSet loadReferencedObjects(String ownerName, String objectName, DBNConnection connection) throws SQLException;

    ResultSet loadReferencingSchemas(String ownerName, String objectName, DBNConnection connection) throws SQLException;

    /**
     * Loads the referencing objects for the given object (objects depending on it)
     * Column names of the returned ResultSet
     *  <li> OBJECT_OWNER (char)
     *  <li> OBJECT_NAME (char)
     */
    ResultSet loadReferencingObjects(String ownerName, String objectName, DBNConnection connection) throws SQLException;


    void setCurrentSchema(String schemaName, DBNConnection connection) throws SQLException;

    /**
     * Loads the source code (select statement) for the given view;
     * ResultSet should contain only one column (name is not relevant).
     * View source-code may be split on more than one line.
     */
    ResultSet loadViewSourceCode(String ownerName, String viewName, DBNConnection connection) throws SQLException;

    ResultSet loadMaterializedViewSourceCode(String ownerName, String viewName, DBNConnection connection) throws SQLException;

    ResultSet loadDatabaseTriggerSourceCode(String ownerName, String triggerName, DBNConnection connection) throws SQLException;

    ResultSet loadDatasetTriggerSourceCode(String tableOwner, String tableName, String ownerName, String triggerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the source code (select statement) for the given view;
     * ResultSet should contain only one column (name is not relevant).
     * View source-code may be split on more than one line.
     */
    ResultSet loadObjectSourceCode(String ownerName, String objectName, String objectType, DBNConnection connection) throws SQLException;
    ResultSet loadObjectSourceCode(String ownerName, String objectName, String objectType, short overload, DBNConnection connection) throws SQLException;

    /**
     * Loads a actions of invalid object names for the given owner.
     * This is used to update the status of the objects.
     *  <li> OBJECT_NAME (char)
     *  <li> OBJECT_TYPE (char)
     */
    ResultSet loadInvalidObjects(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads a actions of debug object names for the given owner.
     * This is used to update the status of the objects.
     *  <li> OBJECT_NAME (char)
     *  <li> OBJECT_TYPE (char)
     */
    ResultSet loadDebugObjects(String ownerName, DBNConnection connection) throws SQLException;

    /**
     *  Loads object errors. To be used after compiling objects to hint on syntax errors.
     *  <li> LINE (number)
     *  <li> POSITION (number)
     *  <li> TEXT (number)
     *  <li> OBJECT_TYPE (char)
     */
    ResultSet loadCompileObjectErrors(String ownerName, String objectName, DBNConnection connection) throws SQLException;

    void compileObject(String ownerName, String objectName, String objectType, boolean debug, DBNConnection connection) throws SQLException;

    void compileObjectBody(String ownerName, String objectName, String objectType, boolean debug, DBNConnection connection) throws SQLException;

    ResultSet loadObjectChangeTimestamp(String ownerName, String objectName, String objectType, DBNConnection connection) throws SQLException;

    void enableTrigger(String ownerName, String triggerName, DBNConnection connection) throws SQLException;

    void disableTrigger(String ownerName, String triggerName, DBNConnection connection) throws SQLException;

    void enableConstraint(String ownerName, String tableName, String constraintName, DBNConnection connection) throws SQLException;

    void disableConstraint(String ownerName, String tableName, String constraintName, DBNConnection connection) throws SQLException;

    ResultSet loadSessions(DBNConnection connection) throws SQLException;

    ResultSet loadSessionCurrentSql(Object sessionId, DBNConnection connection) throws SQLException;

    void terminateSession(Object sessionId, Object serialNumber, boolean immediate, DBNConnection connection) throws SQLException;

    void disconnectSession(Object sessionId, Object serialNumber, boolean postTransaction, boolean immediate, DBNConnection connection) throws SQLException;

    ResultSet loadExplainPlan(DBNConnection connection) throws SQLException;

    void clearExplainPlanData(DBNConnection connection) throws SQLException;

    void enableLogger(DBNConnection connection) throws SQLException;
    void disableLogger(DBNConnection connection) throws SQLException;
    String readLoggerOutput(DBNConnection connection) throws SQLException;

    boolean isValid(DBNConnection connection);

    String createDateString(Date date);

    boolean hasPendingTransactions(@NotNull DBNConnection connection);
}