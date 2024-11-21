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

package com.dbn.data.type;

import com.dbn.database.common.metadata.def.DBDataTypeMetadata;
import lombok.Data;

import java.sql.SQLException;

@Data
public class DBDataTypeDefinition {
    private final String dataTypeName;
    private final String declaredTypeName;
    private final String declaredTypeOwner;
    private final String declaredTypeProgram;
    private final long length;
    private final int precision;
    private final int scale;
    private final boolean set;

    public DBDataTypeDefinition(DBDataTypeMetadata metadata) throws SQLException {
        this.dataTypeName = metadata.getDataTypeName();
        this.declaredTypeName = metadata.getDeclaredTypeName();
        this.declaredTypeOwner = metadata.getDeclaredTypeOwner();
        this.declaredTypeProgram = metadata.getDeclaredTypeProgram();

        this.length = metadata.getDataLength();
        this.precision = metadata.getDataPrecision();
        this.scale = metadata.getDataScale();
        this.set = metadata.isSet();
    }

    DBDataTypeDefinition(String dataTypeName, String declaredTypeName, String declaredTypeOwner, String declaredTypeProgram, long length, int precision, int scale, boolean set) {
        this.dataTypeName = dataTypeName;
        this.declaredTypeName = declaredTypeName;
        this.declaredTypeOwner = declaredTypeOwner;
        this.declaredTypeProgram = declaredTypeProgram;

        this.length = length;
        this.precision = precision;
        this.scale = scale;
        this.set = set;
    }
}
