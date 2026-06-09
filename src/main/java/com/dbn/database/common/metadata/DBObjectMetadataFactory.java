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
import com.dbn.database.common.metadata.impl.DBAIModelMetaDataImpl;
import com.dbn.database.common.metadata.impl.DBAIProfileMetadataImpl;
import com.dbn.database.common.metadata.impl.DBArgumentMetadataImpl;
import com.dbn.database.common.metadata.impl.DBCharsetMetadataImpl;
import com.dbn.database.common.metadata.impl.DBClusterMetadataImpl;
import com.dbn.database.common.metadata.impl.DBColumnMetadataImpl;
import com.dbn.database.common.metadata.impl.DBConstraintColumnMetadataImpl;
import com.dbn.database.common.metadata.impl.DBConstraintMetadataImpl;
import com.dbn.database.common.metadata.impl.DBCredentialMetadataImpl;
import com.dbn.database.common.metadata.impl.DBDataSourceConfigEntryMetadataImpl;
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
import com.dbn.database.common.metadata.impl.DBJavaResourceMetadataImpl;
import com.dbn.database.common.metadata.impl.DBJsonViewMetadataImpl;
import com.dbn.database.common.metadata.impl.DBJsonViewTableMetadataImpl;
import com.dbn.database.common.metadata.impl.DBMaterializedViewMetadataImpl;
import com.dbn.database.common.metadata.impl.DBNestedTableMetadataImpl;
import com.dbn.database.common.metadata.impl.DBObjectDependencyMetadataImpl;
import com.dbn.database.common.metadata.impl.DBPackageMetadataImpl;
import com.dbn.database.common.metadata.impl.DBPrivilegeMetadataImpl;
import com.dbn.database.common.metadata.impl.DBProcedureMetadataImpl;
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
        if (contentType instanceof DBObjectType objectType) {
            metadata = (M) createMetadata(objectType, resultSet);

        } else if (contentType instanceof DBObjectRelationType relationType) {
            metadata = (M) createMetadata(relationType, resultSet);
        }

        if (metadata != null) {
            metadata = ObjectIdentifierMonitor.install(metadata, connection);
        }


        return metadata;
    }

    private DBObjectMetadata createMetadata(DBObjectType objectType, ResultSet resultSet) {
        return switch (objectType) {
            case USER -> new DBUserMetadataImpl(resultSet);
            case ROLE -> new DBRoleMetadataImpl(resultSet);
            case PRIVILEGE -> new DBPrivilegeMetadataImpl(resultSet);
            case SCHEMA -> new DBSchemaMetadataImpl(resultSet);
            case DBLINK -> new DBDatabaseLinkMetadataImpl(resultSet);
            case CHARSET -> new DBCharsetMetadataImpl(resultSet);
            case CLUSTER -> new DBClusterMetadataImpl(resultSet);
            case CREDENTIAL -> new DBCredentialMetadataImpl(resultSet);
            case DATA_SOURCE_CONFIG_ENTRY -> new DBDataSourceConfigEntryMetadataImpl(resultSet);
            case AI_PROFILE -> new DBAIProfileMetadataImpl(resultSet);
            case AI_MODEL -> new DBAIModelMetaDataImpl(resultSet);
            case OBJECT_PRIVILEGE -> new DBPrivilegeMetadataImpl(resultSet);
            case SYSTEM_PRIVILEGE -> new DBPrivilegeMetadataImpl(resultSet);
            case PROCEDURE -> new DBProcedureMetadataImpl(resultSet);
            case FUNCTION -> new DBFunctionMetadataImpl(resultSet);
            case TYPE -> new DBTypeMetadataImpl(resultSet);
            case TYPE_FUNCTION -> new DBFunctionMetadataImpl(resultSet);
            case TYPE_PROCEDURE -> new DBProcedureMetadataImpl(resultSet);
            case TYPE_ATTRIBUTE -> new DBTypeAttributeMetadataImpl(resultSet);
            case PACKAGE -> new DBPackageMetadataImpl(resultSet);
            case PACKAGE_TYPE -> new DBTypeMetadataImpl(resultSet);
            case PACKAGE_FUNCTION -> new DBFunctionMetadataImpl(resultSet);
            case PACKAGE_PROCEDURE -> new DBProcedureMetadataImpl(resultSet);
            case DIMENSION -> new DBDimensionMetadataImpl(resultSet);
            case VIEW -> new DBViewMetadataImpl(resultSet);
            case JSON_VIEW -> new DBJsonViewMetadataImpl(resultSet);
            case TABLE -> new DBTableMetadataImpl(resultSet);
            case NESTED_TABLE -> new DBNestedTableMetadataImpl(resultSet);
            case MATERIALIZED_VIEW -> new DBMaterializedViewMetadataImpl(resultSet);
            case SYNONYM -> new DBSynonymMetadataImpl(resultSet);
            case SEQUENCE -> new DBSequenceMetadataImpl(resultSet);
            case INDEX -> new DBIndexMetadataImpl(resultSet);
            case COLUMN -> new DBColumnMetadataImpl(resultSet);
            case CONSTRAINT -> new DBConstraintMetadataImpl(resultSet);
            case ARGUMENT -> new DBArgumentMetadataImpl(resultSet);
            case DATABASE_TRIGGER -> new DBTriggerMetadataImpl(resultSet);
            case DATASET_TRIGGER -> new DBTriggerMetadataImpl(resultSet);
            case JAVA_PRIMITIVE -> new DBJavaClassMetadataImpl(resultSet);
            case JAVA_CLASS -> new DBJavaClassMetadataImpl(resultSet);
            case JAVA_INNER_CLASS -> new DBJavaClassMetadataImpl(resultSet);
            case JAVA_FIELD -> new DBJavaFieldMetadataImpl(resultSet);
            case JAVA_METHOD -> new DBJavaMethodMetadataImpl(resultSet);
            case JAVA_PARAMETER -> new DBJavaParameterMetadataImpl(resultSet);
            case JAVA_RESOURCE -> new DBJavaResourceMetadataImpl(resultSet);
            case INCOMING_DEPENDENCY -> new DBObjectDependencyMetadataImpl(resultSet);
            case OUTGOING_DEPENDENCY -> new DBObjectDependencyMetadataImpl(resultSet);
            default -> throw new UnsupportedOperationException("No metadata provider defined for " + objectType);
        };
    }

    private DBObjectMetadata createMetadata(DBObjectRelationType relationType, ResultSet resultSet) {
        return switch (relationType) {
            case INDEX_COLUMN -> new DBIndexColumnMetadataImpl(resultSet);
            case CONSTRAINT_COLUMN -> new DBConstraintColumnMetadataImpl(resultSet);
            case JSON_VIEW_TABLE -> new DBJsonViewTableMetadataImpl(resultSet);
            case USER_ROLE -> new DBGrantedRoleMetadataImpl(resultSet);
            case USER_PRIVILEGE -> new DBGrantedPrivilegeMetadataImpl(resultSet);
            case ROLE_ROLE -> new DBGrantedRoleMetadataImpl(resultSet);
            case ROLE_PRIVILEGE -> new DBGrantedPrivilegeMetadataImpl(resultSet);
        };
    }


}
