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

package com.dbn.database.interfaces;

import com.dbn.connection.jdbc.DBNConnection;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * Loads database metadata result sets used to build and refresh the database object model.
 */
@NonNls
public interface DatabaseMetadataInterface extends DatabaseInterface {
    @Override
    default DatabaseInterfaceType getInterfaceType() {
        return DatabaseInterfaceType.METADATA;
    }

    /**
     * Loads distinct values for the given dataset column, capped at the requested count.
     * Column names of the returned ResultSet
     *  <li> VALUE (char)
     */
    ResultSet getDistinctValues(String ownerName, String datasetName, String columnName, int maxValues, DBNConnection connection) throws SQLException;

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
     * Load all connection configurations visible to the current user
     * Column names of the returned ResultSet
     *  <li> OWNER (char)
     *  <li> CONFIG_NAME (char)
     *  <li> LAST_UPDATED (char)
     */
    ResultSet loadConnectionConfigurations(DBNConnection connection) throws SQLException;

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
    ResultSet loadJavaClasses(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the java resources of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> OBJECT_NAME (char)
     *  <li> IS_VALID (Y/N)
     */
    ResultSet loadJavaResources(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the java primitive types<br>
     * Column names of the returned ResultSet
     *  <li> OBJECT_NAME (char)
     *  <li> OBJECT_KIND (char)
     *  <li> ACCESSIBILITY (char)
     *  <li> OUTER_CLASS_NAME (char)
     *  <li> IS_FINAL (Y/N)
     *  <li> IS_ABSTRACT (Y/N)
     *  <li> IS_INNER (Y/N)
     *  <li> IS_STATIC (Y/N)
     *  <li> IS_DEBUG (Y/N)
     *  <li> IS_VALID (Y/N)
     *  <li> IS_SOURCE (Y/N)
     *  <li> IS_PRIMITIVE (Y/N)
     */
    ResultSet loadJavaPrimitives(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the materialized views of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> VIEW_NAME (char)
     */
    ResultSet loadMaterializedViews(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the JSON views of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> VIEW_NAME (char)
     *  <li> VIEW_TYPE_OWNER (char)
     *  <li> VIEW_TYPE (char)
     *  <li> COMMENTS (char)
     *  <li> ROOT_TABLE_OWNER (char)
     *  <li> ROOT_TABLE_NAME (char)
     *  <li> JSON_COLUMN_NAME (char)
     *  <li> IS_SYSTEM_VIEW (Y/N)
     *  <li> IS_EDITIONABLE (Y/N)
     *  <li> IS_VALID (Y/N)
     *  <li> IS_READONLY (Y/N)
     *  <li> IS_INSERT_ALLOWED (Y/N)
     *  <li> IS_UPDATE_ALLOWED (Y/N)
     *  <li> IS_DELETE_ALLOWED (Y/N)
     *  <li> JSON_SCHEMA (char)
     *  <li> KEY_ATTRIBUTE_NAMES (char)
     */
    ResultSet loadJsonViews(String ownerName, DBNConnection connection) throws SQLException;

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
     * Loads the table relations of the given JSON view<br>
     * Column names of the returned ResultSet
     *  <li> TABLE_OWNER (char)
     *  <li> TABLE_NAME (char)
     */
    ResultSet loadJsonViewTableRelations(@NotNull String ownerName, String jsonViewName, DBNConnection connection) throws SQLException;

    /**
     * Loads all JSON view table relations of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> VIEW_NAME (char)
     *  <li> TABLE_OWNER (char)
     *  <li> TABLE_NAME (char)
     */
    ResultSet loadAllJsonViewTableRelations(@NotNull String ownerName, DBNConnection connection) throws SQLException;

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
     * Loads the AI models of the given owner
     * Column names of the returned ResultSet
     *  <li> MODEL_NAME (char)
     *  <li> MINING_FUNCTION (char)
     *  <li> ALGORITHM (char)
     *  <li> ALGORITHM_TYPE (char)
     *  <li> MODEL_SIZE (number)
     *  <li> PARTITIONED (Y/N)
     *  <li> EXTERNAL_DATA (Y/N)
     *  <li> INMEMORY (Y/N)
     */
    ResultSet loadAiModels(String ownerName, DBNConnection connection) throws SQLException;


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

    /**
     * Loads the inner java classes of the given class<br>
     * Column names of the returned ResultSet
     *  <li> OBJECT_NAME (char)
     *  <li> OBJECT_KIND (char)
     *  <li> ACCESSIBILITY (char)
     *  <li> OUTER_CLASS_NAME (char)
     *  <li> IS_FINAL (Y/N)
     *  <li> IS_ABSTRACT (Y/N)
     *  <li> IS_INNER (Y/N)
     *  <li> IS_STATIC (Y/N)
     *  <li> IS_DEBUG (Y/N)
     *  <li> IS_VALID (Y/N)
     *  <li> IS_SOURCE (Y/N)
     *  <li> IS_PRIMITIVE (Y/N)
     */
    ResultSet loadJavaInnerClasses(String ownerName, String objectName, DBNConnection connection) throws SQLException;

    /**
     * Loads all inner java classes of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> OBJECT_NAME (char)
     *  <li> OBJECT_KIND (char)
     *  <li> ACCESSIBILITY (char)
     *  <li> OUTER_CLASS_NAME (char)
     *  <li> IS_FINAL (Y/N)
     *  <li> IS_ABSTRACT (Y/N)
     *  <li> IS_INNER (Y/N)
     *  <li> IS_STATIC (Y/N)
     *  <li> IS_DEBUG (Y/N)
     *  <li> IS_VALID (Y/N)
     *  <li> IS_SOURCE (Y/N)
     *  <li> IS_PRIMITIVE (Y/N)
     */
    ResultSet loadAllJavaInnerClasses(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the java fields of the given class<br>
     * Column names of the returned ResultSet
     *  <li> OWNER_CLASS_NAME (char)
     *  <li> FIELD_INDEX (number)
     *  <li> FIELD_NAME (char)
     *  <li> ACCESSIBILITY (char)
     *  <li> IS_FINAL (Y/N)
     *  <li> IS_STATIC (Y/N)
     *  <li> ARRAY_DEPTH (number)
     *  <li> FIELD_CLASS_NAME (char)
     */
    ResultSet loadJavaFields(String ownerName, String objectName, DBNConnection connection) throws SQLException;

    /**
     * Loads all java fields of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> OWNER_CLASS_NAME (char)
     *  <li> FIELD_INDEX (number)
     *  <li> FIELD_NAME (char)
     *  <li> ACCESSIBILITY (char)
     *  <li> IS_FINAL (Y/N)
     *  <li> IS_STATIC (Y/N)
     *  <li> ARRAY_DEPTH (number)
     *  <li> FIELD_CLASS_NAME (char)
     */
    ResultSet loadAllJavaFields(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the java methods of the given class<br>
     * Column names of the returned ResultSet
     *  <li> OWNER_CLASS_NAME (char)
     *  <li> METHOD_NAME (char)
     *  <li> METHOD_INDEX (number)
     *  <li> ACCESSIBILITY (char)
     *  <li> ARRAY_DEPTH (number)
     *  <li> RETURN_CLASS_NAME (char)
     *  <li> IS_STATIC (Y/N)
     *  <li> IS_FINAL (Y/N)
     *  <li> IS_ABSTRACT (Y/N)
     *  <li> METHOD_SIGNATURE (char)
     */
    ResultSet loadJavaMethods(String ownerName, String objectName, DBNConnection connection) throws SQLException;

    /**
     * Loads all java methods of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> OWNER_CLASS_NAME (char)
     *  <li> METHOD_NAME (char)
     *  <li> METHOD_INDEX (number)
     *  <li> ACCESSIBILITY (char)
     *  <li> ARRAY_DEPTH (number)
     *  <li> RETURN_CLASS_NAME (char)
     *  <li> IS_STATIC (Y/N)
     *  <li> IS_FINAL (Y/N)
     *  <li> IS_ABSTRACT (Y/N)
     *  <li> METHOD_SIGNATURE (char)
     */
    ResultSet loadAllJavaMethods(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the java parameters of the given method<br>
     * Column names of the returned ResultSet
     *  <li> CLASS_NAME (char)
     *  <li> METHOD_NAME (char)
     *  <li> METHOD_INDEX (number)
     *  <li> ARGUMENT_POSITION (number)
     *  <li> ARRAY_DEPTH (number)
     *  <li> ARGUMENT_CLASS_NAME (char)
     */
    ResultSet loadJavaParameters(String ownerName, String objectName, String methodName, int methodIndex, DBNConnection connection) throws SQLException;

    /**
     * Loads all java parameters of the given owner<br>
     * Column names of the returned ResultSet
     *  <li> CLASS_NAME (char)
     *  <li> METHOD_NAME (char)
     *  <li> METHOD_INDEX (number)
     *  <li> ARGUMENT_POSITION (number)
     *  <li> ARRAY_DEPTH (number)
     *  <li> ARGUMENT_CLASS_NAME (char)
     */
    ResultSet loadAllJavaParameters(String ownerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the java class dependencies of the given class<br>
     * Column names of the returned ResultSet
     *  <li> OBJECT_OWNER (char)
     *  <li> OBJECT_NAME (char)
     *  <li> HAS_SOURCE (Y/N)
     */
    ResultSet loadJavaClassDependencies(String ownerName, String objectName, DBNConnection connection) throws SQLException;

    /**
     * Loads the database java version
     */
    String loadJavaVersion(DBNConnection connection) throws SQLException;


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

    /**
     * Loads the type attributes of the given program type<br>
     * Column names of the returned ResultSet
     *  <li> ATTRIBUTE_NAME (char)
     *  <li> ATTRIBUTE_TYPE_OWNER (char)
     *  <li> ATTRIBUTE_TYPE_NAME (char)
     */
    ResultSet loadProgramTypeAttributes(String ownerName, String programName, String typeName, DBNConnection connection) throws SQLException;

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

    /**
     * Loads the referencing schemas for the given object (schemas containing objects depending on it)
     * Column names of the returned ResultSet
     *  <li> OBJECT_OWNER (char)
     */
    ResultSet loadReferencingSchemas(String ownerName, String objectName, DBNConnection connection) throws SQLException;

    /**
     * Loads the referencing objects for the given object (objects depending on it)
     * Column names of the returned ResultSet
     *  <li> OBJECT_OWNER (char)
     *  <li> OBJECT_NAME (char)
     */
    ResultSet loadReferencingObjects(String ownerName, String objectName, DBNConnection connection) throws SQLException;


    /**
     * Sets the current schema for the given connection.
     */
    void setCurrentSchema(String schemaName, DBNConnection connection) throws SQLException;

    /**
     * Loads the source code (select statement) for the given view;
     * ResultSet should contain only one column (name is not relevant).
     * View source-code may be split on more than one line.
     */
    ResultSet loadViewSourceCode(String ownerName, String viewName, DBNConnection connection) throws SQLException;

    /**
     * Loads the source code (select statement) for the given materialized view;
     * ResultSet should contain only one column (name is not relevant).
     * Materialized view source-code may be split on more than one line.
     */
    ResultSet loadMaterializedViewSourceCode(String ownerName, String viewName, DBNConnection connection) throws SQLException;

    /**
     * Loads the source code for the given database trigger;
     * ResultSet should contain only one column (name is not relevant).
     * Trigger source-code may be split on more than one line.
     */
    ResultSet loadDatabaseTriggerSourceCode(String ownerName, String triggerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the source code for the given dataset trigger;
     * ResultSet should contain only one column (name is not relevant).
     * Trigger source-code may be split on more than one line.
     */
    ResultSet loadDatasetTriggerSourceCode(String tableOwner, String tableName, String ownerName, String triggerName, DBNConnection connection) throws SQLException;

    /**
     * Loads the source code (select statement) for the given view;
     * ResultSet should contain only one column (name is not relevant).
     * View source-code may be split on more than one line.
     */
    ResultSet loadObjectSourceCode(String ownerName, String objectName, String objectType, DBNConnection connection) throws SQLException;

    /**
     * Loads the source code for the given overloaded object;
     * ResultSet should contain only one column (name is not relevant).
     * Object source-code may be split on more than one line.
     */
    ResultSet loadObjectSourceCode(String ownerName, String objectName, String objectType, short overload, DBNConnection connection) throws SQLException;

    /**
     * Loads the source code for the given java resource.
     */
    String loadJavaResourceSourceCode(String ownerName, String objectName, DBNConnection connection) throws SQLException;

    /**
     * Loads the binary code for the given java class.
     */
    byte[] loadJavaBinaryCode(String ownerName, String objectName, DBNConnection connection) throws SQLException;

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

    /**
     * Loads the change timestamp for the given object.
     * ResultSet should contain only one column (name is not relevant).
     */
    ResultSet loadObjectChangeTimestamp(String ownerName, String objectName, String objectType, DBNConnection connection) throws SQLException;

    /**
     * Enables the given trigger.
     */
    void enableTrigger(String ownerName, String triggerName, DBNConnection connection) throws SQLException;

    /**
     * Disables the given trigger.
     */
    void disableTrigger(String ownerName, String triggerName, DBNConnection connection) throws SQLException;

    /**
     * Enables the given constraint.
     */
    void enableConstraint(String ownerName, String tableName, String constraintName, DBNConnection connection) throws SQLException;

    /**
     * Disables the given constraint.
     */
    void disableConstraint(String ownerName, String tableName, String constraintName, DBNConnection connection) throws SQLException;

    /**
     * Loads data event registrations.
     */
    ResultSet loadDataEventRegistrations(DBNConnection connection) throws SQLException;

    /**
     * Loads database sessions.
     */
    ResultSet loadSessions(DBNConnection connection) throws SQLException;

    /**
     * Loads the current SQL for the given session.
     */
    ResultSet loadSessionCurrentSql(Object sessionId, DBNConnection connection) throws SQLException;

    /**
     * Terminates the given database session.
     */
    void terminateSession(Object sessionId, Object serialNumber, boolean immediate, DBNConnection connection) throws SQLException;

    /**
     * Disconnects the given database session.
     */
    void disconnectSession(Object sessionId, Object serialNumber, boolean postTransaction, boolean immediate, DBNConnection connection) throws SQLException;

    /**
     * Loads the current explain plan data.
     */
    ResultSet loadExplainPlan(DBNConnection connection) throws SQLException;

    /**
     * Clears the current explain plan data.
     */
    void clearExplainPlanData(DBNConnection connection) throws SQLException;

    /**
     * Enables database log output collection.
     */
    void enableLogger(DBNConnection connection) throws SQLException;

    /**
     * Disables database log output collection.
     */
    void disableLogger(DBNConnection connection) throws SQLException;

    /**
     * Reads collected database log output.
     */
    String readLoggerOutput(DBNConnection connection) throws SQLException;

    /**
     * Checks if the given connection is valid.
     */
    boolean isValid(DBNConnection connection);

    /**
     * Loads the current database session user.
     */
    String loadSessionUser(DBNConnection connection) throws SQLException;

    /**
     * Creates a database date literal for the given date.
     */
    String createDateString(Date date);

    /**
     * Checks if the given connection has pending transactions.
     */
    boolean hasPendingTransactions(@NotNull DBNConnection connection);

    /**
     * Checks if the current user has the given system privilege.
     */
    boolean hasSystemPrivilege(String privilegeName, DBNConnection connection) throws SQLException;

    /**
     * Checks if the current user has the given object privilege.
     */
    boolean hasObjectPrivilege(String privilegeName, String ownerName, String objectName, DBNConnection connection) throws SQLException;

    /**
     * Checks if the current user has tablespace quota.
     */
    boolean hasTablespaceQuota(DBNConnection connection) throws SQLException;

    /**
     * Checks if the given user has the given network privilege.
     */
    boolean hasNetworkPrivilege(String userName, String hostName, String privilegeName, DBNConnection connection) throws SQLException;

    /**
     * Grants the given network privilege to the given user.
     */
    void grantNetworkPrivilege(String userName, String hostName, String privilegeName, DBNConnection connection) throws SQLException;
}
