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

package com.dbn.database.common;

import com.dbn.common.util.Strings;
import com.dbn.database.DatabaseObjectIdentifier;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;

import java.util.ArrayList;
import java.util.List;

public class DatabaseObjectIdentifierImpl implements DatabaseObjectIdentifier {
    private DBObjectType[] objectTypes;
    private String[] objectNames;

    public DatabaseObjectIdentifierImpl(DBObject object) {
        List<DBObject> chain = new ArrayList<>();
        chain.add(object);

        DBObject parentObject = object.getParentObject();
        while (parentObject != null) {
            chain.add(0, parentObject);
            parentObject = parentObject.getParentObject();
        }
        int length = chain.size();
        objectTypes = new DBObjectType[length];
        objectNames = new String[length];

        for (int i = 0; i<length; i++) {
            DBObject chainObject = chain.get(i);
            objectTypes[i] = chainObject.getObjectType();
            objectNames[i] = chainObject.getName();
        }
    }

    @Override
    public int getObjectTypeIndex(DBObjectType objectType) {
        for (int i=0; i< objectTypes.length; i++) {
            if (objectTypes[i] == objectType) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getObjectTypeIndex(DBObjectType[] objectTypes) {
        for (DBObjectType objectType : objectTypes) {
            int index = getObjectTypeIndex(objectType);
            if (index > -1) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public String getObjectName(DBObjectType objectType) {
        int index = getObjectTypeIndex(objectType);
        return index > -1 ? objectNames[index] : null;
    }

    @Override
    public String getObjectName(DBObjectType[] objectTypes) {
        int index = getObjectTypeIndex(objectTypes);
        return index > -1 ? objectNames[index] : null;
    }


    public DatabaseObjectIdentifierImpl(DBObjectType[] objectTypes, String[] objectNames) {
        this.objectNames = objectNames;
        this.objectTypes = objectTypes;
    }

    @Override
    public String[] getObjectNames() {
        return objectNames;
    }

    @Override
    public void setObjectNames(String[] objectNames) {
        this.objectNames = objectNames;
    }

    @Override
    public DBObjectType[] getObjectTypes() {
        return objectTypes;
    }

    @Override
    public void setObjectTypes(DBObjectType[] objectTypes) {
        this.objectTypes = objectTypes;
    }

    @Override
    public String getQualifiedType() {
        StringBuilder buffer = new StringBuilder();
        for (DBObjectType objectType : objectTypes) {
            if(buffer.length() > 0) {
                buffer.append('.');
            }
            String typeName = objectType.getName();
            buffer.append(typeName);
        }

        return buffer.toString();
    }

    @Override
    public String getQualifiedName() {
        StringBuilder buffer = new StringBuilder();
        for (String objectName : objectNames) {
            if(buffer.length() > 0) {
                buffer.append('.');
            }
            buffer.append(objectName);
        }

        return buffer.toString();
    }

    @Override
    public boolean matches(DBObject object) {
        int index = objectTypes.length - 1;
        while (object != null && index > -1) {
            if (object.getObjectType() == objectTypes[index] &&
                Strings.equalsIgnoreCase(object.getName(), objectNames[index])){
                object = object.getParentObject();
                index--;
            } else {
                return false;
            }
        }
        return true;
    }
}
