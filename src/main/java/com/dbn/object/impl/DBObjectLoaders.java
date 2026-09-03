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

package com.dbn.object.impl;

import com.dbn.common.content.DynamicContent;
import com.dbn.common.content.GroupedDynamicContent;
import com.dbn.common.content.loader.DynamicContentLoaderImpl;
import com.dbn.common.content.loader.DynamicContentResultSetLoader;
import com.dbn.common.content.loader.DynamicSubcontentLoader;
import com.dbn.common.exception.ElementSkippedException;
import com.dbn.database.common.metadata.DBObjectMetadata;
import com.dbn.database.common.metadata.def.DBAIProfileMetadata;
import com.dbn.database.common.metadata.def.DBArgumentMetadata;
import com.dbn.database.common.metadata.def.DBCharsetMetadata;
import com.dbn.database.common.metadata.def.DBClusterMetadata;
import com.dbn.database.common.metadata.def.DBColumnColumnMetadata;
import com.dbn.database.common.metadata.def.DBColumnMetadata;
import com.dbn.database.common.metadata.def.DBConstraintColumnMetadata;
import com.dbn.database.common.metadata.def.DBConstraintMetadata;
import com.dbn.database.common.metadata.def.DBCredentialMetadata;
import com.dbn.database.common.metadata.def.DBDatabaseLinkMetadata;
import com.dbn.database.common.metadata.def.DBDatasourceConfigMetadata;
import com.dbn.database.common.metadata.def.DBDimensionMetadata;
import com.dbn.database.common.metadata.def.DBFunctionMetadata;
import com.dbn.database.common.metadata.def.DBGrantedPrivilegeMetadata;
import com.dbn.database.common.metadata.def.DBGrantedRoleMetadata;
import com.dbn.database.common.metadata.def.DBIndexColumnMetadata;
import com.dbn.database.common.metadata.def.DBIndexMetadata;
import com.dbn.database.common.metadata.def.DBJavaClassMetadata;
import com.dbn.database.common.metadata.def.DBJavaFieldMetadata;
import com.dbn.database.common.metadata.def.DBJavaMethodMetadata;
import com.dbn.database.common.metadata.def.DBJavaParameterMetadata;
import com.dbn.database.common.metadata.def.DBJavaResourceMetadata;
import com.dbn.database.common.metadata.def.DBJsonViewMetadata;
import com.dbn.database.common.metadata.def.DBJsonViewTableMetadata;
import com.dbn.database.common.metadata.def.DBMaterializedViewMetadata;
import com.dbn.database.common.metadata.def.DBMiningModelMetadata;
import com.dbn.database.common.metadata.def.DBNestedTableMetadata;
import com.dbn.database.common.metadata.def.DBObjectDependencyMetadata;
import com.dbn.database.common.metadata.def.DBPackageMetadata;
import com.dbn.database.common.metadata.def.DBPrivilegeMetadata;
import com.dbn.database.common.metadata.def.DBProcedureMetadata;
import com.dbn.database.common.metadata.def.DBRoleMetadata;
import com.dbn.database.common.metadata.def.DBSchemaMetadata;
import com.dbn.database.common.metadata.def.DBSequenceMetadata;
import com.dbn.database.common.metadata.def.DBSynonymMetadata;
import com.dbn.database.common.metadata.def.DBTableMetadata;
import com.dbn.database.common.metadata.def.DBTriggerMetadata;
import com.dbn.database.common.metadata.def.DBTypeAttributeMetadata;
import com.dbn.database.common.metadata.def.DBTypeMetadata;
import com.dbn.database.common.metadata.def.DBUserMetadata;
import com.dbn.database.common.metadata.def.DBViewMetadata;
import com.dbn.object.DBArgument;
import com.dbn.object.DBCharset;
import com.dbn.object.DBCluster;
import com.dbn.object.DBColumn;
import com.dbn.object.DBConsole;
import com.dbn.object.DBConstraint;
import com.dbn.object.DBCredential;
import com.dbn.object.DBDatabaseLink;
import com.dbn.object.DBDatabaseTrigger;
import com.dbn.object.DBDataset;
import com.dbn.object.DBDatasetTrigger;
import com.dbn.object.DBDatasourceConfig;
import com.dbn.object.DBDimension;
import com.dbn.object.DBFunction;
import com.dbn.object.DBGrantedPrivilege;
import com.dbn.object.DBGrantedRole;
import com.dbn.object.DBIndex;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.DBJavaResource;
import com.dbn.object.DBJsonView;
import com.dbn.object.DBMaterializedView;
import com.dbn.object.DBMethod;
import com.dbn.object.DBNestedTable;
import com.dbn.object.DBObjectPrivilege;
import com.dbn.object.DBPackage;
import com.dbn.object.DBPackageFunction;
import com.dbn.object.DBPackageProcedure;
import com.dbn.object.DBPackageType;
import com.dbn.object.DBPrivilege;
import com.dbn.object.DBProcedure;
import com.dbn.object.DBProgram;
import com.dbn.object.DBRole;
import com.dbn.object.DBSchema;
import com.dbn.object.DBSequence;
import com.dbn.object.DBSynonym;
import com.dbn.object.DBSystemPrivilege;
import com.dbn.object.DBTable;
import com.dbn.object.DBType;
import com.dbn.object.DBTypeAttribute;
import com.dbn.object.DBTypeFunction;
import com.dbn.object.DBTypeProcedure;
import com.dbn.object.DBUser;
import com.dbn.object.DBView;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.list.loader.DBObjectListFromRelationListLoader;
import com.dbn.object.type.DBObjectType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.content.DynamicContentProperty.MASTER;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.object.type.DBObjectRelationType.COLUMN_COLUMN;
import static com.dbn.object.type.DBObjectRelationType.CONSTRAINT_COLUMN;
import static com.dbn.object.type.DBObjectRelationType.INDEX_COLUMN;
import static com.dbn.object.type.DBObjectRelationType.JSON_VIEW_TABLE;
import static com.dbn.object.type.DBObjectRelationType.ROLE_PRIVILEGE;
import static com.dbn.object.type.DBObjectRelationType.ROLE_ROLE;
import static com.dbn.object.type.DBObjectRelationType.USER_PRIVILEGE;
import static com.dbn.object.type.DBObjectRelationType.USER_ROLE;
import static com.dbn.object.type.DBObjectType.AI_PROFILE;
import static com.dbn.object.type.DBObjectType.ARGUMENT;
import static com.dbn.object.type.DBObjectType.CHARSET;
import static com.dbn.object.type.DBObjectType.CLUSTER;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.CONSOLE;
import static com.dbn.object.type.DBObjectType.CONSTRAINT;
import static com.dbn.object.type.DBObjectType.CREDENTIAL;
import static com.dbn.object.type.DBObjectType.DATABASE_TRIGGER;
import static com.dbn.object.type.DBObjectType.DATASET;
import static com.dbn.object.type.DBObjectType.DATASET_TRIGGER;
import static com.dbn.object.type.DBObjectType.DATASOURCE_CONFIG;
import static com.dbn.object.type.DBObjectType.DBLINK;
import static com.dbn.object.type.DBObjectType.DIMENSION;
import static com.dbn.object.type.DBObjectType.FUNCTION;
import static com.dbn.object.type.DBObjectType.GRANTED_PRIVILEGE;
import static com.dbn.object.type.DBObjectType.GRANTED_ROLE;
import static com.dbn.object.type.DBObjectType.INCOMING_DEPENDENCY;
import static com.dbn.object.type.DBObjectType.INDEX;
import static com.dbn.object.type.DBObjectType.JAVA_CLASS;
import static com.dbn.object.type.DBObjectType.JAVA_FIELD;
import static com.dbn.object.type.DBObjectType.JAVA_INNER_CLASS;
import static com.dbn.object.type.DBObjectType.JAVA_METHOD;
import static com.dbn.object.type.DBObjectType.JAVA_PARAMETER;
import static com.dbn.object.type.DBObjectType.JAVA_PRIMITIVE;
import static com.dbn.object.type.DBObjectType.JAVA_RESOURCE;
import static com.dbn.object.type.DBObjectType.JSON_VIEW;
import static com.dbn.object.type.DBObjectType.MATERIALIZED_VIEW;
import static com.dbn.object.type.DBObjectType.METHOD;
import static com.dbn.object.type.DBObjectType.NESTED_TABLE;
import static com.dbn.object.type.DBObjectType.OBJECT_PRIVILEGE;
import static com.dbn.object.type.DBObjectType.OUTGOING_DEPENDENCY;
import static com.dbn.object.type.DBObjectType.PACKAGE;
import static com.dbn.object.type.DBObjectType.PACKAGE_BODY;
import static com.dbn.object.type.DBObjectType.PACKAGE_FUNCTION;
import static com.dbn.object.type.DBObjectType.PACKAGE_PROCEDURE;
import static com.dbn.object.type.DBObjectType.PACKAGE_TYPE;
import static com.dbn.object.type.DBObjectType.PRIVILEGE;
import static com.dbn.object.type.DBObjectType.PROCEDURE;
import static com.dbn.object.type.DBObjectType.ROLE;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static com.dbn.object.type.DBObjectType.SEQUENCE;
import static com.dbn.object.type.DBObjectType.SYNONYM;
import static com.dbn.object.type.DBObjectType.SYSTEM_PRIVILEGE;
import static com.dbn.object.type.DBObjectType.TABLE;
import static com.dbn.object.type.DBObjectType.TYPE;
import static com.dbn.object.type.DBObjectType.TYPE_ATTRIBUTE;
import static com.dbn.object.type.DBObjectType.TYPE_BODY;
import static com.dbn.object.type.DBObjectType.TYPE_FUNCTION;
import static com.dbn.object.type.DBObjectType.TYPE_PROCEDURE;
import static com.dbn.object.type.DBObjectType.USER;
import static com.dbn.object.type.DBObjectType.VIEW;
import static com.dbn.object.type.DBObjectType.get;

@UtilityClass
public class DBObjectLoaders {
    public static void initLoaders() {}

    /* Loaders for root objects (children of DBObjectBundle) */
    static {
        DynamicContentLoaderImpl.<DBConsole, DBObjectMetadata>create(
                "CONSOLES", null, CONSOLE, true,
                content -> content.setElements(content.getConnection().getConsoleBundle().getConsoles()));


        DynamicContentResultSetLoader.<DBSchema, DBSchemaMetadata>create(
                "SCHEMAS", null, SCHEMA, true, true,
                (content, conn, mdi) -> mdi.loadSchemas(conn),
                (content, cache, md) -> new DBSchemaImpl(content.getConnection(), cast(md)));

        DynamicContentResultSetLoader.<DBUser, DBUserMetadata>create(
                "USERS", null, USER, true, true,
                (content, conn, mdi) -> mdi.loadUsers(conn),
                (content, cache, md) -> new DBUserImpl(content.getConnection(), md));

        DynamicContentResultSetLoader.<DBRole, DBRoleMetadata>create(
                "ROLES", null, ROLE, true, true,
                (content, conn, mdi) -> mdi.loadRoles(conn),
                (content, cache, md) -> new DBRoleImpl(content.getConnection(), cast(md)));

        DynamicContentResultSetLoader.<DBSystemPrivilege, DBPrivilegeMetadata>create(
                "SYSTEM_PRIVILEGES", null, SYSTEM_PRIVILEGE, true, true,
                (content, conn, mdi) -> mdi.loadSystemPrivileges(conn),
                (content, cache, md) -> new DBSystemPrivilegeImpl(content.getConnection(), md));

        DynamicContentResultSetLoader.<DBObjectPrivilege, DBPrivilegeMetadata>create(
                "OBJECT_PRIVILEGES", null, OBJECT_PRIVILEGE, true, true,
                (content, conn, mdi) -> mdi.loadObjectPrivileges(conn),
                (content, cache, md) -> new DBObjectPrivilegeImpl(content.getConnection(), md));

        DynamicContentResultSetLoader.<DBCharset, DBCharsetMetadata>create(
                "CHARSETS", null, CHARSET, true, true,
                (content, conn, mdi) -> mdi.loadCharsets(conn),
                (content, cache, md) -> new DBCharsetImpl(content.getConnection(), md));

        DynamicContentResultSetLoader.<DBUserRoleRelation, DBGrantedRoleMetadata>create(
                "USER_ROLES", null, USER_ROLE, true, true,
                (content, conn, mdi) -> mdi.loadAllUserRoles(conn),
                (content, cache, md) -> {
                    DBObjectBundle objects = content.ensureParentEntity();
                    DBUser user = valid(objects.getUser(md.getUserName()));
                    DBGrantedRole role = new DBGrantedRoleImpl(user, md);
                    return new DBUserRoleRelation(user, role);
                });

        DynamicContentResultSetLoader.<DBUserPrivilegeRelation, DBGrantedPrivilegeMetadata>create(
                "USER_PRIVILEGES", null, USER_PRIVILEGE, true, true,
                (content, conn, mdi) -> mdi.loadAllUserPrivileges(conn),
                (content, cache, md) -> {
                    DBObjectBundle objects = content.ensureParentEntity();
                    DBUser user = valid(objects.getUser(md.getUserName()));
                    DBGrantedPrivilege privilege = new DBGrantedPrivilegeImpl(user, md);
                    return new DBUserPrivilegeRelation(user, privilege);
                });

        DynamicContentResultSetLoader.<DBRoleRoleRelation, DBGrantedRoleMetadata>create(
                "ROLE_ROLES", null, ROLE_ROLE, true, true,
                (content, conn, mdi) -> mdi.loadAllRoleRoles(conn),
                (content, cache, md) -> {
                    DBObjectBundle objects = content.ensureParentEntity();
                    DBRole role = valid(objects.getRole(md.getRoleName()));
                    DBGrantedRole grantedRole = new DBGrantedRoleImpl(role, md);
                    return new DBRoleRoleRelation(role, grantedRole);
                });

        DynamicContentResultSetLoader.<DBRolePrivilegeRelation, DBGrantedPrivilegeMetadata>create(
                "ROLE_PRIVILEGES", null, ROLE_PRIVILEGE, true, true,
                (content, conn, mdi) -> mdi.loadAllRolePrivileges(conn),
                (content, cache, md) -> {
                    DBObjectBundle objects = content.ensureParentEntity();
                    DBRole role = valid(objects.getRole(md.getRoleName()));
                    DBGrantedPrivilege privilege = new DBGrantedPrivilegeImpl(role, md);
                    return new DBRolePrivilegeRelation(role, privilege);
                });
    }

    /* Loaders for acl objects (DBUser / DBRole / DBPrivilege) */
    static {
        DynamicContentLoaderImpl.<DBUser, DBObjectMetadata>create(
                "PRIVILEGE_USERS", PRIVILEGE, USER, true,
                content -> {
                    DBPrivilege privilege = content.ensureParentEntity();
                    List<DBUser> users = nd(privilege.getObjectBundle().getUsers());

                    List<DBUser> grantees = new ArrayList<>();
                    for (DBUser user : users) {
                        if (user.hasPrivilege(privilege)) {
                            grantees.add(user);
                        }
                    }
                    content.setElements(grantees);
                    content.set(MASTER, false);
                });

        DynamicContentLoaderImpl.<DBRole, DBObjectMetadata>create(
                "PRIVILEGE_ROLES", PRIVILEGE, ROLE, true, content -> {
                    DBPrivilege privilege = content.ensureParentEntity();
                    List<DBRole> roles = nd(privilege.getObjectBundle().getRoles());

                    List<DBRole> grantees = new ArrayList<>();
                    for (DBRole role : roles) {
                        if (role.hasPrivilege(privilege)) grantees.add(role);
                    }
                    content.setElements(grantees);
                    content.set(MASTER, false);
                });
    }

    /* Loaders for schema objects (children of DBSchema) */
    static {
        DynamicContentResultSetLoader.<DBTable, DBTableMetadata>create(
                "TABLES", SCHEMA, TABLE, true, true,
                (content, conn, mdi) -> mdi.loadTables(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBTableImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBView, DBViewMetadata>create(
                "VIEWS", SCHEMA, VIEW, true, true,
                (content, conn, mdi) -> mdi.loadViews(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBViewImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBJsonView, DBJsonViewMetadata>create(
                "JSON_VIEWS", SCHEMA, JSON_VIEW, true, true,
                (content, conn, mdi) -> mdi.loadJsonViews(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBJsonViewImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBMaterializedView, DBMaterializedViewMetadata>create(
                "MATERIALIZED_VIEWS", SCHEMA, MATERIALIZED_VIEW, true, true,
                (content, conn, mdi) -> mdi.loadMaterializedViews(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBMaterializedViewImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBSynonym, DBSynonymMetadata>create(
                "SYNONYMS", SCHEMA, SYNONYM, true, true,
                (content, conn, mdi) -> mdi.loadSynonyms(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBSynonymImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBSequence, DBSequenceMetadata>create(
                "SEQUENCES", SCHEMA, SEQUENCE, true, true,
                (content, conn, mdi) -> mdi.loadSequences(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBSequenceImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBProcedure, DBProcedureMetadata>create(
                "PROCEDURES", SCHEMA, PROCEDURE, true, true,
                (content, conn, mdi) -> mdi.loadProcedures(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBProcedureImpl((DBSchema) content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBFunction, DBFunctionMetadata>create(
                "FUNCTIONS", SCHEMA, FUNCTION, true, true,
                (content, conn, mdi) -> mdi.loadFunctions(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBFunctionImpl((DBSchema) content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBPackage, DBPackageMetadata>create(
                "PACKAGES", SCHEMA, PACKAGE, true, true,
                (content, conn, mdi) -> mdi.loadPackages(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBPackageImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBType, DBTypeMetadata>create(
                "TYPES", SCHEMA, TYPE, true, true,
                (content, conn, mdi) -> mdi.loadTypes(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBTypeImpl((DBSchema) content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBDatabaseTrigger, DBTriggerMetadata>create(
                "DATABASE_TRIGGERS", SCHEMA, DATABASE_TRIGGER, true, true,
                (content, conn, mdi) -> mdi.loadDatabaseTriggers(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBDatabaseTriggerImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBJavaClass, DBJavaClassMetadata>create(
                "JAVA_PRIMITIVES", SCHEMA, JAVA_PRIMITIVE, true, true,
                (content, conn, mdi) -> mdi.loadJavaPrimitives(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBJavaClassImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBJavaClass, DBJavaClassMetadata>create(
                "JAVA_CLASSES", SCHEMA, JAVA_CLASS, true, true,
                (content, conn, mdi) -> mdi.loadJavaClasses(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBJavaClassImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBJavaResource, DBJavaResourceMetadata>create(
                "JAVA_RESOURCES", SCHEMA, JAVA_RESOURCE, true, true,
                (content, conn, mdi) -> mdi.loadJavaResources(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBJavaResourceImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBDimension, DBDimensionMetadata>create(
                "DIMENSIONS", SCHEMA, DIMENSION, true, true,
                (content, conn, mdi) -> mdi.loadDimensions(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBDimensionImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBCluster, DBClusterMetadata>create(
                "CLUSTERS", SCHEMA, CLUSTER, true, true,
                (content, conn, mdi) -> mdi.loadClusters(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBClusterImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBCredential, DBCredentialMetadata>create(
                "CREDENTIALS", SCHEMA, CREDENTIAL, true, true,
                (content, conn, mdi) -> mdi.loadCredentials(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBCredentialImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBDatasourceConfig, DBDatasourceConfigMetadata>create(
                "DATASOURCE_CONFIGS", SCHEMA, DATASOURCE_CONFIG, true, true,
                (content, conn, mdi) -> mdi.loadDatasourceConfigs(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBDatasourceConfigImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBAIProfileImpl, DBAIProfileMetadata>create(
                "AI_PROFILES", SCHEMA, AI_PROFILE, true, true,
                (content, conn, mdi) -> mdi.loadAiProfiles(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBAIProfileImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBMiningModelImpl, DBMiningModelMetadata>create(
                "MINING_MODELS", DBObjectType.SCHEMA, DBObjectType.MINING_MODEL, true, true,
                (content, conn, mdi) -> mdi.loadAiModels(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBMiningModelImpl(content.getParentEntity(), md));
        DynamicContentResultSetLoader.<DBDatabaseLink, DBDatabaseLinkMetadata>create(
                "DBLINKS", SCHEMA, DBLINK, true, true,
                (content, conn, mdi) -> mdi.loadDatabaseLinks(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBDatabaseLinkImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBColumn, DBColumnMetadata>create(
                "ALL_COLUMNS", SCHEMA, COLUMN, true, true,
                (content, conn, mdi) -> mdi.loadAllColumns(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String datasetName = md.getDatasetName();
                    DBDataset dataset = valid(cache.get(datasetName, () -> ((DBSchema) content.ensureParentEntity()).getDataset(datasetName)));
                    return new DBColumnImpl(dataset, md);
                });

        DynamicContentResultSetLoader.<DBConstraint, DBConstraintMetadata>create(
                "ALL_CONSTRAINTS", SCHEMA, CONSTRAINT, true, true,
                (content, conn, mdi) -> mdi.loadAllConstraints(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String datasetName = md.getDatasetName();
                    DBDataset dataset = valid(cache.get(datasetName, () -> ((DBSchema) content.ensureParentEntity()).getDataset(datasetName)));
                    return new DBConstraintImpl(dataset, md);
                });

        DynamicContentResultSetLoader.<DBIndex, DBIndexMetadata>create(
                "ALL_INDEXES", SCHEMA, INDEX, true, true,
                (content, conn, mdi) -> mdi.loadAllIndexes(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String datasetName = md.getTableName();
                    DBDataset dataset = valid(cache.get(datasetName, () -> ((DBSchema) content.ensureParentEntity()).getDataset(datasetName)));
                    return new DBIndexImpl(dataset, md);
                });

        DynamicContentResultSetLoader.<DBDatasetTrigger, DBTriggerMetadata>create(
                "ALL_DATASET_TRIGGERS", SCHEMA, DATASET_TRIGGER, true, true,
                (content, conn, mdi) -> mdi.loadAllDatasetTriggers(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String datasetName = md.getDatasetName();
                    DBDataset dataset = valid(cache.get(datasetName, () -> ((DBSchema) content.ensureParentEntity()).getDataset(datasetName)));
                    return new DBDatasetTriggerImpl(dataset, md);
                });

        DynamicContentResultSetLoader.<DBNestedTable, DBNestedTableMetadata>create(
                "ALL_NESTED_TABLES", SCHEMA, NESTED_TABLE, true, true,
                (content, conn, mdi) -> mdi.loadAllNestedTables(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String tableName = md.getTableName();
                    DBTable table = valid(cache.get(tableName, () -> ((DBSchema) content.ensureParentEntity()).getTable(tableName)));
                    return new DBNestedTableImpl(table, md);
                });

        DynamicContentResultSetLoader.<DBJavaClass, DBJavaClassMetadata>create(
                "ALL_JAVA_INNER_CLASSES", SCHEMA, JAVA_INNER_CLASS, true, true,
                (content, conn, mdi) -> mdi.loadAllJavaInnerClasses(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String className = md.getOuterClassName();
                    return new DBJavaClassImpl(content.getParentEntity(), md);
                });

        DynamicContentResultSetLoader.<DBJavaField, DBJavaFieldMetadata>create(
                "ALL_JAVA_FIELDS", SCHEMA, JAVA_FIELD, true, true,
                (content, conn, mdi) -> mdi.loadAllJavaFields(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String className = md.getOwnerClassName();
                    DBJavaClass javaClass = valid(cache.get(className, () -> ((DBSchema) content.ensureParentEntity()).getJavaClass(className)));
                    return new DBJavaFieldImpl(javaClass, md);
                });

        DynamicContentResultSetLoader.<DBJavaMethod, DBJavaMethodMetadata>create(
                "ALL_JAVA_METHODS", SCHEMA, JAVA_METHOD, true, true,
                (content, conn, mdi) -> mdi.loadAllJavaMethods(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String className = md.getOwnerClassName();
                    DBJavaClass javaClass = valid(cache.get(className, () -> ((DBSchema) content.ensureParentEntity()).getJavaClass(className)));
                    return new DBJavaMethodImpl(javaClass, md);
                });

        DynamicContentResultSetLoader.<DBJavaParameter, DBJavaParameterMetadata>create(
                "ALL_JAVA_METHOD_PARAMETERS", SCHEMA, JAVA_PARAMETER, true, true,
                (content, conn, mdi) -> mdi.loadAllJavaParameters(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String className = md.getClassName();
                    String methodName = md.getMethodName();

                    String key = className + methodName;
                    DBJavaMethod javaMethod = cache.get(key);
                    if (javaMethod == null) {
                        DBSchema schema = content.ensureParentEntity();
                        DBJavaClass javaClass = valid(schema.getJavaClass(className));
                        javaMethod = valid(javaClass.getMethod(methodName));
                        cache.set(key, javaMethod);
                    }

                    return new DBJavaParameterImpl(javaMethod, md);
                });

        DynamicContentResultSetLoader.<DBPackageFunction, DBFunctionMetadata>create(
                "ALL_PACKAGE_FUNCTIONS", SCHEMA, PACKAGE_FUNCTION, true, true,
                (content, conn, mdi) -> mdi.loadAllPackageFunctions(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String programName = md.getPackageName();
                    DBPackage program = valid(cache.get(programName, () -> ((DBSchema) content.ensureParentEntity()).getPackage(programName)));
                    return new DBPackageFunctionImpl(program, md);
                });

        DynamicContentResultSetLoader.<DBPackageProcedure, DBProcedureMetadata>create(
                "ALL_PACKAGE_PROCEDURES", SCHEMA, PACKAGE_PROCEDURE, true, true,
                (content, conn, mdi) -> mdi.loadAllPackageProcedures(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String programName = md.getPackageName();
                    DBPackage program = valid(cache.get(programName, () -> ((DBSchema) content.ensureParentEntity()).getPackage(programName)));
                    return new DBPackageProcedureImpl(program, md);
                });

        DynamicContentResultSetLoader.<DBPackageType, DBTypeMetadata>create(
                "ALL_PACKAGE_TYPES", SCHEMA, PACKAGE_TYPE, true, true,
                (content, conn, mdi) -> mdi.loadAllPackageTypes(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String programName = md.getPackageName();
                    DBPackage program = valid(cache.get(programName, () -> ((DBSchema) content.ensureParentEntity()).getPackage(programName)));
                    return new DBPackageTypeImpl(program, md);
                });

        DynamicContentResultSetLoader.<DBTypeAttribute, DBTypeAttributeMetadata>create(
                "ALL_TYPE_ATTRIBUTES", SCHEMA, TYPE_ATTRIBUTE, true, true,
                (content, conn, mdi) -> mdi.loadAllTypeAttributes(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String typeName = md.getTypeName();
                    DBType type = valid(cache.get(typeName, () -> ((DBSchema) content.ensureParentEntity()).getType(typeName)));
                    return new DBTypeAttributeImpl(type, md);
                });

        DynamicContentResultSetLoader.<DBTypeFunction, DBFunctionMetadata>create(
                "ALL_TYPE_FUNCTIONS", SCHEMA, TYPE_FUNCTION, true, true,
                (content, conn, mdi) -> mdi.loadAllTypeFunctions(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String typeName = md.getTypeName();
                    DBType type = valid(cache.get(typeName, () -> ((DBSchema) content.ensureParentEntity()).getType(typeName)));
                    return new DBTypeFunctionImpl(type, md);
                });

        DynamicContentResultSetLoader.<DBTypeProcedure, DBProcedureMetadata>create(
                "ALL_TYPE_PROCEDURES", SCHEMA, TYPE_PROCEDURE, true, true,
                (content, conn, mdi) -> mdi.loadAllTypeProcedures(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String typeName = md.getTypeName();
                    DBType type = valid(cache.get(typeName, () -> ((DBSchema) content.ensureParentEntity()).getType(typeName)));
                    return new DBTypeProcedureImpl(type, md);
                });

        DynamicContentResultSetLoader.<DBArgument, DBArgumentMetadata>create(
                "ALL_METHOD_ARGUMENTS", SCHEMA, ARGUMENT, true, true,
                (content, conn, mdi) -> mdi.loadAllMethodArguments(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String programName = md.getProgramName();
                    String methodName = md.getMethodName();
                    String methodType = md.getMethodType();
                    short overload = md.getOverload();
                    DBSchema schema = content.ensureParentEntity();
                    DBProgram program = programName == null ? null : schema.getProgram(programName);

                    String key = methodName + methodType + overload;
                    DBMethod method = cache.get(key);
                    DBObjectType objectType = get(methodType);

                    if (method == null || method.getProgram() != program || method.getOverload() != overload) {
                        method = programName == null ?
                                schema.getMethod(methodName, objectType, overload):
                                program == null ? null : program.getMethod(methodName, overload);
                        cache.set(key, method);
                    }
                    return new DBArgumentImpl(valid(method), md);
                });

        DynamicContentResultSetLoader.<DBColumnColumnRelation, DBColumnColumnMetadata>create(
                "ALL_COLUMN_RELATIONS", SCHEMA, COLUMN_COLUMN, true, false,
                (content, conn, mdi) -> mdi.loadAllColumnRelations(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    DBSchema sourceSchema = getSchema(content, md.getSourceSchemaName());
                    DBDataset sourceDataset = valid(sourceSchema.getDataset(md.getSourceDatasetName()));
                    DBColumn sourceColumn = valid(sourceDataset.getColumn(md.getSourceColumnName()));

                    DBSchema targetSchema = content.ensureParentEntity();
                    DBDataset targetDataset = valid(targetSchema.getDataset(md.getTargetDatasetName()));
                    DBColumn targetColumn = valid(targetDataset.getColumn(md.getTargetColumnName()));
                    return new DBColumnColumnRelation(sourceColumn, targetColumn);
                });

        DynamicContentResultSetLoader.<DBConstraintColumnRelation, DBConstraintColumnMetadata>create(
                "ALL_CONSTRAINT_COLUMNS", SCHEMA, CONSTRAINT_COLUMN, true, false,
                (content, conn, mdi) -> mdi.loadAllConstraintRelations(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String datasetName = md.getDatasetName();
                    DBDataset dataset = valid(cache.get(datasetName, () -> ((DBSchema) content.ensureParentEntity()).getDataset(datasetName)));

                    DBColumn column = getCachedColumn(dataset, md.getColumnName());
                    DBConstraint constraint = getCachedConstraint(dataset, md.getConstraintName());
                    return new DBConstraintColumnRelation(constraint, column, md.getPosition());
                });

        DynamicContentResultSetLoader.<DBIndexColumnRelation, DBIndexColumnMetadata>create(
                "ALL_INDEX_COLUMNS", SCHEMA, INDEX_COLUMN, true, false,
                (content, conn, mdi) -> mdi.loadAllIndexRelations(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    DBSchema schema = content.ensureParentEntity();
                    String tableName = md.getTableName();
                    DBDataset dataset = valid(cache.get(tableName, () -> schema.getDataset(tableName)));

                    DBColumn column = getCachedColumn(dataset, md.getColumnName());
                    DBIndex index = getCachedIndex(dataset, md.getIndexName());
                    return new DBIndexColumnRelation(index, column);
                });

        DynamicContentResultSetLoader.<DBJsonViewTableRelation, DBJsonViewTableMetadata>create(
                "ALL_JSON_VIEW_TABLES", SCHEMA, JSON_VIEW_TABLE, true, false,
                (content, conn, mdi) -> mdi.loadAllJsonViewTableRelations(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    DBSchema schema = content.ensureParentEntity();
                    String jsonViewName = md.getJsonViewName();
                    String tableName = md.getTableName();
                    DBSchema tableSchema = content.ensureParentEntity();

                    DBJsonView jsonView = valid(cache.get(jsonViewName, () -> schema.getJsonView(jsonViewName)));
                    DBTable table = valid(tableSchema.getTable(tableName));
                    return new DBJsonViewTableRelation(jsonView, table, md.getPosition(), md.isRootTable(), md.isReadonly());
                });

    }

    /* Loaders for table child objects (children of DBDataset) */
    static {
        DynamicSubcontentLoader.create("DATASET_COLUMNS", DATASET, COLUMN,
                DynamicContentResultSetLoader.<DBColumn, DBColumnMetadata>create(
                        "DATASET_COLUMNS", DATASET, COLUMN, false, true,
                        (content, conn, mdi) -> mdi.loadColumns(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBColumnImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("DATASET_CONSTRAINTS", DATASET, CONSTRAINT,
                DynamicContentResultSetLoader.<DBConstraint, DBConstraintMetadata>create(
                        "DATASET_CONSTRAINTS", DATASET, CONSTRAINT, false, true,
                        (content, conn, mdi) -> mdi.loadConstraints(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBConstraintImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("DATASET_TRIGGERS", DATASET, DATASET_TRIGGER,
                DynamicContentResultSetLoader.<DBDatasetTrigger, DBTriggerMetadata>create(
                        "DATASET_TRIGGERS", DATASET, DATASET_TRIGGER, false, true,
                        (content, conn, mdi) -> mdi.loadDatasetTriggers(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBDatasetTriggerImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("DATASET_INDEXES", DATASET, INDEX,
                DynamicContentResultSetLoader.<DBIndex, DBIndexMetadata>create(
                        "DATASET_INDEXES", DATASET, INDEX, false, true,
                        (content, conn, mdi) -> mdi.loadIndexes(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBIndexImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("DATASET_INDEX_COLUMNS", DATASET, INDEX_COLUMN,
                DynamicContentResultSetLoader.<DBIndexColumnRelation, DBIndexColumnMetadata>create(
                        "DATASET_INDEX_COLUMNS", DATASET, INDEX_COLUMN, false, false,
                        (content, conn, mdi) -> mdi.loadIndexRelations(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> {
                            DBDataset dataset = valid(content.getParentEntity());
                            DBIndex index = valid(dataset.getIndex(md.getIndexName()));
                            DBColumn column = valid(dataset.getColumn(md.getColumnName()));
                            return new DBIndexColumnRelation(index, column);
                        }));

        DynamicSubcontentLoader.create("DATASET_CONSTRAINT_COLUMNS", DATASET, CONSTRAINT_COLUMN,
                DynamicContentResultSetLoader.<DBConstraintColumnRelation, DBConstraintColumnMetadata>create(
                        "DATASET_CONSTRAINT_COLUMNS", DATASET, CONSTRAINT_COLUMN, false, false,
                        (content, conn, mdi) -> mdi.loadConstraintRelations(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> {
                            DBDataset dataset = valid(content.getParentEntity());
                            DBColumn column = valid(dataset.getColumn(md.getColumnName()));
                            DBConstraint constraint = valid(dataset.getConstraint(md.getConstraintName()));
                            return new DBConstraintColumnRelation(constraint, column, md.getPosition());
                        }));

        DynamicSubcontentLoader.create("DATASET_COLUMN_RELATIONS", DATASET, COLUMN_COLUMN,
                DynamicContentResultSetLoader.<DBColumnColumnRelation, DBColumnColumnMetadata>create(
                        "DATASET_COLUMN_RELATIONS", DATASET, COLUMN_COLUMN, false, false,
                        (content, conn, mdi) -> mdi.loadColumnRelations(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> {
                            DBSchema sourceSchema = getSchema(content, md.getSourceSchemaName());
                            DBDataset sourceDataset = valid(sourceSchema.getDataset(md.getSourceDatasetName()));
                            DBColumn sourceColumn = valid(sourceDataset.getColumn(md.getSourceColumnName()));

                            DBDataset targetDataset = valid(content.getParentEntity());
                            DBColumn targetColumn = valid(targetDataset.getColumn(md.getTargetColumnName()));
                            return new DBColumnColumnRelation(sourceColumn, targetColumn);
                        }));

        DynamicSubcontentLoader.create("JSON_VIEW_TABLES", JSON_VIEW, JSON_VIEW_TABLE,
                DynamicContentResultSetLoader.<DBJsonViewTableRelation, DBJsonViewTableMetadata>create(
                        "JSON_VIEW_TABLES", JSON_VIEW, JSON_VIEW_TABLE, false, false,
                        (content, conn, mdi) -> mdi.loadJsonViewTableRelations(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> {
                            DBJsonView jsonView = valid(content.getParentEntity());
                            DBSchema tableSchema = content.ensureParentEntity(); // TODO
                            DBTable table = valid(tableSchema.getTable(md.getTableName()));
                            return new DBJsonViewTableRelation(jsonView, table, md.getPosition(), md.isRootTable(), md.isReadonly());
                        }));


        DynamicSubcontentLoader.create("NESTED_TABLES", TABLE, NESTED_TABLE,
                DynamicContentResultSetLoader.<DBNestedTable, DBNestedTableMetadata>create(
                        "NESTED_TABLES", TABLE, NESTED_TABLE, false, true,
                        (content, conn, mdi) -> mdi.loadNestedTables(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBNestedTableImpl(valid(content.getParentEntity()), md)));
    }

    /* Loaders for program child objects (children of DBProgram) */
    static {
        DynamicSubcontentLoader.create("PACKAGE_FUNCTIONS", PACKAGE, PACKAGE_FUNCTION,
                DynamicContentResultSetLoader.<DBPackageFunction, DBFunctionMetadata>create(
                        "PACKAGE_FUNCTIONS", PACKAGE, PACKAGE_FUNCTION, false, true,
                        (content, conn, mdi) -> mdi.loadPackageFunctions(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBPackageFunctionImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("PACKAGE_PROCEDURES", PACKAGE, PACKAGE_PROCEDURE,
                DynamicContentResultSetLoader.<DBPackageProcedure, DBProcedureMetadata>create(
                        "PACKAGE_PROCEDURES", PACKAGE, PACKAGE_PROCEDURE, false, true,
                        (content, conn, mdi) -> mdi.loadPackageProcedures(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBPackageProcedureImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("PACKAGE_TYPES", PACKAGE, PACKAGE_TYPE,
                DynamicContentResultSetLoader.<DBPackageType, DBTypeMetadata>create(
                        "PACKAGE_TYPES", PACKAGE, PACKAGE_TYPE, false, true,
                        (content, conn, mdi) -> mdi.loadPackageTypes(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBPackageTypeImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("JAVA_METHODS", JAVA_CLASS, JAVA_METHOD,
                DynamicContentResultSetLoader.<DBJavaMethod, DBJavaMethodMetadata>create(
                        "JAVA_METHODS", JAVA_CLASS, JAVA_METHOD, false, true,
                        (content, conn, mdi) -> mdi.loadJavaMethods(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBJavaMethodImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("JAVA_FIELDS", JAVA_CLASS, JAVA_FIELD,
                DynamicContentResultSetLoader.<DBJavaField, DBJavaFieldMetadata>create(
                        "JAVA_FIELDS", JAVA_CLASS, JAVA_FIELD, false, true,
                        (content, conn, mdi) -> mdi.loadJavaFields(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBJavaFieldImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("JAVA_INNER_CLASSES", JAVA_CLASS, JAVA_INNER_CLASS,
                DynamicContentResultSetLoader.<DBJavaClass, DBJavaClassMetadata>create(
                        "JAVA_INNER_CLASSES", JAVA_CLASS, JAVA_INNER_CLASS, false, true,
                        (content, conn, mdi) -> mdi.loadJavaInnerClasses(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBJavaClassImpl(valid(content.getSchema()), md)));

        DynamicContentResultSetLoader.<DBTypeAttribute, DBTypeAttributeMetadata>create(
                "PACKAGE_TYPE_ATTRIBUTES", PACKAGE_TYPE, TYPE_ATTRIBUTE, true, true,
                (content, conn, mdi) -> {
                    DBPackageType type = valid(content.getParentEntity());
                    return mdi.loadProgramTypeAttributes(
                            type.getSchema().getName(),
                            type.getPackage().getName(),
                            type.getName(), conn);
                    },
                (content, cache, md) -> new DBTypeAttributeImpl(valid(content.getParentEntity()), md));

        DynamicSubcontentLoader.create("TYPE_TYPE_ATTRIBUTES", TYPE, TYPE_ATTRIBUTE,
                DynamicContentResultSetLoader.<DBTypeAttribute, DBTypeAttributeMetadata>create(
                        "TYPE_TYPE_ATTRIBUTES", TYPE, TYPE_ATTRIBUTE, false, true,
                        (content, conn, mdi) -> mdi.loadTypeAttributes(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBTypeAttributeImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("TYPE_TYPE_FUNCTIONS", TYPE, TYPE_FUNCTION,
                DynamicContentResultSetLoader.<DBTypeFunction, DBFunctionMetadata>create(
                        "TYPE_TYPE_FUNCTIONS", TYPE, TYPE_FUNCTION, false, true,
                        (content, conn, mdi) -> mdi.loadTypeFunctions(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBTypeFunctionImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("TYPE_TYPE_PROCEDURES", TYPE, TYPE_PROCEDURE,
                DynamicContentResultSetLoader.<DBTypeProcedure, DBProcedureMetadata>create(
                        "TYPE_TYPE_PROCEDURES", TYPE, TYPE_PROCEDURE, false, true,
                        (content, conn, mdi) -> mdi.loadTypeProcedures(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBTypeProcedureImpl(valid(content.getParentEntity()), md)));


        DynamicSubcontentLoader.create("TYPE_TYPES", TYPE, TYPE, null/*TODO*/);

        DynamicSubcontentLoader.create("METHOD_ARGUMENTS", METHOD, ARGUMENT,
                DynamicContentResultSetLoader.<DBArgument, DBArgumentMetadata>create(
                        "METHOD_ARGUMENTS", METHOD, ARGUMENT, false, true,
                        (content, conn, mdi) -> {
                            DBMethod method = content.ensureParentEntity();
                            String ownerName = method.getSchemaName();
                            short overload = method.getOverload();
                            DBProgram program = method.getProgram();
                            return program == null ?
                                    mdi.loadMethodArguments(ownerName, method.getName(), method.getMethodType().id(), overload, conn) :
                                    mdi.loadProgramMethodArguments(ownerName, program.getName(), method.getName(), overload, conn);
                        },
                        (content, cache, md) -> new DBArgumentImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("JAVA_METHOD_PARAMETERS", JAVA_METHOD, JAVA_PARAMETER,
                DynamicContentResultSetLoader.<DBJavaParameter, DBJavaParameterMetadata>create(
                        "JAVA_METHOD_PARAMETERS", JAVA_METHOD, JAVA_PARAMETER, false, true,
                        (content, conn, mdi) -> {
                            DBJavaMethod method = content.ensureParentEntity();
                            String className = method.getOwnerClass().getName();
                            String methodName = method.getName();
                            String ownerName = method.getSchemaName();
                            short index = method.getIndex();
                            return mdi.loadJavaParameters(ownerName, className, methodName, index, conn);
                        },
                        (content, cache, md) -> new DBJavaParameterImpl(valid(content.getParentEntity()), md)));
    }

    /* Loaders for object dependencies */
    static {
        DynamicContentResultSetLoader.<DBObject, DBObjectDependencyMetadata>create(
                "INCOMING_DEPENDENCIES", null, INCOMING_DEPENDENCY, true, false,
                (content, conn, mdi) ->  mdi.loadReferencedObjects(content.getParentSchemaName(), content.getParentObjectName(), conn),
                (content, cache, md) -> {
                    String objectOwner = md.getObjectOwner();
                    String objectName = md.getObjectName();
                    String objectTypeName = md.getObjectType();
                    DBObjectType objectType = get(objectTypeName);
                    if (objectType == PACKAGE_BODY) objectType = PACKAGE;
                    if (objectType == TYPE_BODY) objectType = TYPE;

                    DBSchema schema = getSchema(content, objectOwner);
                    return schema.getChildObject(objectType, objectName, (short) 0, true);
                });

        DynamicContentResultSetLoader.<DBObject, DBObjectDependencyMetadata>create(
                "OUTGOING_DEPENDENCIES", null, OUTGOING_DEPENDENCY, true, false,
                (content, conn, mdi) ->  mdi.loadReferencingObjects(content.getParentSchemaName(), content.getParentObjectName(), conn),
                (content, cache, md) -> {
                    String objectOwner = md.getObjectOwner();
                    String objectName = md.getObjectName();
                    String objectTypeName = md.getObjectType();
                    DBObjectType objectType = get(objectTypeName);
                    if (objectType == PACKAGE_BODY) objectType = PACKAGE;
                    if (objectType == TYPE_BODY) objectType = TYPE;

                    DBSchema schema = getSchema(content, objectOwner);
                    return schema.getChildObject(objectType, objectName, (short) 0, true);
                });
    }

    /* Loaders for sub-contents from relation lists */
    static {
        DBObjectListFromRelationListLoader.create("COLUMN_CONSTRAINTS", COLUMN, CONSTRAINT);
        DBObjectListFromRelationListLoader.create("COLUMN_INDEXES", COLUMN, INDEX);
        DBObjectListFromRelationListLoader.create("CONSTRAINT_COLUMNS", CONSTRAINT, COLUMN);
        DBObjectListFromRelationListLoader.create("INDEX_COLUMNS", INDEX, COLUMN);
        DBObjectListFromRelationListLoader.create("ROLE_PRIVILEGES", ROLE, GRANTED_PRIVILEGE);
        DBObjectListFromRelationListLoader.create("ROLE_ROLES", ROLE, GRANTED_ROLE);
        DBObjectListFromRelationListLoader.create("USER_ROLES", USER, GRANTED_ROLE);
        DBObjectListFromRelationListLoader.create("USER_PRIVILEGES", USER, GRANTED_PRIVILEGE);
        DBObjectListFromRelationListLoader.create("JSON_VIEW_TABLES", JSON_VIEW, TABLE);
    }


    private static <T> T valid(T element) {
        if (element == null || isNotValid(element)) {
            throw ElementSkippedException.INSTANCE;
        }
        return element;
    }

    private static DBColumn getCachedColumn(DBDataset dataset, String columnName) {
        DBObjectListContainer schemaObjects = valid(dataset.getSchema().getChildObjects());
        GroupedDynamicContent<DBColumn> columns = valid(cast(schemaObjects.getObjectList(COLUMN)));
        return valid(columns.getChildElement(dataset, columnName));
    }

    private static DBConstraint getCachedConstraint(DBDataset dataset, String constraintName) {
        DBObjectListContainer schemaObjects = valid(dataset.getSchema().getChildObjects());
        GroupedDynamicContent<DBConstraint> constraint = valid(cast(schemaObjects.getObjectList(CONSTRAINT)));
        return valid(constraint.getChildElement(dataset, constraintName));
    }

    private static DBIndex getCachedIndex(DBDataset dataset, String indexName) {
        DBObjectListContainer schemaObjects = valid(dataset.getSchema().getChildObjects());
        GroupedDynamicContent<DBIndex> index = valid(cast(schemaObjects.getObjectList(INDEX)));
        return valid(index.getChildElement(dataset, indexName));
    }

    private static @NotNull DBSchema getSchema(DynamicContent content, String schemaName) {
        return valid(content.getConnection().getObjectBundle().getSchema(schemaName));
    }
}
