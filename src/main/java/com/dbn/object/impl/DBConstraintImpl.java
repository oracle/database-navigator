package com.dbn.object.impl;

import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBConstraintMetadata;
import com.dbn.object.DBColumn;
import com.dbn.object.DBConstraint;
import com.dbn.object.DBDataset;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.list.DBObjectRelationList;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.properties.DBObjectPresentableProperty;
import com.dbn.object.properties.PresentableProperty;
import com.dbn.object.properties.SimplePresentableProperty;
import com.dbn.object.type.DBConstraintType;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.dbn.object.common.property.DBObjectProperty.DISABLEABLE;
import static com.dbn.object.common.property.DBObjectProperty.SCHEMA_OBJECT;
import static com.dbn.object.type.DBObjectRelationType.CONSTRAINT_COLUMN;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.CONSTRAINT;

@Getter
class DBConstraintImpl extends DBSchemaObjectImpl<DBConstraintMetadata> implements DBConstraint {
    private DBConstraintType constraintType;
    private DBObjectRef<DBConstraint> foreignKeyConstraint;

    private String checkCondition;

    DBConstraintImpl(DBDataset dataset, DBConstraintMetadata metadata) throws SQLException {
        super(dataset, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBConstraintMetadata metadata) throws SQLException {
        String name = metadata.getConstraintName();
        checkCondition = metadata.getCheckCondition();

        String typeString = metadata.getConstraintType();
        constraintType = // TODO move to metadata interface
            typeString == null ? DBConstraintType.UNKNOWN :
            Objects.equals(typeString, "CHECK")? DBConstraintType.CHECK :
            Objects.equals(typeString, "UNIQUE") ? DBConstraintType.UNIQUE_KEY :
            Objects.equals(typeString, "PRIMARY KEY") ? DBConstraintType.PRIMARY_KEY :
            Objects.equals(typeString, "FOREIGN KEY") ? DBConstraintType.FOREIGN_KEY :
            Objects.equals(typeString, "VIEW CHECK") ? DBConstraintType.VIEW_CHECK :
            Objects.equals(typeString, "VIEW READONLY") ? DBConstraintType.VIEW_READONLY : DBConstraintType.UNKNOWN;

        if (checkCondition == null && constraintType == DBConstraintType.CHECK) checkCondition = "";

        if (isForeignKey()) {
            String fkOwner = metadata.getFkConstraintOwner();
            String fkName = metadata.getFkConstraintName();

            DBSchema schema = connection.getObjectBundle().getSchema(fkOwner);
            if (schema != null) {
                DBObjectRef<DBSchema> schemaRef = schema.ref();
                foreignKeyConstraint = new DBObjectRef<>(schemaRef, CONSTRAINT, fkName);
            }
        }
        return name;
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        super.initLists(connection);
        DBObjectListContainer childObjects = ensureChildObjects();
        childObjects.createSubcontentObjectList(
                COLUMN, this,
                getDataset(),
                CONSTRAINT_COLUMN);
    }

    @Override
    public void initStatus(DBConstraintMetadata metadata) throws SQLException {
        boolean enabled = metadata.isEnabled();
        getStatus().set(DBObjectStatus.ENABLED, enabled);
    }

    @Override
    protected void initProperties() {
        properties.set(SCHEMA_OBJECT, true);
        properties.set(DISABLEABLE, true);
    }

    @NotNull
    @Override
    public String getQualifiedName() {
        return getSchemaName() + '.' + getName();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        boolean enabled = getStatus().is(DBObjectStatus.ENABLED);
        return enabled ? Icons.DBO_CONSTRAINT : Icons.DBO_CONSTRAINT_DISABLED;
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return CONSTRAINT;
    }

    @Override
    public boolean isPrimaryKey() {
        return constraintType == DBConstraintType.PRIMARY_KEY;
    }

    @Override
    public boolean isForeignKey() {
        return constraintType == DBConstraintType.FOREIGN_KEY;
    }
    
    @Override
    public boolean isUniqueKey() {
        return constraintType == DBConstraintType.UNIQUE_KEY;
    }

    @Override
    public DBDataset getDataset() {
        return (DBDataset) getParentObject();
    }

    @Override
    public List<DBColumn> getColumns() {
        return getChildObjects(COLUMN);
    }

    @Override
    public short getColumnPosition(DBColumn column) {
        DBDataset dataset = getDataset();
        if (dataset == null) return 0;

        DBObjectListContainer childObjects = dataset.getChildObjects();
        if (childObjects == null) return 0;

        DBObjectRelationList<DBConstraintColumnRelation> relations = childObjects.getRelations(CONSTRAINT_COLUMN);
        if (relations == null) return 0;

        for (DBConstraintColumnRelation relation : relations.getObjectRelations()) {
            if (Objects.equals(relation.getConstraint(), this) &&
                    Objects.equals(relation.getColumn(), column)) {
                return relation.getPosition();
            }
        }
        return 0;
    }

    @Override
    @Nullable
    public DBColumn getColumnForPosition(short position) {
        DBDataset dataset = getDataset();
        if (dataset == null) return null;

        DBObjectListContainer childObjects = dataset.getChildObjects();
        if (childObjects == null) return null;

        DBObjectRelationList<DBConstraintColumnRelation> relations = childObjects.getRelations(CONSTRAINT_COLUMN);
        if (relations == null) return null;

        for (DBConstraintColumnRelation relation : relations.getObjectRelations()) {
            DBConstraint constraint = relation.getConstraint();
            if (Objects.equals(constraint, this) && relation.getPosition() == position)
                return relation.getColumn();
        }
        return null;
    }

    @Override
    @Nullable
    public DBConstraint getForeignKeyConstraint() {
        return DBObjectRef.get(foreignKeyConstraint);
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        switch (constraintType) {
            case CHECK: ttb.append(true, "check constraint - " + (
                    checkCondition.length() > 120 ?
                            checkCondition.substring(0, 120) + "..." :
                            checkCondition), true); break;
            case PRIMARY_KEY: ttb.append(true, "primary key constraint", true); break;
            case FOREIGN_KEY: ttb.append(true, "foreign key constraint", true); break;
            case UNIQUE_KEY: ttb.append(true, "unique constraint", true); break;
        }

        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }

    @Override
    public List<PresentableProperty> getPresentableProperties() {
        List<PresentableProperty> properties = super.getPresentableProperties();
        switch (constraintType) {
            case CHECK:
                properties.add(0, new SimplePresentableProperty("Check condition", checkCondition));
                properties.add(0, new SimplePresentableProperty("Constraint type", "Check"));
                break;
            case PRIMARY_KEY: properties.add(0, new SimplePresentableProperty("Constraint type", "Primary Key")); break;
            case FOREIGN_KEY:
                DBConstraint foreignKeyConstraint = getForeignKeyConstraint();
                if (foreignKeyConstraint != null) {
                    properties.add(0, new DBObjectPresentableProperty(foreignKeyConstraint));
                    properties.add(0, new SimplePresentableProperty("Constraint type", "Foreign Key"));
                }
                break;
            case UNIQUE_KEY: properties.add(0, new SimplePresentableProperty("Constraint type", "Unique")); break;
        }

        return properties;
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        List<DBObjectNavigationList> navigationLists = new LinkedList<>();

        List<DBColumn> columns = getColumns();
        if (columns.size() > 0) {
            navigationLists.add(DBObjectNavigationList.create("Columns", columns));
        }

        DBConstraint foreignKeyConstraint = getForeignKeyConstraint();
        if (foreignKeyConstraint != null) {
            navigationLists.add(DBObjectNavigationList.create("Foreign key constraint", foreignKeyConstraint));
        }

        return navigationLists;
    }

    @Override
    public String getPresentableTextConditionalDetails() {
         switch (constraintType) {
            case CHECK: return "Check (" + checkCondition + ")";
            case PRIMARY_KEY: return "Primary key";
            case FOREIGN_KEY: return "Foreign key (" + (foreignKeyConstraint == null ? "" : foreignKeyConstraint.getPath()) + ")";
            case UNIQUE_KEY: return "Unique";
        }
        return null;
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/

    @Override
    public boolean isLeaf() {
        return true;
    }
}
