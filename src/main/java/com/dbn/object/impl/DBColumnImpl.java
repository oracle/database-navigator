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
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.grid.options.DataGridSettings;
import com.dbn.data.type.DBDataType;
import com.dbn.database.common.metadata.def.DBColumnMetadata;
import com.dbn.object.DBColumn;
import com.dbn.object.DBConstraint;
import com.dbn.object.DBDataset;
import com.dbn.object.DBIndex;
import com.dbn.object.DBType;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectImpl;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.list.DBObjectListImpl;
import com.dbn.object.common.list.DBObjectListProxy;
import com.dbn.object.common.list.DBObjectRelationList;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.dbn.object.common.property.DBObjectProperty.FOREIGN_KEY;
import static com.dbn.object.common.property.DBObjectProperty.HIDDEN;
import static com.dbn.object.common.property.DBObjectProperty.IDENTITY;
import static com.dbn.object.common.property.DBObjectProperty.NULLABLE;
import static com.dbn.object.common.property.DBObjectProperty.PRIMARY_KEY;
import static com.dbn.object.common.property.DBObjectProperty.UNIQUE_KEY;
import static com.dbn.object.type.DBObjectRelationType.COLUMN_COLUMN;
import static com.dbn.object.type.DBObjectRelationType.CONSTRAINT_COLUMN;
import static com.dbn.object.type.DBObjectRelationType.INDEX_COLUMN;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.CONSTRAINT;
import static com.dbn.object.type.DBObjectType.INDEX;
import static com.dbn.object.type.DBObjectType.TYPE_ATTRIBUTE;
import static java.util.Collections.emptyList;

@Getter
class DBColumnImpl extends DBObjectImpl<DBColumnMetadata> implements DBColumn {
    private DBDataType dataType;
    private short position;
    private String comments;

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
        comments = metadata.getComments();

        dataType = DBDataType.get(connection, metadata.getDataType());
        return name;
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        DBDataset dataset = getDataset();
        DBObjectListContainer childObjects = ensureChildObjects();
        childObjects.createSubcontentObjectList(CONSTRAINT, this, dataset, CONSTRAINT_COLUMN);
        childObjects.createSubcontentObjectList(INDEX, this, dataset, INDEX_COLUMN);


        initDeclaredType(childObjects);
    }

    private void initDeclaredType(DBObjectListContainer childObjects) {
        if (!dataType.isDeclared()) return;

        DBObjectListImpl<DBObject> empty = DBObjectListImpl.empty(TYPE_ATTRIBUTE, this);
            childObjects.addObjectList(DBObjectListProxy.create(() -> {
            DBType declaredType = dataType.getDeclaredType();
            if (declaredType == null) return empty;

            DBObjectList<DBObject> typeAttributes = declaredType.getChildObjectList(TYPE_ATTRIBUTE);
            return typeAttributes == null ? empty : typeAttributes;
        }));
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return COLUMN;
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
        if (!isPrimaryKey()) return emptyList();

        DBObjectListContainer childObjects = getDataset().getChildObjects();
        if (childObjects == null) return emptyList();

        DBObjectRelationList<DBColumnColumnRelation> relations = childObjects.getRelations(COLUMN_COLUMN);
        if (relations == null) return emptyList();

        List<DBColumn> list = new ArrayList<>();
        for (DBColumnColumnRelation relation : relations.getObjectRelations()) {
            if (this.equals(relation.getTargetColumn())) {
                list.add(relation.getSourceColumn());
            }
        }
        return list;
    }

    @Override
    public String getPresentableTextConditionalDetails() {
        return dataType.getQualifiedName();
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
        if (o instanceof DBColumn column)  {
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
