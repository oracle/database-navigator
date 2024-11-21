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

import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.common.icon.Icons;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.grid.options.DataGridSettings;
import com.dbn.data.type.DBDataType;
import com.dbn.database.common.metadata.def.DBColumnMetadata;
import com.dbn.object.DBColumn;
import com.dbn.object.DBConstraint;
import com.dbn.object.DBDataset;
import com.dbn.object.DBIndex;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.DBType;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectImpl;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.list.DBObjectRelationList;
import com.dbn.object.common.list.ObjectListProvider;
import com.dbn.object.properties.DBDataTypePresentableProperty;
import com.dbn.object.properties.DBObjectPresentableProperty;
import com.dbn.object.properties.PresentableProperty;
import com.dbn.object.properties.SimplePresentableProperty;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.dbn.object.common.property.DBObjectProperty.FOREIGN_KEY;
import static com.dbn.object.common.property.DBObjectProperty.HIDDEN;
import static com.dbn.object.common.property.DBObjectProperty.IDENTITY;
import static com.dbn.object.common.property.DBObjectProperty.NULLABLE;
import static com.dbn.object.common.property.DBObjectProperty.PRIMARY_KEY;
import static com.dbn.object.common.property.DBObjectProperty.UNIQUE_KEY;
import static com.dbn.object.type.DBObjectRelationType.CONSTRAINT_COLUMN;
import static com.dbn.object.type.DBObjectRelationType.INDEX_COLUMN;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.CONSTRAINT;
import static com.dbn.object.type.DBObjectType.INDEX;
import static com.dbn.object.type.DBObjectType.TYPE_ATTRIBUTE;

class DBColumnImpl extends DBObjectImpl<DBColumnMetadata> implements DBColumn {
    private DBDataType dataType;
    private short position;

    DBColumnImpl(@NotNull DBDataset dataset, DBColumnMetadata metadata) throws SQLException {
        super(dataset, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBColumnMetadata metadata) throws SQLException {
        String name = metadata.getColumnName();
        set(PRIMARY_KEY, metadata.isPrimaryKey());
        set(FOREIGN_KEY, metadata.isForeignKey());
        set(UNIQUE_KEY, metadata.isUniqueKey());
        set(IDENTITY, metadata.isIdentity());
        set(NULLABLE, metadata.isNullable());
        set(HIDDEN, metadata.isHidden());
        position = metadata.getPosition();

        dataType = DBDataType.get(connection, metadata.getDataType());
        return name;
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        DBDataset dataset = getDataset();
        DBObjectListContainer childObjects = ensureChildObjects();
        childObjects.createSubcontentObjectList(CONSTRAINT, this, dataset, CONSTRAINT_COLUMN);
        childObjects.createSubcontentObjectList(INDEX, this, dataset, INDEX_COLUMN);

        DBObjectList typeAttributes = initDeclaredType();
        childObjects.addObjectList(typeAttributes);
    }

    private DBObjectList initDeclaredType() {
        DBType declaredType = dataType.getDeclaredType();
        if (declaredType == null) return null;

        DBObjectListContainer typeChildObjects = declaredType.getChildObjects();
        if (typeChildObjects == null) return null;

        return typeChildObjects.getObjectList(TYPE_ATTRIBUTE);
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return COLUMN;
    }

    @Override
    public DBDataType getDataType() {
        return dataType;
    }

    @Override
    public short getPosition() {
        return position;
    }

    @Override
    @Nullable
    public DBObject getDefaultNavigationObject() {
        if (isForeignKey()) {
            return getForeignKeyColumn();
        }
        return null;
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, getObjectType().getName(), true);
        ttb.append(false, " - ", true);
        ttb.append(false, dataType.getQualifiedName(), true);

        if (isPrimaryKey()) ttb.append(false,  "&nbsp;&nbsp;PK", true);
        if (isForeignKey()) ttb.append(false, isPrimaryKey() ? ",&nbsp;FK" : "&nbsp;&nbsp;FK", true);
        if (!isPrimaryKey() && !isForeignKey() && !isNullable()) ttb.append(false, "&nbsp;&nbsp;NOT NULL", true);

        if (isForeignKey() && getForeignKeyColumn() != null) {
            ttb.append(true, "FK column:&nbsp;", false);
            DBColumn foreignKeyColumn = getForeignKeyColumn();
            if (foreignKeyColumn != null) {
                ttb.append(false, foreignKeyColumn.getDataset().getName() + '.' + foreignKeyColumn.getName(), false);
            }
        }

        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return isPrimaryKey() ? isForeignKey() ? Icons.DBO_COLUMN_PFK : Icons.DBO_COLUMN_PK :
               isForeignKey() ? Icons.DBO_COLUMN_FK :
               isHidden() || isAudit() ? Icons.DBO_COLUMN_HIDDEN :
               Icons.DBO_COLUMN;
    }

    @Override
    public DBDataset getDataset() {
        return getParentObject();
    }

    public boolean isAudit() {
        return DataGridSettings.isAuditColumn(getProject(), getName());
    }

    @Override
    public boolean isNullable() {
        return is(NULLABLE);
    }

    @Override
    public boolean isHidden() {
        return is(HIDDEN);
    }

    @Override
    public boolean isPrimaryKey() {
        return is(PRIMARY_KEY);
    }

    @Override
    public boolean isUniqueKey() {
        return is(UNIQUE_KEY);
    }

    @Override
    public boolean isIdentity() {
        return is(IDENTITY);
    }

    @Override
    public boolean isForeignKey() {
        return is(FOREIGN_KEY);
    }

    @Override
    public boolean isSinglePrimaryKey() {
        if (!isPrimaryKey()) return false;

        for (DBConstraint constraint : getConstraints()) {
            if (constraint.isPrimaryKey() && constraint.getColumns().size() == 1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<DBIndex> getIndexes() {
        return getChildObjects(INDEX);
    }

    @Override
    public List<DBConstraint> getConstraints() {
        return getChildObjects(CONSTRAINT);
    }

    @Override
    public short getConstraintPosition(DBConstraint constraint) {
        DBObjectListContainer childObjects = getDataset().getChildObjects();
        if (childObjects == null) return 0;

        DBObjectRelationList<DBConstraintColumnRelation> relations = childObjects.getRelations(CONSTRAINT_COLUMN);
        if (relations == null) return 0;

        for (DBConstraintColumnRelation relation : relations.getObjectRelations()) {
            DBColumn relationColumn = relation.getColumn();
            DBConstraint relationConstraint = relation.getConstraint();
            if (Objects.equals(relationColumn, this) && Objects.equals(relationConstraint, constraint)){
                return relation.getPosition();
            }
        }
        return 0;
    }

    @Override
    public DBConstraint getConstraintForPosition(short position) {
        DBObjectListContainer childObjects = getDataset().getChildObjects();
        if (childObjects == null) return null;

        DBObjectRelationList<DBConstraintColumnRelation> relations = childObjects.getRelations(CONSTRAINT_COLUMN);
        if (relations == null) return null;

        for (DBConstraintColumnRelation relation : relations.getObjectRelations()) {
            DBColumn relationColumn = relation.getColumn();
            if (Objects.equals(relationColumn, this) && relation.getPosition() == position) {
                return relation.getConstraint();
            }
        }
        return null;
    }

    @Override
    @Nullable
    public DBColumn getForeignKeyColumn() {
        for (DBConstraint constraint : getConstraints()) {
            if (!constraint.isForeignKey()) continue;

            DBConstraint foreignKeyConstraint = constraint.getForeignKeyConstraint();
            if (foreignKeyConstraint == null) continue;

            short position = getConstraintPosition(constraint);
            return foreignKeyConstraint.getColumnForPosition(position);
        }
        return null;
    }

    @Override
    public List<DBColumn> getReferencingColumns() {
        assert isPrimaryKey();

        List<DBColumn> list = new ArrayList<>();
        boolean isSystemSchema = getDataset().getSchema().isSystemSchema();
        for (DBSchema schema : getObjectBundle().getSchemas()) {
            if (ProgressMonitor.isProgressCancelled()) {
                break;
            }
            if (schema.isSystemSchema() == isSystemSchema) {
                List<DBColumn> columns = schema.getForeignKeyColumns();
                for (DBColumn column : columns){
                    if (this.equals(column.getForeignKeyColumn())) {
                        list.add(column);
                    }
                }
            }
        }
        return list;
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        List<DBObjectNavigationList> navigationLists = new LinkedList<>();

        if (dataType.isDeclared()) {
            navigationLists.add(DBObjectNavigationList.create("Type", dataType.getDeclaredType()));
        }

        List<DBConstraint> constraints = getConstraints();
        if (constraints.size() > 0) {
            navigationLists.add(DBObjectNavigationList.create("Constraints", constraints));
        }

        if (getParentObject() instanceof DBTable) {
            List<DBIndex> indexes = getIndexes();
            if (indexes.size() > 0) {
                navigationLists.add(DBObjectNavigationList.create("Indexes", indexes));
            }

            if (isForeignKey()) {
                DBColumn foreignKeyColumn = getForeignKeyColumn();
                navigationLists.add(DBObjectNavigationList.create("Referenced column", foreignKeyColumn));
            }
        }

        if (isPrimaryKey()) {
            ObjectListProvider<DBColumn> objectListProvider = () -> getReferencingColumns();
            navigationLists.add(DBObjectNavigationList.create("Foreign-key columns", objectListProvider));
        }
        return navigationLists;
    }

    @Override
    public String getPresentableTextConditionalDetails() {
        return dataType.getQualifiedName();
    }

    @Override
    public List<PresentableProperty> getPresentableProperties() {
        List<PresentableProperty> properties = super.getPresentableProperties();

        if (isForeignKey()) {
            DBColumn foreignKeyColumn = getForeignKeyColumn();
            if (foreignKeyColumn != null) {
                properties.add(0, new DBObjectPresentableProperty("Foreign key column", foreignKeyColumn, true));
            }
        }

        StringBuilder attributes  = new StringBuilder();
        if (isIdentity()) attributes.append("IDENTITY");
        if (isPrimaryKey()) attributes.append(" PK");
        if (isForeignKey()) attributes.append(" FK");
        if (!isPrimaryKey() && !isNullable()) attributes.append(" not null");

        if (attributes.length() > 0) {
            properties.add(0, new SimplePresentableProperty("Attributes", attributes.toString().trim()));
        }
        properties.add(0, new DBDataTypePresentableProperty(dataType));

        return properties;
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof DBColumn)  {
            DBColumn column = (DBColumn) o;
            if (Objects.equals(getDataset(), column.getDataset())) {
                if (isPrimaryKey() && column.isPrimaryKey()) {
                    return super.compareTo(o);
                } else if (isPrimaryKey()) {
                    return -1;
                } else if (column.isPrimaryKey()){
                    return 1;
                } else {
                    return super.compareTo(o);
                }
            }
        }
        return super.compareTo(o);
    }
}
