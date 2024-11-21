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

import com.dbn.browser.DatabaseBrowserUtils;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.DBDataTypeDefinition;
import com.dbn.data.type.DBNativeDataType;
import com.dbn.database.common.metadata.def.DBDataTypeMetadata;
import com.dbn.database.common.metadata.def.DBTypeMetadata;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBSchema;
import com.dbn.object.DBType;
import com.dbn.object.DBTypeAttribute;
import com.dbn.object.DBTypeFunction;
import com.dbn.object.DBTypeProcedure;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectTreeNodeBase;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.properties.DBDataTypePresentableProperty;
import com.dbn.object.properties.PresentableProperty;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

class DBTypeImpl
        extends DBProgramImpl<DBTypeMetadata, DBTypeProcedure, DBTypeFunction, DBType>
        implements DBType {

    private String superTypeOwner;
    private String superTypeName;
    private DBObjectRef<DBType> superType;

    private DBDataTypeDefinition collectionElementTypeRef;
    private DBDataType collectionElementType;

    private DBNativeDataType nativeDataType;

    DBTypeImpl(DBSchemaObject parent, DBTypeMetadata metadata) throws SQLException {
        // type functions are not editable independently
        super(parent, metadata);
        assert this.getClass() != DBTypeImpl.class;
    }

    DBTypeImpl(DBSchema schema, DBTypeMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBTypeMetadata metadata) throws SQLException {
        String name = metadata.getTypeName();
        superTypeOwner = metadata.getSupertypeOwner();
        superTypeName = metadata.getSupertypeName();

        String typeCode = metadata.getTypeCode();
        boolean collection = metadata.isCollection();
        set(DBObjectProperty.COLLECTION, collection);

        nativeDataType = connection.getObjectBundle().getNativeDataType(typeCode);
        if (collection) {
            DBDataTypeMetadata collectionMetadata = metadata.getDataType().collection();
            collectionElementTypeRef = new DBDataTypeDefinition(collectionMetadata);
        }
        return name;
    }


    @Override
    protected void initLists(ConnectionHandler connection) {
        super.initLists(connection);
        if (!isCollection()) {
            DBObjectListContainer childObjects = ensureChildObjects();
            DBSchema schema = getSchema();
            childObjects.createSubcontentObjectList(DBObjectType.TYPE_ATTRIBUTE, this, schema);
            childObjects.createSubcontentObjectList(DBObjectType.TYPE_PROCEDURE, this, schema);
            childObjects.createSubcontentObjectList(DBObjectType.TYPE_FUNCTION, this, schema);
            childObjects.createSubcontentObjectList(DBObjectType.TYPE_TYPE, this, schema);
        }
    }

    @Override
    protected DBObjectType getFunctionObjectType() {
        return DBObjectType.TYPE_FUNCTION;
    }

    @Override
    protected DBObjectType getProcedureObjectType() {
        return DBObjectType.TYPE_PROCEDURE;
    }

    @Override
    protected DBObjectType getTypeObjectType() {
        return DBObjectType.TYPE_TYPE;
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.TYPE;
    }

    @Override
    @Nullable
    public Icon getIcon() {
        if (getStatus().is(DBObjectStatus.VALID)) {
            if (getStatus().is(DBObjectStatus.DEBUG))  {
                return Icons.DBO_TYPE_DEBUG;
            } else {
                return isCollection() ? Icons.DBO_TYPE_COLLECTION : Icons.DBO_TYPE;
            }
        } else {
            return isCollection() ? Icons.DBO_TYPE_COLLECTION_ERR : Icons.DBO_TYPE_ERR;
        }
    }

    @Override
    public Icon getOriginalIcon() {
        return isCollection() ? Icons.DBO_TYPE_COLLECTION : Icons.DBO_TYPE;
    }

    @Override
    public List<DBTypeAttribute> getAttributes() {
        return getChildObjects(DBObjectType.TYPE_ATTRIBUTE);
    }

    @Override
    public DBType getSuperType() {
        if (superType == null && superTypeOwner != null && superTypeName != null) {
            DBSchema schema = getObjectBundle().getSchema(superTypeOwner);
            DBType type = schema == null ? null : schema.getType(superTypeName);
            superType = DBObjectRef.of(type);
            superTypeOwner = null;
            superTypeName = null;
        }
        return DBObjectRef.get(superType);
    }

    @Override
    public DBDataType getCollectionElementType() {
        if (collectionElementType == null && collectionElementTypeRef != null) {
            collectionElementType = getObjectBundle().getDataTypes().getDataType(collectionElementTypeRef);
            collectionElementTypeRef = null;
        }
        return collectionElementType;
    }

    @Override
    @Nullable
    public DBObject getDefaultNavigationObject() {
        if (isCollection()) {
            DBDataType dataType = getCollectionElementType();
            if (dataType != null && dataType.isDeclared()) {
                return dataType.getDeclaredType();
            }

        }
        return null;
    }

    @Override
    public List<PresentableProperty> getPresentableProperties() {
        List<PresentableProperty> properties = super.getPresentableProperties();
        DBDataType collectionElementType = getCollectionElementType();
        if (collectionElementType != null) {
            properties.add(0, new DBDataTypePresentableProperty("Collection element type", collectionElementType));
        }
        return properties;
    }

    @Override
    public DBNativeDataType getNativeDataType() {
        return nativeDataType;
    }

    @Override
    public boolean isCollection() {
        return is(DBObjectProperty.COLLECTION);
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
        return isCollection() ?
                DBObjectTreeNodeBase.EMPTY_TREE_NODE_LIST :
                DatabaseBrowserUtils.createList(
                        getChildObjectList(DBObjectType.TYPE_ATTRIBUTE),
                        getChildObjectList(DBObjectType.TYPE_PROCEDURE),
                        getChildObjectList(DBObjectType.TYPE_FUNCTION),
                        getChildObjectList(DBObjectType.TYPE_TYPE));
    }

    @Override
    public boolean hasVisibleTreeChildren() {
        if (isCollection()) return false;

        ObjectTypeFilterSettings settings = getObjectTypeFilterSettings();
        return
            settings.isVisible(DBObjectType.ATTRIBUTE) ||
            settings.isVisible(DBObjectType.PROCEDURE) ||
            settings.isVisible(DBObjectType.FUNCTION);
    }

    @Override
    public String getCodeParseRootId(DBContentType contentType) {
        if (getParentObject() instanceof DBSchema) {
            return contentType == DBContentType.CODE_SPEC ? "type_spec" :
                   contentType == DBContentType.CODE_BODY ? "type_body" : null;
        }
        return null;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof DBType) {
            DBType that = (DBType) o;
            if (Objects.equals(this.getParentObject(), that.getParentObject())) {
                return this.isCollection() == that.isCollection() ?
                        super.compareTo(o) :
                        that.isCollection() ? -1 : 1;
            }
        }
        return super.compareTo(o);
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        List<DBObjectNavigationList> navigationLists = new LinkedList<>();

        DBType superType = getSuperType();
        if (superType != null) {
            navigationLists.add(DBObjectNavigationList.create("Super Type", superType));
        }
        List<DBObject> types = getChildObjects(DBObjectType.TYPE_TYPE);
        if (!types.isEmpty()) {
            navigationLists.add(DBObjectNavigationList.create("Sub Types", types));
        }
        if (isCollection()) {
            DBDataType dataType = getCollectionElementType();
            if (dataType != null && dataType.isDeclared()) {
                DBType collectionElementType = dataType.getDeclaredType();
                if (collectionElementType != null) {
                    navigationLists.add(DBObjectNavigationList.create("Collection element type", collectionElementType));
                }
            }
        }

        return navigationLists;
    }
}
