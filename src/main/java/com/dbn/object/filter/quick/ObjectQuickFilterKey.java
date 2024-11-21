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

package com.dbn.object.filter.quick;

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.connection.ConnectionId;
import com.dbn.object.DBSchema;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.type.DBObjectType;
import lombok.Data;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Data
class ObjectQuickFilterKey implements PersistentStateElement {
    private ConnectionId connectionId;
    private String schemaName;
    private DBObjectType objectType;

    public ObjectQuickFilterKey() {
    }

    private ObjectQuickFilterKey(DBObjectList<?> objectList) {
        connectionId = objectList.getConnection().getConnectionId();
        BrowserTreeNode treeParent = objectList.getParent();
        if (treeParent instanceof DBSchema) {
            schemaName = treeParent.getName();
        } else {
            schemaName = "";
        }
        objectType = objectList.getObjectType();
    }

    public static ObjectQuickFilterKey from(DBObjectList<?> objectList) {
        return new ObjectQuickFilterKey(objectList);
    }

    @Override
    public void readState(Element element) {
        connectionId = connectionIdAttribute(element, "connection-id");
        schemaName = stringAttribute(element, "schema");
        objectType = DBObjectType.get(stringAttribute(element, "object-type"));
    }

    @Override
    public void writeState(Element element) {
        element.setAttribute("connection-id", connectionId.id());
        element.setAttribute("schema", schemaName);
        element.setAttribute("object-type", objectType.getName());
    }
}
