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
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.type.DBDataType;
import com.dbn.database.common.metadata.def.DBTypeAttributeMetadata;
import com.dbn.object.DBType;
import com.dbn.object.DBTypeAttribute;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectImpl;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.properties.DBDataTypePresentableProperty;
import com.dbn.object.properties.PresentableProperty;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

class DBTypeAttributeImpl extends DBObjectImpl<DBTypeAttributeMetadata> implements DBTypeAttribute {
    private DBDataType dataType;
    private short position;

    DBTypeAttributeImpl(DBType parent, DBTypeAttributeMetadata metadata) throws SQLException {
        super(parent, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBTypeAttributeMetadata metadata) throws SQLException {
        String name = metadata.getAttributeName();
        position = metadata.getPosition();
        dataType = DBDataType.get(connection, metadata.getDataType());
        return name;
    }


    @Override
    public short getPosition() {
        return position;
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.TYPE_ATTRIBUTE;
    }

    @Override
    public DBType getType() {
        return (DBType) getParentObject();        
    }

    @Override
    public DBDataType getDataType() {
        return dataType;
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, "type attribute", true);
        ttb.append(false, " - ", true);
        ttb.append(false, dataType.getQualifiedName(), true);

        ttb.createEmptyRow();
        super.buildToolTip(ttb);            
    }

    @Override
    public List<PresentableProperty> getPresentableProperties() {
        List<PresentableProperty> properties = super.getPresentableProperties();
        properties.add(0, new DBDataTypePresentableProperty(dataType));
        return properties;
    }

    @Override
    public String getPresentableTextConditionalDetails() {
        return dataType.getQualifiedName();
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        if (dataType.isDeclared()) {
            List<DBObjectNavigationList> navigationLists = new LinkedList<>();
            navigationLists.add(DBObjectNavigationList.create("Type", dataType.getDeclaredType()));
            return navigationLists;
        }
        return null;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof DBTypeAttribute) {
            DBTypeAttribute typeAttribute = (DBTypeAttribute) o;
            if (Objects.equals(getType(), typeAttribute.getType())) {
                return position - typeAttribute.getPosition();
            }
        }
        return super.compareTo(o);
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/

    @Override
    public boolean isLeaf() {
        return true;
    }

}
