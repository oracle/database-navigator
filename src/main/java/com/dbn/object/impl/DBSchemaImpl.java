package com.dbn.object.impl;

import com.dbn.browser.DatabaseBrowserUtils;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.common.latent.Latent;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseEntity;
import com.dbn.connection.SchemaId;
import com.dbn.database.common.metadata.def.DBSchemaMetadata;
import com.dbn.object.DBCluster;
import com.dbn.object.DBColumn;
import com.dbn.object.DBConstraint;
import com.dbn.object.DBDatabaseLink;
import com.dbn.object.DBDatabaseTrigger;
import com.dbn.object.DBDataset;
import com.dbn.object.DBDatasetTrigger;
import com.dbn.object.DBDimension;
import com.dbn.object.DBFunction;
import com.dbn.object.DBIndex;
import com.dbn.object.DBMaterializedView;
import com.dbn.object.DBMethod;
import com.dbn.object.DBPackage;
import com.dbn.object.DBProcedure;
import com.dbn.object.DBProgram;
import com.dbn.object.DBSchema;
import com.dbn.object.DBSequence;
import com.dbn.object.DBSynonym;
import com.dbn.object.DBTable;
import com.dbn.object.DBType;
import com.dbn.object.DBUser;
import com.dbn.object.DBView;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBRootObjectImpl;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.list.DBObjectListVisitor;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.dbn.common.content.DynamicContentProperty.GROUPED;
import static com.dbn.common.content.DynamicContentProperty.HIDDEN;
import static com.dbn.common.content.DynamicContentProperty.INTERNAL;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.object.common.property.DBObjectProperty.DEBUGABLE;
import static com.dbn.object.common.property.DBObjectProperty.EMPTY_SCHEMA;
import static com.dbn.object.common.property.DBObjectProperty.INVALIDABLE;
import static com.dbn.object.common.property.DBObjectProperty.PUBLIC_SCHEMA;
import static com.dbn.object.common.property.DBObjectProperty.ROOT_OBJECT;
import static com.dbn.object.common.property.DBObjectProperty.SCHEMA_OBJECT;
import static com.dbn.object.common.property.DBObjectProperty.SYSTEM_SCHEMA;
import static com.dbn.object.common.property.DBObjectProperty.USER_SCHEMA;
import static com.dbn.object.type.DBObjectRelationType.CONSTRAINT_COLUMN;
import static com.dbn.object.type.DBObjectRelationType.INDEX_COLUMN;
import static com.dbn.object.type.DBObjectType.ARGUMENT;
import static com.dbn.object.type.DBObjectType.CLUSTER;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.CONSTRAINT;
import static com.dbn.object.type.DBObjectType.DATABASE_TRIGGER;
import static com.dbn.object.type.DBObjectType.DATASET_TRIGGER;
import static com.dbn.object.type.DBObjectType.DBLINK;
import static com.dbn.object.type.DBObjectType.DIMENSION;
import static com.dbn.object.type.DBObjectType.FUNCTION;
import static com.dbn.object.type.DBObjectType.INDEX;
import static com.dbn.object.type.DBObjectType.JAVA_OBJECT;
import static com.dbn.object.type.DBObjectType.MATERIALIZED_VIEW;
import static com.dbn.object.type.DBObjectType.NESTED_TABLE;
import static com.dbn.object.type.DBObjectType.PACKAGE;
import static com.dbn.object.type.DBObjectType.PACKAGE_FUNCTION;
import static com.dbn.object.type.DBObjectType.PACKAGE_PROCEDURE;
import static com.dbn.object.type.DBObjectType.PACKAGE_TYPE;
import static com.dbn.object.type.DBObjectType.PROCEDURE;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static com.dbn.object.type.DBObjectType.SEQUENCE;
import static com.dbn.object.type.DBObjectType.SYNONYM;
import static com.dbn.object.type.DBObjectType.TABLE;
import static com.dbn.object.type.DBObjectType.TYPE;
import static com.dbn.object.type.DBObjectType.TYPE_ATTRIBUTE;
import static com.dbn.object.type.DBObjectType.TYPE_FUNCTION;
import static com.dbn.object.type.DBObjectType.TYPE_PROCEDURE;
import static com.dbn.object.type.DBObjectType.VIEW;

class DBSchemaImpl extends DBRootObjectImpl<DBSchemaMetadata> implements DBSchema {
    private Latent<List<DBColumn>> primaryKeyColumns;
    private Latent<List<DBColumn>> foreignKeyColumns;

    DBSchemaImpl(ConnectionHandler connection, DBSchemaMetadata metadata) throws SQLException {
        super(connection, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBSchemaMetadata metadata) throws SQLException {
        String name = metadata.getSchemaName();
        set(PUBLIC_SCHEMA, metadata.isPublic());
        set(SYSTEM_SCHEMA, metadata.isSystem());
        set(EMPTY_SCHEMA, metadata.isEmpty());
        set(USER_SCHEMA, Strings.equalsIgnoreCase(name, connection.getUserName()));
        return name;
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        DBObjectListContainer childObjects = ensureChildObjects();

        childObjects.createObjectList(TABLE,             this);
        childObjects.createObjectList(VIEW,              this);
        childObjects.createObjectList(MATERIALIZED_VIEW, this);
        childObjects.createObjectList(SYNONYM,           this);
        childObjects.createObjectList(SEQUENCE,          this);
        childObjects.createObjectList(PROCEDURE,         this);
        childObjects.createObjectList(FUNCTION,          this);
        childObjects.createObjectList(PACKAGE,           this);
        childObjects.createObjectList(TYPE,              this);
        childObjects.createObjectList(DATABASE_TRIGGER,  this);
        childObjects.createObjectList(JAVA_OBJECT,       this);
        childObjects.createObjectList(DIMENSION,         this);
        childObjects.createObjectList(CLUSTER,           this);
        childObjects.createObjectList(DBLINK,            this);

        DBObjectList<DBConstraint> constraints = childObjects.createObjectList(CONSTRAINT, this, INTERNAL, GROUPED);
        DBObjectList<DBIndex> indexes          = childObjects.createObjectList(INDEX,      this, INTERNAL, GROUPED);
        DBObjectList<DBColumn> columns         = childObjects.createObjectList(COLUMN,     this, INTERNAL, GROUPED, HIDDEN);

        childObjects.createObjectList(DATASET_TRIGGER,   this, INTERNAL, GROUPED);
        childObjects.createObjectList(NESTED_TABLE,      this, INTERNAL, GROUPED, HIDDEN);
        childObjects.createObjectList(PACKAGE_FUNCTION,  this, INTERNAL, GROUPED, HIDDEN);
        childObjects.createObjectList(PACKAGE_PROCEDURE, this, INTERNAL, GROUPED, HIDDEN);
        childObjects.createObjectList(PACKAGE_TYPE,      this, INTERNAL, GROUPED, HIDDEN);
        childObjects.createObjectList(TYPE_ATTRIBUTE,    this, INTERNAL, GROUPED, HIDDEN);
        childObjects.createObjectList(TYPE_FUNCTION,     this, INTERNAL, GROUPED, HIDDEN);
        childObjects.createObjectList(TYPE_PROCEDURE,    this, INTERNAL, GROUPED, HIDDEN);
        childObjects.createObjectList(ARGUMENT,          this, INTERNAL, GROUPED, HIDDEN);

        //ol.createHiddenObjectList(DBObjectType.TYPE_METHOD, this, TYPE_METHODS_LOADER);

        childObjects.createObjectRelationList(CONSTRAINT_COLUMN, this, constraints, columns, INTERNAL, GROUPED);
        childObjects.createObjectRelationList(INDEX_COLUMN, this, indexes, columns, INTERNAL, GROUPED);

        this.primaryKeyColumns = Latent.mutable(
                () -> nd(columns).getSignature(),
                () -> nvl(Lists.filter(nd(columns).getObjects(), c -> c.isPrimaryKey()), Collections.emptyList()));

        this.foreignKeyColumns = Latent.mutable(
                () -> nd(columns).getSignature(),
                () -> nvl(Lists.filter(nd(columns).getObjects(), c -> c.isForeignKey()), Collections.emptyList()));
    }

    @Override
    public void initProperties() {
        properties.set(ROOT_OBJECT, true);
    }

    @Nullable
    @Override
    public DBUser getOwner() {
        return getObjectBundle().getUser(getName());
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return SCHEMA;
    }

    @Override
    public boolean isPublicSchema() {
        return is(PUBLIC_SCHEMA);
    }

    @Override
    public boolean isUserSchema() {
        return is(USER_SCHEMA);
    }

    @Override
    public boolean isSystemSchema() {
        return is(SYSTEM_SCHEMA);
    }

    @Override
    public boolean isEmptySchema() {
        return is(EMPTY_SCHEMA);
    }

    @Nullable
    @Override
    public DBObject getDefaultNavigationObject() {
        return getOwner();
    }

    @Override
    public <T extends DBObject> T  getChildObject(DBObjectType type, String name, short overload, boolean lookupHidden) {
        if (!type.isSchemaObject()) return null;

        DBObject object = super.getChildObject(type, name, overload, lookupHidden);
        if (object == null && type != SYNONYM) {
            DBSynonym synonym = super.getChildObject(SYNONYM, name, overload, lookupHidden);
            if (synonym != null) {
                DBObjectType underlyingObjectType = synonym.getUnderlyingObjectType();
                if (underlyingObjectType != null && underlyingObjectType.matches(type)) {
                    return cast(synonym);
                }
            }
        } else {
            return cast(object);
        }
        return null;
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        DBUser user = getOwner();
        if (user == null) return null;

        LinkedList<DBObjectNavigationList> navigationLists = new LinkedList<>();
        navigationLists.add(DBObjectNavigationList.create("User", user));
        return navigationLists;
    }

    @Override
    public List<DBTable> getTables() {
        return getChildObjects(TABLE);
    }

    @Override
    public List<DBView> getViews() {
        return getChildObjects(VIEW);
    }

    @Override
    public List<DBMaterializedView> getMaterializedViews() {
        return getChildObjects(MATERIALIZED_VIEW);
    }

    @Override
    public List<DBIndex> getIndexes() {
        return getChildObjects(INDEX);
    }

    @Override
    public List<DBSynonym> getSynonyms() {
        return getChildObjects(SYNONYM);
    }

    @Override
    public List<DBSequence> getSequences() {
        return getChildObjects(SEQUENCE);
    }

    @Override
    public List<DBProcedure> getProcedures() {
        return getChildObjects(PROCEDURE);
    }

    @Override
    public List<DBFunction> getFunctions() {
        return getChildObjects(FUNCTION);
    }

    @Override
    public List<DBPackage> getPackages() {
        return getChildObjects(PACKAGE);
    }

    public List<DBColumn> getPrimaryKeyColumns() {
        return primaryKeyColumns.get();
    }

    public List<DBColumn> getForeignKeyColumns() {
        return foreignKeyColumns.get();
    }

    @Override
    public List<DBDatasetTrigger> getDatasetTriggers() {
        return getChildObjects(DATASET_TRIGGER);
    }

    @Override
    public List<DBDatabaseTrigger> getDatabaseTriggers() {
        return getChildObjects(DATABASE_TRIGGER);
    }

    @Override
    public List<DBType> getTypes() {
        return getChildObjects(TYPE);
    }

    @Override
    public List<DBDimension> getDimensions() {
        return getChildObjects(DIMENSION);
    }

    @Override
    public List<DBCluster> getClusters() {
        return getChildObjects(CLUSTER);
    }

    @Override
    public List<DBDatabaseLink> getDatabaseLinks() {
        return getChildObjects(DBLINK);
    }


    @Override
    public DBTable getTable(String name) {
        return getChildObject(TABLE, name);
    }

    @Override
    public DBView getView(String name) {
        return getChildObject(VIEW, name);
    }

    @Override
    public DBMaterializedView getMaterializedView(String name) {
        return getChildObject(MATERIALIZED_VIEW, name);
    }

    @Override
    public DBIndex getIndex(String name) {
        return getChildObject(INDEX, name);
    }

    @Override
    public DBCluster getCluster(String name) {
        return getChildObject(CLUSTER, name);
    }

    @Override
    public DBDatabaseLink getDatabaseLink(String name) {
        return getChildObject(DBLINK, name);
    }

    @Override
    public List<DBDataset> getDatasets() {
        List<DBDataset> datasets = new ArrayList<>();
        datasets.addAll(getTables());
        datasets.addAll(getViews());
        datasets.addAll(getMaterializedViews());
        return datasets;
    }


    @Override
    public DBDataset getDataset(String name) {
        DBDataset dataset = getTable(name);
        if (dataset != null) return dataset;

        dataset = getView(name);
        if (dataset != null) return dataset;

        if (!MATERIALIZED_VIEW.isSupported(this)) return null;
        dataset = getMaterializedView(name);
        return dataset;
    }

    @Nullable
    private <T extends DBSchemaObject> T getObjectFallbackOnSynonym(DBObjectType objectType, String name) {
        DBObjectList<T> objects = getChildObjectList(objectType);
        if (objects == null) return null;

        T object = objects.getObject(name);
        if (object != null) return object;

        if (!SYNONYM.isSupported(this)) return null;
        DBSynonym synonym = getChildObject(SYNONYM, name);
        if (synonym == null) return null;

        DBObject underlyingObject = synonym.getUnderlyingObject();
        if (underlyingObject == null) return null;
        if (underlyingObject.getObjectType() != objects.getObjectType()) return null;

        return cast(underlyingObject);

    }

    @Override
    public DBType getType(String name) {
        return getObjectFallbackOnSynonym(TYPE, name);
    }

    @Override
    public DBPackage getPackage(String name) {
        return getObjectFallbackOnSynonym(PACKAGE, name);
    }

    @Override
    public DBProcedure getProcedure(String name, short overload) {
        return overload > 0 ?
                getChildObject(PROCEDURE, name, overload) :
                getObjectFallbackOnSynonym(PROCEDURE, name);
    }

    @Override
    public DBFunction getFunction(String name, short overload) {
        return overload > 0 ?
                getChildObject(FUNCTION, name, overload) :
                getObjectFallbackOnSynonym(FUNCTION, name);
    }

    @Override
    public DBProgram getProgram(String name) {
        DBProgram program = getPackage(name);
        if (program == null) program = getType(name);
        return program;
    }

    @Override
    public DBMethod getMethod(String name, DBObjectType methodType, short overload) {
        if (methodType == null) {
            DBMethod method = getProcedure(name, overload);
            if (method == null) method = getFunction(name, overload);
            return method;
        } else if (methodType == PROCEDURE) {
            return getProcedure(name, overload);
        } else if (methodType == FUNCTION) {
            return getFunction(name, overload);
        }
        return null;
    }

    @Override
    public DBMethod getMethod(String name, short overload) {
        return getMethod(name, null, overload);
    }

    @Override
    public boolean isParentOf(DBObject object) {
        if (object instanceof DBSchemaObject) {
            DBSchemaObject schemaObject = (DBSchemaObject) object;
            return schemaObject.is(SCHEMA_OBJECT) && this.equals(schemaObject.getSchema());

        }
        return false;
    }

    @Override
    public SchemaId getIdentifier() {
        return SchemaId.get(getName());
    }

    public Set<DatabaseEntity> resetObjectsStatus() {
        ObjectStatusUpdater updater = new ObjectStatusUpdater();
        ensureChildObjects().visit(updater, true);
        return updater.getRefreshNodes();
    }

    @Getter
    static class ObjectStatusUpdater implements DBObjectListVisitor {
        private final Set<DatabaseEntity> refreshNodes = new HashSet<>();

        @Override
        public void visit(DBObjectList<?> objectList) {
            if (objectList.isDirty()) return;
            if (objectList.isLoading()) return;
            if (!objectList.isLoaded()) return;

            List<DBObject> objects = cast(objectList.getObjects());
            for (DBObject object : objects) {
                ProgressMonitor.checkCancelled();

                if (object instanceof DBSchemaObject) {
                    DBSchemaObject schemaObject = (DBSchemaObject) object;
                    DBObjectStatusHolder objectStatus = schemaObject.getStatus();
                    if (schemaObject.is(INVALIDABLE)) {
                        if (objectStatus.set(DBObjectStatus.VALID, true)) {
                            refreshNodes.add(object.getParent());
                        }
                    }
                    if (schemaObject.is(DEBUGABLE)) {
                        if (objectStatus.set(DBObjectStatus.DEBUG, false)) {
                            refreshNodes.add(object.getParent());
                        }
                    }
                } else {
                    break;
                }
            }
        }
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, getObjectType().getName(), true);
        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/
    @Override
    @NotNull
    public List<BrowserTreeNode> buildPossibleTreeChildren() {
        return DatabaseBrowserUtils.createList(
                getChildObjectList(TABLE),
                getChildObjectList(VIEW),
                getChildObjectList(MATERIALIZED_VIEW),
                getChildObjectList(SYNONYM),
                getChildObjectList(SEQUENCE),
                getChildObjectList(PROCEDURE),
                getChildObjectList(FUNCTION),
                getChildObjectList(PACKAGE),
                getChildObjectList(TYPE),
                getChildObjectList(DATABASE_TRIGGER),
                getChildObjectList(JAVA_OBJECT),
                getChildObjectList(DIMENSION),
                getChildObjectList(CLUSTER),
                getChildObjectList(DBLINK));
    }

    @Override
    public boolean hasVisibleTreeChildren() {
        ObjectTypeFilterSettings settings = getObjectTypeFilterSettings();
        return
            settings.isVisible(TABLE) ||
            settings.isVisible(VIEW) ||
            settings.isVisible(JAVA_OBJECT) ||
            settings.isVisible(MATERIALIZED_VIEW) ||
            settings.isVisible(SYNONYM) ||
            settings.isVisible(SEQUENCE) ||
            settings.isVisible(PROCEDURE) ||
            settings.isVisible(FUNCTION) ||
            settings.isVisible(PACKAGE) ||
            settings.isVisible(TYPE) ||
            settings.isVisible(DATABASE_TRIGGER) ||
            settings.isVisible(DIMENSION) ||
            settings.isVisible(CLUSTER) ||
            settings.isVisible(DBLINK);
    }
}
