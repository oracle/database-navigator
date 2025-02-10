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

package com.dbn.database.common.metadata;

import com.dbn.common.content.DynamicContentType;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.metadata.impl.DBArgumentMetadataImpl;
import com.dbn.database.common.metadata.impl.DBCharsetMetadataImpl;
import com.dbn.database.common.metadata.impl.DBClusterMetadataImpl;
import com.dbn.database.common.metadata.impl.DBColumnMetadataImpl;
import com.dbn.database.common.metadata.impl.DBConstraintColumnMetadataImpl;
import com.dbn.database.common.metadata.impl.DBConstraintMetadataImpl;
import com.dbn.database.common.metadata.impl.DBCredentialMetadataImpl;
import com.dbn.database.common.metadata.impl.DBDatabaseLinkMetadataImpl;
import com.dbn.database.common.metadata.impl.DBDimensionMetadataImpl;
import com.dbn.database.common.metadata.impl.DBFunctionMetadataImpl;
import com.dbn.database.common.metadata.impl.DBGrantedPrivilegeMetadataImpl;
import com.dbn.database.common.metadata.impl.DBGrantedRoleMetadataImpl;
import com.dbn.database.common.metadata.impl.DBIndexColumnMetadataImpl;
import com.dbn.database.common.metadata.impl.DBIndexMetadataImpl;
import com.dbn.database.common.metadata.impl.DBJavaClassMetadataImpl;
import com.dbn.database.common.metadata.impl.DBJavaFieldMetadataImpl;
import com.dbn.database.common.metadata.impl.DBJavaMethodMetadataImpl;
import com.dbn.database.common.metadata.impl.DBJavaParameterMetadataImpl;
import com.dbn.database.common.metadata.impl.DBMaterializedViewMetadataImpl;
import com.dbn.database.common.metadata.impl.DBNestedTableMetadataImpl;
import com.dbn.database.common.metadata.impl.DBObjectDependencyMetadataImpl;
import com.dbn.database.common.metadata.impl.DBPackageMetadataImpl;
import com.dbn.database.common.metadata.impl.DBPrivilegeMetadataImpl;
import com.dbn.database.common.metadata.impl.DBProcedureMetadataImpl;
import com.dbn.database.common.metadata.impl.DBProfileMetadataImpl;
import com.dbn.database.common.metadata.impl.DBRoleMetadataImpl;
import com.dbn.database.common.metadata.impl.DBSchemaMetadataImpl;
import com.dbn.database.common.metadata.impl.DBSequenceMetadataImpl;
import com.dbn.database.common.metadata.impl.DBSynonymMetadataImpl;
import com.dbn.database.common.metadata.impl.DBTableMetadataImpl;
import com.dbn.database.common.metadata.impl.DBTriggerMetadataImpl;
import com.dbn.database.common.metadata.impl.DBTypeAttributeMetadataImpl;
import com.dbn.database.common.metadata.impl.DBTypeMetadataImpl;
import com.dbn.database.common.metadata.impl.DBUserMetadataImpl;
import com.dbn.database.common.metadata.impl.DBViewMetadataImpl;
import com.dbn.database.common.security.ObjectIdentifierMonitor;
import com.dbn.object.type.DBObjectRelationType;
import com.dbn.object.type.DBObjectType;

import java.sql.ResultSet;

public class DBObjectMetadataFactory {
    public static final DBObjectMetadataFactory INSTANCE = new DBObjectMetadataFactory();

    private DBObjectMetadataFactory() {}

    public <M extends DBObjectMetadata> M create(DynamicContentType contentType, ResultSet resultSet, DBNConnection connection) {
        M metadata = null;
        if (contentType instanceof DBObjectType) {
            DBObjectType objectType = (DBObjectType) contentType;
            metadata = (M) createMetadata(objectType, resultSet);

        } else if (contentType instanceof DBObjectRelationType) {
            DBObjectRelationType relationType = (DBObjectRelationType) contentType;
            metadata = (M) createMetadata(relationType, resultSet);
        }

        if (metadata != null) {
            metadata = ObjectIdentifierMonitor.install(metadata, connection);
        }


        return metadata;
    }

    private DBObjectMetadata createMetadata(DBObjectType objectType, ResultSet resultSet) {
        switch (objectType) {
            case USER:                return new DBUserMetadataImpl(resultSet);
            case ROLE:                return new DBRoleMetadataImpl(resultSet);
            case PRIVILEGE:           return new DBPrivilegeMetadataImpl(resultSet);
            case SCHEMA:              return new DBSchemaMetadataImpl(resultSet);
            case DBLINK:              return new DBDatabaseLinkMetadataImpl(resultSet);
            case CHARSET:             return new DBCharsetMetadataImpl(resultSet);
            case CLUSTER:             return new DBClusterMetadataImpl(resultSet);
            case CREDENTIAL:          return new DBCredentialMetadataImpl(resultSet);
            case AI_PROFILE:          return new DBProfileMetadataImpl(resultSet);
            case OBJECT_PRIVILEGE:    return new DBPrivilegeMetadataImpl(resultSet);
            case SYSTEM_PRIVILEGE:    return new DBPrivilegeMetadataImpl(resultSet);
            case PROCEDURE:           return new DBProcedureMetadataImpl(resultSet);
            case FUNCTION:            return new DBFunctionMetadataImpl(resultSet);
            case TYPE:                return new DBTypeMetadataImpl(resultSet);
            case TYPE_FUNCTION:       return new DBFunctionMetadataImpl(resultSet);
            case TYPE_PROCEDURE:      return new DBProcedureMetadataImpl(resultSet);
            case TYPE_ATTRIBUTE:      return new DBTypeAttributeMetadataImpl(resultSet);
            case PACKAGE:             return new DBPackageMetadataImpl(resultSet);
            case PACKAGE_TYPE:        return new DBTypeMetadataImpl(resultSet);
            case PACKAGE_FUNCTION:    return new DBFunctionMetadataImpl(resultSet);
            case PACKAGE_PROCEDURE:   return new DBProcedureMetadataImpl(resultSet);
            case DIMENSION:           return new DBDimensionMetadataImpl(resultSet);
            case VIEW:                return new DBViewMetadataImpl(resultSet);
            case TABLE:               return new DBTableMetadataImpl(resultSet);
            case NESTED_TABLE:        return new DBNestedTableMetadataImpl(resultSet);
            case MATERIALIZED_VIEW:   return new DBMaterializedViewMetadataImpl(resultSet);
            case SYNONYM:             return new DBSynonymMetadataImpl(resultSet);
            case SEQUENCE:            return new DBSequenceMetadataImpl(resultSet);
            case INDEX:               return new DBIndexMetadataImpl(resultSet);
            case COLUMN:              return new DBColumnMetadataImpl(resultSet);
            case CONSTRAINT:          return new DBConstraintMetadataImpl(resultSet);
            case ARGUMENT:            return new DBArgumentMetadataImpl(resultSet);
            case DATABASE_TRIGGER:    return new DBTriggerMetadataImpl(resultSet);
            case DATASET_TRIGGER:     return new DBTriggerMetadataImpl(resultSet);
            case JAVA_PRIMITIVE:      return new DBJavaClassMetadataImpl(resultSet);
            case JAVA_CLASS:          return new DBJavaClassMetadataImpl(resultSet);
            case JAVA_INNER_CLASS:    return new DBJavaClassMetadataImpl(resultSet);
            case JAVA_FIELD:          return new DBJavaFieldMetadataImpl(resultSet);
            case JAVA_METHOD:         return new DBJavaMethodMetadataImpl(resultSet);
            case JAVA_PARAMETER:      return new DBJavaParameterMetadataImpl(resultSet);
            case INCOMING_DEPENDENCY: return new DBObjectDependencyMetadataImpl(resultSet);
            case OUTGOING_DEPENDENCY: return new DBObjectDependencyMetadataImpl(resultSet);
        }
        throw new UnsupportedOperationException("No metadata provider defined for " + objectType);
    }

    private DBObjectMetadata createMetadata(DBObjectRelationType relationType, ResultSet resultSet) {
        switch (relationType) {
            case INDEX_COLUMN:      return new DBIndexColumnMetadataImpl(resultSet);
            case CONSTRAINT_COLUMN: return new DBConstraintColumnMetadataImpl(resultSet);
            case USER_ROLE:         return new DBGrantedRoleMetadataImpl(resultSet);
            case USER_PRIVILEGE:    return new DBGrantedPrivilegeMetadataImpl(resultSet);
            case ROLE_ROLE:         return new DBGrantedRoleMetadataImpl(resultSet);
            case ROLE_PRIVILEGE:    return new DBGrantedPrivilegeMetadataImpl(resultSet);
        }
        throw new UnsupportedOperationException("No metadata provider defined for " + relationType);
    }


}
