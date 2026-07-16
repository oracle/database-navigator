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


import com.dbn.common.constant.Constant;
import com.dbn.common.util.Enumerations;

public enum DatabaseObjectTypeId implements Constant<DatabaseObjectTypeId> {
    AI_PROFILE,
    AI_MODEL,
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
    DATASOURCE_CONFIG,
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
    ENGINE,
    EVENT,
    FUNCTION,
    GRANTED_PRIVILEGE,
    GRANTED_ROLE,
    INDEX,
    INDEXTYPE,
    JAVA_METHOD,
    JAVA_PARAMETER,
    JAVA_FIELD,
    JAVA_INNER_CLASS,
    JAVA_CLASS,
    JAVA_RESOURCE,
    JAVA_PRIMITIVE,
    JSON_VIEW,
    LIBRARY,
    LOGFILE_GROUP,
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
    PARTITION_SET,
    PRIVILEGE,
    SYSTEM_PRIVILEGE,
    OBJECT_PRIVILEGE,
    PROCEDURE,
    PROFILE,
    POLICY,
    PROGRAM,
    RESOURCE_GROUP,
    ROLLBACK_SEGMENT,
    ROLE,
    SCHEMA,
    SEQUENCE,
    SERVER,
    SUBPARTITION,
    SYNONYM,
    TABLE,
    TABLESPACE,
    TABLESPACE_SET,
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
    ZONEMAP,


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
