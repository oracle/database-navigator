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


import com.dbn.common.util.Enumerations;

public enum DatabaseObjectTypeId {
    AI_PROFILE,
    ATTRIBUTE,
    ARGUMENT,
    CATEGORY,
    CERTIFICATE,
    CHARSET,
    CLUSTER,
    COLLATION,
    CONTEXT,
    CONNECTION,
    COLUMN,
    CONSTRAINT,
    CREDENTIAL,
    DATABASE,
    DATASET,
    DIRECTORY,
    DBLINK,
    DIMENSION,
    DIMENSION_ATTRIBUTE,
    DIMENSION_HIERARCHY,
    DIMENSION_LEVEL,
    DISKGROUP,
    DOMAIN,
    EDITION,
    FUNCTION,
    GRANTED_PRIVILEGE,
    GRANTED_ROLE,
    INDEX,
    INDEXTYPE,
    JAVA_METHOD,
    JAVA_PARAMETER,
    JAVA_CLASS,
    JAVA_OBJECT,
    LIBRARY,
    LOB,
    MATERIALIZED_VIEW,
    METHOD,
    MODEL,
    MINING_MODEL,
    NESTED_TABLE,
    NESTED_TABLE_COLUMN,
    OPERATOR,
    OUTLINE,
    PACKAGE,
    PACKAGE_FUNCTION,
    PACKAGE_PROCEDURE,
    PACKAGE_TYPE,
    PACKAGE_BODY,
    PARTITION,
    PRIVILEGE,
    SYSTEM_PRIVILEGE,
    OBJECT_PRIVILEGE,
    PROCEDURE,
    PROFILE,
    POLICY,
    PROGRAM,
    ROLLBACK_SEGMENT,
    ROLE,
    SCHEMA,
    SEQUENCE,
    SUBPARTITION,
    SYNONYM,
    TABLE,
    TABLESPACE,
    TRIGGER,
    DATASET_TRIGGER,
    DATABASE_TRIGGER,
    TYPE,
    TYPE_BODY,
    TYPE_ATTRIBUTE,
    TYPE_FUNCTION,
    TYPE_PROCEDURE,
    TYPE_TYPE,
    USER,
    VARRAY,
    VARRAY_TYPE,
    VIEW,


    XMLTYPE,
    CURSOR,
    RECORD,
    PROPERTY,
    JAVA,
    JAVA_LIB,
    PARAMETER,
    LABEL,
    CONSTANT,
    VARIABLE,
    SAVEPOINT,
    EXCEPTION,
    LANGUAGE,
    WINDOW,

    CONSOLE,
    UNKNOWN,
    NONE,
    ANY,
    BUNDLE,
    NON_EXISTENT,
    INCOMING_DEPENDENCY,
    OUTGOING_DEPENDENCY;

    public boolean isOneOf(DatabaseObjectTypeId ... objectTypeIds){
        return Enumerations.isOneOf(this, objectTypeIds);
    }
}
