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

package com.dbn.database.common.metadata.impl;

import com.dbn.database.common.metadata.DBObjectMetadataBase;
import com.dbn.database.common.metadata.def.DBConstraintMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBConstraintMetadataImpl extends DBObjectMetadataBase implements DBConstraintMetadata {

    public DBConstraintMetadataImpl(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public String getConstraintName() throws SQLException {
        return getString("CONSTRAINT_NAME");
    }

    @Override
    public String getDatasetName() throws SQLException {
        return getString("DATASET_NAME");
    }

    @Override
    public String getConstraintType() throws SQLException {
        return getString("CONSTRAINT_TYPE");
    }


    @Override
    public String getCheckCondition() throws SQLException {
        return getString("CHECK_CONDITION");
    }

    @Override
    public String getFkConstraintOwner() throws SQLException {
        return getString("FK_CONSTRAINT_OWNER");
    }

    @Override
    public String getFkConstraintName() throws SQLException {
        return getString("FK_CONSTRAINT_NAME");
    }

    @Override
    public boolean isEnabled() throws SQLException {
        return isYesFlag("IS_ENABLED");
    }


/*
        String name = metadata.getString("CONSTRAINT_NAME");
        checkCondition = metadata.getString("CHECK_CONDITION");

        String typeString = metadata.getString("CONSTRAINT_TYPE");
        constraintType =
            typeString == null ? -1 :
            typeString.equals("CHECK")? DBConstraint.CHECK :
            typeString.equals("UNIQUE") ? DBConstraint.UNIQUE_KEY :
            typeString.equals("PRIMARY KEY") ? DBConstraint.PRIMARY_KEY :
            typeString.equals("FOREIGN KEY") ? DBConstraint.FOREIGN_KEY :
            typeString.equals("VIEW CHECK") ? DBConstraint.VIEW_CHECK :
            typeString.equals("VIEW READONLY") ? DBConstraint.VIEW_READONLY : -1;

        if (checkCondition == null && constraintType == CHECK) checkCondition = "";

        if (isForeignKey()) {
            String fkOwner = metadata.getString("FK_CONSTRAINT_OWNER");
            String fkName = metadata.getString("FK_CONSTRAINT_NAME");

            ConnectionHandler connection = getCache();
            DBSchema schema = connection.getObjectBundle().getSchema(fkOwner);
            if (schema != null) {
                DBObjectRef<DBSchema> schemaRef = schema.getRef();
                foreignKeyConstraint = new DBObjectRef<>(schemaRef, CONSTRAINT, fkName);
            }
        }
 */
}
