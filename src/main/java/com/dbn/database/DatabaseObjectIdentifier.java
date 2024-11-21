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

package com.dbn.database;

import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;

/**
 * Bundles all the information needed to resolve a database object.<br>
 * e.g. if the object represents a column then<br>
 * type = [DatabaseObjectTypes.SCHEMA, DatabaseObjectTypes.TABLE, DatabaseObjectTypes.COLUMN]<br>
 * name = ["SCHEMA_NAME", "TABLE_NAME", "COLUMN_NAME"]<br>
 * representing the column referred as SCHEMA_NAME.TABLE_NAME.COLUMN_NAME
 */
public interface DatabaseObjectIdentifier {
    String[] getObjectNames();

    void setObjectNames(String[] name);

    DBObjectType[] getObjectTypes();

    void setObjectTypes(DBObjectType[] objectTypes);

    String getQualifiedType();
    String getQualifiedName();

    boolean matches(DBObject object);

    String getObjectName(DBObjectType[] objectTypes);

    String getObjectName(DBObjectType objectType);

    int getObjectTypeIndex(DBObjectType[] objectTypes);

    int getObjectTypeIndex(DBObjectType objectType);
}
