package com.dbn.object;

import com.dbn.connection.DatabaseEntity;
import com.dbn.connection.SchemaId;
import com.dbn.object.common.DBRootObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;

import java.util.List;
import java.util.Set;

public interface DBSchema extends DBRootObject, com.dbn.api.object.DBSchema {
    boolean isPublicSchema();
    boolean isUserSchema();
    boolean isSystemSchema();
    boolean isEmptySchema();
    List<DBDataset> getDatasets();
    List<DBTable> getTables();
    List<DBView> getViews();
    List<DBMaterializedView> getMaterializedViews();
    List<DBIndex> getIndexes();
    List<DBSynonym> getSynonyms();
    List<DBSequence> getSequences();
    List<DBProcedure> getProcedures();
    List<DBFunction> getFunctions();
    List<DBPackage> getPackages();
    List<DBDatasetTrigger> getDatasetTriggers();
    List<DBDatabaseTrigger> getDatabaseTriggers();
    List<DBType> getTypes();
    List<DBDimension> getDimensions();
    List<DBCluster> getClusters();
    List<DBDatabaseLink> getDatabaseLinks();
    List<DBColumn> getPrimaryKeyColumns();
    List<DBColumn> getForeignKeyColumns();

    DBDataset getDataset(String name);
    DBTable getTable(String name);
    DBView getView(String name);
    DBMaterializedView getMaterializedView(String name);
    DBIndex getIndex(String name);
    DBType getType(String name);
    DBPackage getPackage(String name);
    DBProgram getProgram(String name);
    DBMethod getMethod(String name, DBObjectType methodType, short overload);
    DBMethod getMethod(String name, short overload);
    DBProcedure getProcedure(String name, short overload);
    DBFunction getFunction(String name, short overload);
    DBCluster getCluster(String name);
    DBDatabaseLink getDatabaseLink(String name);

    @Override
    DBObjectRef<DBSchema> ref();
    SchemaId getIdentifier();

    Set<DatabaseEntity> resetObjectsStatus();
}
