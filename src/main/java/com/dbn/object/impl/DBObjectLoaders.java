package com.dbn.object.impl;

import com.dbn.common.content.loader.DynamicContentLoaderImpl;
import com.dbn.common.content.loader.DynamicContentResultSetLoader;
import com.dbn.common.content.loader.DynamicSubcontentLoader;
import com.dbn.common.exception.ElementSkippedException;
import com.dbn.database.common.metadata.DBObjectMetadata;
import com.dbn.database.common.metadata.def.DBArgumentMetadata;
import com.dbn.database.common.metadata.def.DBCharsetMetadata;
import com.dbn.database.common.metadata.def.DBClusterMetadata;
import com.dbn.database.common.metadata.def.DBColumnMetadata;
import com.dbn.database.common.metadata.def.DBConstraintColumnMetadata;
import com.dbn.database.common.metadata.def.DBConstraintMetadata;
import com.dbn.database.common.metadata.def.DBCredentialMetadata;
import com.dbn.database.common.metadata.def.DBDatabaseLinkMetadata;
import com.dbn.database.common.metadata.def.DBDimensionMetadata;
import com.dbn.database.common.metadata.def.DBFunctionMetadata;
import com.dbn.database.common.metadata.def.DBGrantedPrivilegeMetadata;
import com.dbn.database.common.metadata.def.DBGrantedRoleMetadata;
import com.dbn.database.common.metadata.def.DBIndexColumnMetadata;
import com.dbn.database.common.metadata.def.DBIndexMetadata;
import com.dbn.database.common.metadata.def.DBJavaObjectMetadata;
import com.dbn.database.common.metadata.def.DBMaterializedViewMetadata;
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
import com.dbn.object.DBDimension;
import com.dbn.object.DBFunction;
import com.dbn.object.DBGrantedPrivilege;
import com.dbn.object.DBGrantedRole;
import com.dbn.object.DBIndex;
import com.dbn.object.DBJavaObject;
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
import com.dbn.object.common.list.loader.DBObjectListFromRelationListLoader;
import com.dbn.object.type.DBObjectRelationType;
import com.dbn.object.type.DBObjectType;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.content.DynamicContentProperty.MASTER;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.util.Unsafe.cast;

@UtilityClass
public class DBObjectLoaders {
    public static void initLoaders() {}

    /* Loaders for root objects (children of DBObjectBundle) */
    static {
        DynamicContentLoaderImpl.<DBConsole, DBObjectMetadata>create(
                "CONSOLES", null, DBObjectType.CONSOLE, true,
                content -> content.setElements(content.getConnection().getConsoleBundle().getConsoles()));


        DynamicContentResultSetLoader.<DBSchema, DBSchemaMetadata>create(
                "SCHEMAS", null, DBObjectType.SCHEMA, true, true,
                (content, conn, mdi) -> mdi.loadSchemas(conn),
                (content, cache, md) -> new DBSchemaImpl(content.getConnection(), cast(md)));

        DynamicContentResultSetLoader.<DBUser, DBUserMetadata>create(
                "USERS", null, DBObjectType.USER, true, true,
                (content, conn, mdi) -> mdi.loadUsers(conn),
                (content, cache, md) -> new DBUserImpl(content.getConnection(), md));

        DynamicContentResultSetLoader.<DBRole, DBRoleMetadata>create(
                "ROLES", null, DBObjectType.ROLE, true, true,
                (content, conn, mdi) -> mdi.loadRoles(conn),
                (content, cache, md) -> new DBRoleImpl(content.getConnection(), cast(md)));

        DynamicContentResultSetLoader.<DBSystemPrivilege, DBPrivilegeMetadata>create(
                "SYSTEM_PRIVILEGES", null, DBObjectType.SYSTEM_PRIVILEGE, true, true,
                (content, conn, mdi) -> mdi.loadSystemPrivileges(conn),
                (content, cache, md) -> new DBSystemPrivilegeImpl(content.getConnection(), md));

        DynamicContentResultSetLoader.<DBObjectPrivilege, DBPrivilegeMetadata>create(
                "OBJECT_PRIVILEGES", null, DBObjectType.OBJECT_PRIVILEGE, true, true,
                (content, conn, mdi) -> mdi.loadObjectPrivileges(conn),
                (content, cache, md) -> new DBObjectPrivilegeImpl(content.getConnection(), md));

        DynamicContentResultSetLoader.<DBCharset, DBCharsetMetadata>create(
                "CHARSETS", null, DBObjectType.CHARSET, true, true,
                (content, conn, mdi) -> mdi.loadCharsets(conn),
                (content, cache, md) -> new DBCharsetImpl(content.getConnection(), md));

        DynamicContentResultSetLoader.<DBUserRoleRelation, DBGrantedRoleMetadata>create(
                "USER_ROLES", null, DBObjectRelationType.USER_ROLE, true, true,
                (content, conn, mdi) -> mdi.loadAllUserRoles(conn),
                (content, cache, md) -> {
                    DBObjectBundle objects = content.ensureParentEntity();
                    DBUser user = valid(objects.getUser(md.getUserName()));
                    DBGrantedRole role = new DBGrantedRoleImpl(user, md);
                    return new DBUserRoleRelation(user, role);
                });

        DynamicContentResultSetLoader.<DBUserPrivilegeRelation, DBGrantedPrivilegeMetadata>create(
                "USER_PRIVILEGES", null, DBObjectRelationType.USER_PRIVILEGE, true, true,
                (content, conn, mdi) -> mdi.loadAllUserPrivileges(conn),
                (content, cache, md) -> {
                    DBObjectBundle objects = content.ensureParentEntity();
                    DBUser user = valid(objects.getUser(md.getUserName()));
                    DBGrantedPrivilege privilege = new DBGrantedPrivilegeImpl(user, md);
                    return new DBUserPrivilegeRelation(user, privilege);
                });

        DynamicContentResultSetLoader.<DBRoleRoleRelation, DBGrantedRoleMetadata>create(
                "ROLE_ROLES", null, DBObjectRelationType.ROLE_ROLE, true, true,
                (content, conn, mdi) -> mdi.loadAllRoleRoles(conn),
                (content, cache, md) -> {
                    DBObjectBundle objects = content.ensureParentEntity();
                    DBRole role = valid(objects.getRole(md.getRoleName()));
                    DBGrantedRole grantedRole = new DBGrantedRoleImpl(role, md);
                    return new DBRoleRoleRelation(role, grantedRole);
                });

        DynamicContentResultSetLoader.<DBRolePrivilegeRelation, DBGrantedPrivilegeMetadata>create(
                "ROLE_PRIVILEGES", null, DBObjectRelationType.ROLE_PRIVILEGE, true, true,
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
                "PRIVILEGE_USERS", DBObjectType.PRIVILEGE, DBObjectType.USER, true,
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
                "PRIVILEGE_ROLES", DBObjectType.PRIVILEGE, DBObjectType.ROLE, true, content -> {
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
                "TABLES", DBObjectType.SCHEMA, DBObjectType.TABLE, true, true,
                (content, conn, mdi) -> mdi.loadTables(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBTableImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBView, DBViewMetadata>create(
                "VIEWS", DBObjectType.SCHEMA, DBObjectType.VIEW, true, true,
                (content, conn, mdi) -> mdi.loadViews(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBViewImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBMaterializedView, DBMaterializedViewMetadata>create(
                "MATERIALIZED_VIEWS", DBObjectType.SCHEMA, DBObjectType.MATERIALIZED_VIEW, true, true,
                (content, conn, mdi) -> mdi.loadMaterializedViews(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBMaterializedViewImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBSynonym, DBSynonymMetadata>create(
                "SYNONYMS", DBObjectType.SCHEMA, DBObjectType.SYNONYM, true, true,
                (content, conn, mdi) -> mdi.loadSynonyms(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBSynonymImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBSequence, DBSequenceMetadata>create(
                "SEQUENCES", DBObjectType.SCHEMA, DBObjectType.SEQUENCE, true, true,
                (content, conn, mdi) -> mdi.loadSequences(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBSequenceImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBProcedure, DBProcedureMetadata>create(
                "PROCEDURES", DBObjectType.SCHEMA, DBObjectType.PROCEDURE, true, true,
                (content, conn, mdi) -> mdi.loadProcedures(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBProcedureImpl((DBSchema) content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBFunction, DBFunctionMetadata>create(
                "FUNCTIONS", DBObjectType.SCHEMA, DBObjectType.FUNCTION, true, true,
                (content, conn, mdi) -> mdi.loadFunctions(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBFunctionImpl((DBSchema) content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBPackage, DBPackageMetadata>create(
                "PACKAGES", DBObjectType.SCHEMA, DBObjectType.PACKAGE, true, true,
                (content, conn, mdi) -> mdi.loadPackages(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBPackageImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBType, DBTypeMetadata>create(
                "TYPES", DBObjectType.SCHEMA, DBObjectType.TYPE, true, true,
                (content, conn, mdi) -> mdi.loadTypes(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBTypeImpl((DBSchema) content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBDatabaseTrigger, DBTriggerMetadata>create(
                "DATABASE_TRIGGERS", DBObjectType.SCHEMA, DBObjectType.DATABASE_TRIGGER, true, true,
                (content, conn, mdi) -> mdi.loadDatabaseTriggers(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBDatabaseTriggerImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBJavaObject, DBJavaObjectMetadata>create(
                "JAVA_OBJECTS", DBObjectType.SCHEMA, DBObjectType.JAVA_OBJECT, true, true,
                (content, conn, mdi) -> mdi.loadJavaObjects(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBJavaObjectImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBDimension, DBDimensionMetadata>create(
                "DIMENSIONS", DBObjectType.SCHEMA, DBObjectType.DIMENSION, true, true,
                (content, conn, mdi) -> mdi.loadDimensions(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBDimensionImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBCluster, DBClusterMetadata>create(
                "CLUSTERS", DBObjectType.SCHEMA, DBObjectType.CLUSTER, true, true,
                (content, conn, mdi) -> mdi.loadClusters(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBClusterImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBCredential, DBCredentialMetadata>create(
                "CREDENTIALS", DBObjectType.SCHEMA, DBObjectType.CREDENTIAL, true, true,
                (content, conn, mdi) -> mdi.loadCredentials(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBCredentialImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBDatabaseLink, DBDatabaseLinkMetadata>create(
                "DBLINKS", DBObjectType.SCHEMA, DBObjectType.DBLINK, true, true,
                (content, conn, mdi) -> mdi.loadDatabaseLinks(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> new DBDatabaseLinkImpl(content.getParentEntity(), md));

        DynamicContentResultSetLoader.<DBColumn, DBColumnMetadata>create(
                "ALL_COLUMNS", DBObjectType.SCHEMA, DBObjectType.COLUMN, true, true,
                (content, conn, mdi) -> mdi.loadAllColumns(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String datasetName = md.getDatasetName();
                    DBDataset dataset = valid(cache.get(datasetName, () -> ((DBSchema) content.ensureParentEntity()).getDataset(datasetName)));
                    return new DBColumnImpl(dataset, md);
                });

        DynamicContentResultSetLoader.<DBConstraint, DBConstraintMetadata>create(
                "ALL_CONSTRAINTS", DBObjectType.SCHEMA, DBObjectType.CONSTRAINT, true, true,
                (content, conn, mdi) -> mdi.loadAllConstraints(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String datasetName = md.getDatasetName();
                    DBDataset dataset = valid(cache.get(datasetName, () -> ((DBSchema) content.ensureParentEntity()).getDataset(datasetName)));
                    return new DBConstraintImpl(dataset, md);
                });

        DynamicContentResultSetLoader.<DBIndex, DBIndexMetadata>create(
                "ALL_INDEXES", DBObjectType.SCHEMA, DBObjectType.INDEX, true, true,
                (content, conn, mdi) -> mdi.loadAllIndexes(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String datasetName = md.getTableName();
                    DBDataset dataset = valid(cache.get(datasetName, () -> ((DBSchema) content.ensureParentEntity()).getDataset(datasetName)));
                    return new DBIndexImpl(dataset, md);
                });

        DynamicContentResultSetLoader.<DBDatasetTrigger, DBTriggerMetadata>create(
                "ALL_DATASET_TRIGGERS", DBObjectType.SCHEMA, DBObjectType.DATASET_TRIGGER, true, true,
                (content, conn, mdi) -> mdi.loadAllDatasetTriggers(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String datasetName = md.getDatasetName();
                    DBDataset dataset = valid(cache.get(datasetName, () -> ((DBSchema) content.ensureParentEntity()).getDataset(datasetName)));
                    return new DBDatasetTriggerImpl(dataset, md);
                });

        DynamicContentResultSetLoader.<DBNestedTable, DBNestedTableMetadata>create(
                "ALL_NESTED_TABLES", DBObjectType.SCHEMA, DBObjectType.NESTED_TABLE, true, true,
                (content, conn, mdi) -> mdi.loadAllNestedTables(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String tableName = md.getTableName();
                    DBTable table = valid(cache.get(tableName, () -> ((DBSchema) content.ensureParentEntity()).getTable(tableName)));
                    return new DBNestedTableImpl(table, md);
                });

        DynamicContentResultSetLoader.<DBPackageFunction, DBFunctionMetadata>create(
                "ALL_PACKAGE_FUNCTIONS", DBObjectType.SCHEMA, DBObjectType.PACKAGE_FUNCTION, true, true,
                (content, conn, mdi) -> mdi.loadAllPackageFunctions(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String programName = md.getPackageName();
                    DBPackage program = valid(cache.get(programName, () -> ((DBSchema) content.ensureParentEntity()).getPackage(programName)));
                    return new DBPackageFunctionImpl(program, md);
                });

        DynamicContentResultSetLoader.<DBPackageProcedure, DBProcedureMetadata>create(
                "ALL_PACKAGE_PROCEDURES", DBObjectType.SCHEMA, DBObjectType.PACKAGE_PROCEDURE, true, true,
                (content, conn, mdi) -> mdi.loadAllPackageProcedures(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String programName = md.getPackageName();
                    DBPackage program = valid(cache.get(programName, () -> ((DBSchema) content.ensureParentEntity()).getPackage(programName)));
                    return new DBPackageProcedureImpl(program, md);
                });

        DynamicContentResultSetLoader.<DBPackageType, DBTypeMetadata>create(
                "ALL_PACKAGE_TYPES", DBObjectType.SCHEMA, DBObjectType.PACKAGE_TYPE, true, true,
                (content, conn, mdi) -> mdi.loadAllPackageTypes(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String programName = md.getPackageName();
                    DBPackage program = valid(cache.get(programName, () -> ((DBSchema) content.ensureParentEntity()).getPackage(programName)));
                    return new DBPackageTypeImpl(program, md);
                });

        DynamicContentResultSetLoader.<DBTypeAttribute, DBTypeAttributeMetadata>create(
                "ALL_TYPE_ATTRIBUTES", DBObjectType.SCHEMA, DBObjectType.TYPE_ATTRIBUTE, true, true,
                (content, conn, mdi) -> mdi.loadAllTypeAttributes(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String typeName = md.getTypeName();
                    DBType type = valid(cache.get(typeName, () -> ((DBSchema) content.ensureParentEntity()).getType(typeName)));
                    return new DBTypeAttributeImpl(type, md);
                });

        DynamicContentResultSetLoader.<DBTypeFunction, DBFunctionMetadata>create(
                "ALL_TYPE_FUNCTIONS", DBObjectType.SCHEMA, DBObjectType.TYPE_FUNCTION, true, true,
                (content, conn, mdi) -> mdi.loadAllTypeFunctions(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String typeName = md.getTypeName();
                    DBType type = valid(cache.get(typeName, () -> ((DBSchema) content.ensureParentEntity()).getType(typeName)));
                    return new DBTypeFunctionImpl(type, md);
                });

        DynamicContentResultSetLoader.<DBTypeProcedure, DBProcedureMetadata>create(
                "ALL_TYPE_PROCEDURES", DBObjectType.SCHEMA, DBObjectType.TYPE_PROCEDURE, true, true,
                (content, conn, mdi) -> mdi.loadAllTypeProcedures(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String typeName = md.getTypeName();
                    DBType type = valid(cache.get(typeName, () -> ((DBSchema) content.ensureParentEntity()).getType(typeName)));
                    return new DBTypeProcedureImpl(type, md);
                });

        DynamicContentResultSetLoader.<DBArgument, DBArgumentMetadata>create(
                "ALL_METHOD_ARGUMENTS", DBObjectType.SCHEMA, DBObjectType.ARGUMENT, true, true,
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
                    DBObjectType objectType = DBObjectType.get(methodType);

                    if (method == null || method.getProgram() != program || method.getOverload() != overload) {
                        method = programName == null ?
                                schema.getMethod(methodName, objectType, overload):
                                program == null ? null : program.getMethod(methodName, overload);
                        cache.set(key, method);
                    }
                    return new DBArgumentImpl(valid(method), md);
                });

        DynamicContentResultSetLoader.<DBConstraintColumnRelation, DBConstraintColumnMetadata>create(
                "ALL_CONSTRAINT_COLUMNS", DBObjectType.SCHEMA, DBObjectRelationType.CONSTRAINT_COLUMN, true, false,
                (content, conn, mdi) -> mdi.loadAllConstraintRelations(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String datasetName = md.getDatasetName();
                    DBDataset dataset = valid(cache.get(datasetName, () -> ((DBSchema) content.ensureParentEntity()).getDataset(datasetName)));
                    DBColumn column = valid(dataset.getColumn(md.getColumnName()));
                    DBConstraint constraint = valid(dataset.getConstraint(md.getConstraintName()));
                    return new DBConstraintColumnRelation(constraint, column, md.getPosition());
                });

        DynamicContentResultSetLoader.<DBIndexColumnRelation, DBIndexColumnMetadata>create(
                "ALL_INDEX_COLUMNS", DBObjectType.SCHEMA, DBObjectRelationType.INDEX_COLUMN, true, false,
                (content, conn, mdi) -> mdi.loadAllIndexRelations(content.ensureParentEntity().getName(), conn),
                (content, cache, md) -> {
                    String tableName = md.getTableName();
                    DBDataset dataset = valid(cache.get(tableName, () -> ((DBSchema) content.ensureParentEntity()).getDataset(tableName)));
                    DBColumn column = valid(dataset.getColumn(md.getColumnName()));
                    DBIndex index = valid(dataset.getIndex(md.getIndexName()));
                    return new DBIndexColumnRelation(index, column);
                });

    }

    /* Loaders for table child objects (children of DBDataset) */
    static {
        DynamicSubcontentLoader.create("DATASET_COLUMNS", DBObjectType.DATASET, DBObjectType.COLUMN,
                DynamicContentResultSetLoader.<DBColumn, DBColumnMetadata>create(
                        "DATASET_COLUMNS", DBObjectType.DATASET, DBObjectType.COLUMN, false, true,
                        (content, conn, mdi) -> mdi.loadColumns(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBColumnImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("DATASET_CONSTRAINTS", DBObjectType.DATASET, DBObjectType.CONSTRAINT,
                DynamicContentResultSetLoader.<DBConstraint, DBConstraintMetadata>create(
                        "DATASET_CONSTRAINTS", DBObjectType.DATASET, DBObjectType.CONSTRAINT, false, true,
                        (content, conn, mdi) -> mdi.loadConstraints(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBConstraintImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("DATASET_TRIGGERS", DBObjectType.DATASET, DBObjectType.DATASET_TRIGGER,
                DynamicContentResultSetLoader.<DBDatasetTrigger, DBTriggerMetadata>create(
                        "DATASET_TRIGGERS", DBObjectType.DATASET, DBObjectType.DATASET_TRIGGER, false, true,
                        (content, conn, mdi) -> mdi.loadDatasetTriggers(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBDatasetTriggerImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("DATASET_INDEXES", DBObjectType.DATASET, DBObjectType.INDEX,
                DynamicContentResultSetLoader.<DBIndex, DBIndexMetadata>create(
                        "DATASET_INDEXES", DBObjectType.DATASET, DBObjectType.INDEX, false, true,
                        (content, conn, mdi) -> mdi.loadIndexes(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBIndexImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("DATASET_INDEX_COLUMNS", DBObjectType.DATASET, DBObjectRelationType.INDEX_COLUMN,
                DynamicContentResultSetLoader.<DBIndexColumnRelation, DBIndexColumnMetadata>create(
                        "DATASET_INDEX_COLUMNS", DBObjectType.DATASET, DBObjectRelationType.INDEX_COLUMN, false, false,
                        (content, conn, mdi) -> mdi.loadIndexRelations(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> {
                            DBDataset dataset = valid(content.getParentEntity());
                            DBIndex index = valid(dataset.getIndex(md.getIndexName()));
                            DBColumn column = valid(dataset.getColumn(md.getColumnName()));
                            return new DBIndexColumnRelation(index, column);
                        }));

        DynamicSubcontentLoader.create("DATASET_CONSTRAINT_COLUMNS", DBObjectType.DATASET, DBObjectRelationType.CONSTRAINT_COLUMN,
                DynamicContentResultSetLoader.<DBConstraintColumnRelation, DBConstraintColumnMetadata>create(
                        "DATASET_CONSTRAINT_COLUMNS", DBObjectType.DATASET, DBObjectRelationType.CONSTRAINT_COLUMN, false, false,
                        (content, conn, mdi) -> mdi.loadConstraintRelations(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> {
                            DBDataset dataset = valid(content.getParentEntity());
                            DBColumn column = valid(dataset.getColumn(md.getColumnName()));
                            DBConstraint constraint = valid(dataset.getConstraint(md.getConstraintName()));
                            return new DBConstraintColumnRelation(constraint, column, md.getPosition());
                        }));

        DynamicSubcontentLoader.create("NESTED_TABLES", DBObjectType.TABLE, DBObjectType.NESTED_TABLE,
                DynamicContentResultSetLoader.<DBNestedTable, DBNestedTableMetadata>create(
                        "NESTED_TABLES", DBObjectType.TABLE, DBObjectType.NESTED_TABLE, false, true,
                        (content, conn, mdi) -> mdi.loadNestedTables(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBNestedTableImpl(valid(content.getParentEntity()), md)));
    }

    /* Loaders for program child objects (children of DBProgram) */
    static {
        DynamicSubcontentLoader.create("ALL_PACKAGE_FUNCTIONS", DBObjectType.PACKAGE, DBObjectType.PACKAGE_FUNCTION,
                DynamicContentResultSetLoader.<DBPackageFunction, DBFunctionMetadata>create(
                        "PACKAGE_FUNCTIONS", DBObjectType.PACKAGE, DBObjectType.PACKAGE_FUNCTION, false, true,
                        (content, conn, mdi) -> mdi.loadPackageFunctions(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBPackageFunctionImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("ALL_PACKAGE_PROCEDURES", DBObjectType.PACKAGE, DBObjectType.PACKAGE_PROCEDURE,
                DynamicContentResultSetLoader.<DBPackageProcedure, DBProcedureMetadata>create(
                        "PACKAGE_PROCEDURES", DBObjectType.PACKAGE, DBObjectType.PACKAGE_PROCEDURE, false, true,
                        (content, conn, mdi) -> mdi.loadPackageProcedures(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBPackageProcedureImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("ALL_PACKAGE_TYPES", DBObjectType.PACKAGE, DBObjectType.PACKAGE_TYPE,
                DynamicContentResultSetLoader.<DBPackageType, DBTypeMetadata>create(
                        "PACKAGE_TYPES", DBObjectType.PACKAGE, DBObjectType.PACKAGE_TYPE, false, true,
                        (content, conn, mdi) -> mdi.loadPackageTypes(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBPackageTypeImpl(valid(content.getParentEntity()), md)));


        DynamicContentResultSetLoader.<DBTypeAttribute, DBTypeAttributeMetadata>create(
                "PACKAGE_TYPE_ATTRIBUTES", DBObjectType.PACKAGE_TYPE, DBObjectType.TYPE_ATTRIBUTE, true, true,
                (content, conn, mdi) -> {
                    DBPackageType type = valid(content.getParentEntity());
                    return mdi.loadProgramTypeAttributes(
                            type.getSchema().getName(),
                            type.getPackage().getName(),
                            type.getName(), conn);
                    },
                (content, cache, md) -> new DBTypeAttributeImpl(valid(content.getParentEntity()), md));

        DynamicSubcontentLoader.create("TYPE_TYPE_ATTRIBUTES", DBObjectType.TYPE, DBObjectType.TYPE_ATTRIBUTE,
                DynamicContentResultSetLoader.<DBTypeAttribute, DBTypeAttributeMetadata>create(
                        "TYPE_TYPE_ATTRIBUTES", DBObjectType.TYPE, DBObjectType.TYPE_ATTRIBUTE, false, true,
                        (content, conn, mdi) -> mdi.loadTypeAttributes(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBTypeAttributeImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("TYPE_TYPE_FUNCTIONS", DBObjectType.TYPE, DBObjectType.TYPE_FUNCTION,
                DynamicContentResultSetLoader.<DBTypeFunction, DBFunctionMetadata>create(
                        "TYPE_TYPE_FUNCTIONS", DBObjectType.TYPE, DBObjectType.TYPE_FUNCTION, false, true,
                        (content, conn, mdi) -> mdi.loadTypeFunctions(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBTypeFunctionImpl(valid(content.getParentEntity()), md)));

        DynamicSubcontentLoader.create("TYPE_TYPE_PROCEDURES", DBObjectType.TYPE, DBObjectType.TYPE_PROCEDURE,
                DynamicContentResultSetLoader.<DBTypeProcedure, DBProcedureMetadata>create(
                        "TYPE_TYPE_PROCEDURES", DBObjectType.TYPE, DBObjectType.TYPE_PROCEDURE, false, true,
                        (content, conn, mdi) -> mdi.loadTypeProcedures(content.getParentSchemaName(), content.getParentObjectName(), conn),
                        (content, cache, md) -> new DBTypeProcedureImpl(valid(content.getParentEntity()), md)));


        DynamicSubcontentLoader.create("TYPE_TYPES", DBObjectType.TYPE, DBObjectType.TYPE, null/*TODO*/);

        DynamicSubcontentLoader.create("METHOD_ARGUMENTS", DBObjectType.METHOD, DBObjectType.ARGUMENT,
                DynamicContentResultSetLoader.<DBArgument, DBArgumentMetadata>create(
                        "METHOD_ARGUMENTS", DBObjectType.METHOD, DBObjectType.ARGUMENT, false, true,
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
    }

    /* Loaders for object dependencies */
    static {
        DynamicContentResultSetLoader.<DBObject, DBObjectDependencyMetadata>create(
                "INCOMING_DEPENDENCIES", null, DBObjectType.INCOMING_DEPENDENCY, true, false,
                (content, conn, mdi) ->  mdi.loadReferencedObjects(content.getParentSchemaName(), content.getParentObjectName(), conn),
                (content, cache, md) -> {
                    String objectOwner = md.getObjectOwner();
                    String objectName = md.getObjectName();
                    String objectTypeName = md.getObjectType();
                    DBObjectType objectType = DBObjectType.get(objectTypeName);
                    if (objectType == DBObjectType.PACKAGE_BODY) objectType = DBObjectType.PACKAGE;
                    if (objectType == DBObjectType.TYPE_BODY) objectType = DBObjectType.TYPE;

                    DBSchema schema = valid(cache.get(objectOwner, () -> content.ensureParentEntity().getObjectBundle().getSchema(objectOwner)));
                    return schema.getChildObject(objectType, objectName, (short) 0, true);
                });

        DynamicContentResultSetLoader.<DBObject, DBObjectDependencyMetadata>create(
                "OUTGOING_DEPENDENCIES", null, DBObjectType.OUTGOING_DEPENDENCY, true, false,
                (content, conn, mdi) ->  mdi.loadReferencingObjects(content.getParentSchemaName(), content.getParentObjectName(), conn),
                (content, cache, md) -> {
                    String objectOwner = md.getObjectOwner();
                    String objectName = md.getObjectName();
                    String objectTypeName = md.getObjectType();
                    DBObjectType objectType = DBObjectType.get(objectTypeName);
                    if (objectType == DBObjectType.PACKAGE_BODY) objectType = DBObjectType.PACKAGE;
                    if (objectType == DBObjectType.TYPE_BODY) objectType = DBObjectType.TYPE;

                    DBSchema schema = valid(cache.get(objectOwner, () -> content.ensureParentEntity().getObjectBundle().getSchema(objectOwner)));
                    return schema.getChildObject(objectType, objectName, (short) 0, true);
                });
    }

    /* Loaders for sub-contents from relation lists */
    static {
        DBObjectListFromRelationListLoader.create("COLUMN_CONSTRAINTS", DBObjectType.COLUMN, DBObjectType.CONSTRAINT);
        DBObjectListFromRelationListLoader.create("COLUMN_INDEXES", DBObjectType.COLUMN, DBObjectType.INDEX);
        DBObjectListFromRelationListLoader.create("CONSTRAINT_COLUMNS", DBObjectType.CONSTRAINT, DBObjectType.COLUMN);
        DBObjectListFromRelationListLoader.create("INDEX_COLUMNS", DBObjectType.INDEX, DBObjectType.COLUMN);
        DBObjectListFromRelationListLoader.create("ROLE_PRIVILEGES", DBObjectType.ROLE, DBObjectType.GRANTED_PRIVILEGE);
        DBObjectListFromRelationListLoader.create("ROLE_ROLES", DBObjectType.ROLE, DBObjectType.GRANTED_ROLE);
        DBObjectListFromRelationListLoader.create("USER_ROLES", DBObjectType.USER, DBObjectType.GRANTED_ROLE);
        DBObjectListFromRelationListLoader.create("USER_PRIVILEGES", DBObjectType.USER, DBObjectType.GRANTED_PRIVILEGE);
    }


    private static <T> T valid(T element) {
        if (element == null || isNotValid(element)) throw ElementSkippedException.INSTANCE;
        return element;
    }

}
