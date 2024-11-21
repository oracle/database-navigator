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

import com.dbn.connection.ConnectionHandler;
import com.dbn.data.value.ComplexValue;
import com.dbn.database.common.metadata.def.DBDataTypeMetadata;
import com.dbn.object.DBType;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Getter;
import lombok.Setter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static com.dbn.common.util.Strings.toLowerCase;

@Getter
@Setter
public class DBDataType {
    private DBNativeDataType nativeType;
    private DBObjectRef<DBType> declaredType;
    private String name;
    private String qualifiedName;
    private long length;
    private int precision;
    private int scale;
    private boolean set;

    public static DBDataType get(ConnectionHandler connection, DBDataTypeMetadata metadata) throws SQLException {
        DBDataTypeDefinition definition = new DBDataTypeDefinition(metadata);
        return connection.getObjectBundle().getDataTypes().getDataType(definition);
    }

    public static DBDataType get(ConnectionHandler connection, String dataTypeName, long length, int precision, int scale, boolean set) {
        String declaredTypeName = null;
        String declaredTypeOwner = null;
        String declaredTypePackage = null;
        if (dataTypeName.contains(".")) {
            String[] nameChain = dataTypeName.split("\\.");
            if (nameChain.length == 1) {
                declaredTypeName = nameChain[0];
            } else if (nameChain.length == 2) {
                declaredTypeOwner = nameChain[0];
                declaredTypeName = nameChain[1];
            } else if (nameChain.length == 3) {
                declaredTypeOwner = nameChain[0];
                declaredTypePackage = nameChain[1];
                declaredTypeName = nameChain[2];
            }
        }
        DBDataTypeDefinition definition = new DBDataTypeDefinition(dataTypeName, declaredTypeName, declaredTypeOwner, declaredTypePackage, length, precision, scale, set);
        return connection.getObjectBundle().getDataTypes().getDataType(definition);
    }

    public DBDataType() {}

    public DBType getDeclaredType() {
        return DBObjectRef.get(declaredType);
    }

    public boolean isDeclared() {
        return declaredType != null;
    }

    public boolean isNative() {
        return nativeType != null;
    }

    public boolean isPurelyDeclared() {
        return isDeclared() && !isNative();
    }

    public boolean isPurelyNative() {
        return isNative() && !isDeclared();
    }

    public boolean isNativeDeclared() {
        return nativeType != null && declaredType != null;
    }

    public String getName() {
        return (set ? "set of " : "") +
                (nativeType == null && declaredType == null ? name :
                 nativeType == null ? declaredType.getQualifiedName() :
                 nativeType.getName());
    }

    public Class getTypeClass() {
        return nativeType == null ? Object.class : nativeType.getDefinition().getTypeClass();
    }

    public int getSqlType() {
        return nativeType == null ? Types.CHAR : nativeType.getSqlType();
    }

    public Object getValueFromResultSet(ResultSet resultSet, int columnIndex) throws SQLException {
        if (nativeType != null) {
            return nativeType.getValueFromResultSet(resultSet, columnIndex);
        } else {
            return new ComplexValue(resultSet, columnIndex);
        }
    }

    public void setValueToResultSet(ResultSet resultSet, int columnIndex, Object value) throws SQLException {
        if (nativeType != null) {
            nativeType.setValueToResultSet(resultSet, columnIndex, value);
        }
    }

    public void setValueToPreparedStatement(PreparedStatement preparedStatement, int index, Object value) throws SQLException {
        if (nativeType != null) {
            nativeType.setValueToStatement(preparedStatement, index, value);
        }
    }

    public String getQualifiedName() {
        if (qualifiedName == null) {
            StringBuilder buffer = new StringBuilder();
            String name = getName();
            buffer.append(name == null ? "" : toLowerCase(name));
            if (precision > 0) {
                buffer.append(" (");
                buffer.append(precision);
                if (scale > 0) {
                    buffer.append(", ");
                    buffer.append(scale);
                }
                buffer.append(')');
            } else if (length > 0) {
                buffer.append(" (");
                buffer.append(length);
                buffer.append(')');
            }
            qualifiedName = buffer.toString();
        }
        return qualifiedName;
    }

    @Override
    public String toString() {
        return getName();
    }

    public GenericDataType getGenericDataType() {
        return nativeType != null ? nativeType.getGenericDataType() : GenericDataType.OBJECT;
    }

    public String getContentTypeName() {
        return nativeType == null ? null : nativeType.getDefinition().getContentTypeName();
    }

}