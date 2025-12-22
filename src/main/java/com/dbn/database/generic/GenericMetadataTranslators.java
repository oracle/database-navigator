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

package com.dbn.database.generic;

import com.dbn.common.latent.Latent;
import com.dbn.common.util.Strings;
import com.dbn.database.common.metadata.impl.DBArgumentMetadataImpl;
import com.dbn.database.common.metadata.impl.DBColumnMetadataImpl;
import com.dbn.database.common.metadata.impl.DBConstraintColumnMetadataImpl;
import com.dbn.database.common.metadata.impl.DBConstraintMetadataImpl;
import com.dbn.database.common.metadata.impl.DBDataTypeMetadataImpl;
import com.dbn.database.common.metadata.impl.DBFunctionMetadataImpl;
import com.dbn.database.common.metadata.impl.DBIndexColumnMetadataImpl;
import com.dbn.database.common.metadata.impl.DBIndexMetadataImpl;
import com.dbn.database.common.metadata.impl.DBMethodMetadataImpl;
import com.dbn.database.common.metadata.impl.DBProcedureMetadataImpl;
import com.dbn.database.common.metadata.impl.DBSchemaMetadataImpl;
import com.dbn.database.common.metadata.impl.DBTableMetadataImpl;
import com.dbn.database.common.metadata.impl.DBViewMetadataImpl;
import com.dbn.database.common.util.CachedResultSet;
import com.dbn.database.common.util.CachedResultSetRow;
import com.dbn.database.common.util.WrappedCachedResultSet;
import com.dbn.database.interfaces.DatabaseInterface.Callable;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static java.sql.DatabaseMetaData.functionColumnIn;
import static java.sql.DatabaseMetaData.functionColumnInOut;
import static java.sql.DatabaseMetaData.functionColumnOut;
import static java.sql.DatabaseMetaData.functionColumnResult;
import static java.sql.DatabaseMetaData.functionColumnUnknown;
import static java.sql.DatabaseMetaData.functionNoTable;
import static java.sql.DatabaseMetaData.functionResultUnknown;
import static java.sql.DatabaseMetaData.functionReturn;
import static java.sql.DatabaseMetaData.functionReturnsTable;
import static java.sql.DatabaseMetaData.procedureNoResult;
import static java.sql.DatabaseMetaData.procedureResultUnknown;
import static java.sql.DatabaseMetaData.procedureReturnsResult;

@NonNls
public class GenericMetadataTranslators {
    private GenericMetadataTranslators() {}

    public static final Latent<Map<Integer, String>> DATA_TYPE_NAMES = Latent.basic(() -> initDataTypeNames());

    @NotNull
    @SneakyThrows
    static Map<Integer, String> initDataTypeNames() {
        Map<Integer, String> result = new HashMap<>();
        for (Field field : Types.class.getFields()) {
            result.put((Integer) field.get(null), field.getName());
        }
        return result;
    }

    enum MetadataSource {
        FUNCTIONS,
        PROCEDURES
    }


    /**
     * Metadata translation for SCHEMAS
     *  - from {@link java.sql.DatabaseMetaData#getSchemas(String, String)}
     *  - comply with {@link DBSchemaMetadataImpl}
     */
    public static abstract class SchemasResultSet extends WrappedCachedResultSet {
        SchemasResultSet(@Nullable CachedResultSet inner) {
            super(inner);
        }

        @Override
        public String getString(String columnLabel) throws SQLException {
            String schemaName = resolveOwner(inner, "TABLE_CAT", "TABLE_SCHEM");
            return switch (columnLabel) {
                case "SCHEMA_NAME" -> schemaName;
                case "IS_PUBLIC" -> literalBoolean(false);
                case "IS_SYSTEM" -> literalBoolean("information_schema".equalsIgnoreCase(schemaName));
                case "IS_EMPTY" -> literalBoolean(isEmpty(schemaName));
                default -> null;
            };
        }

        protected abstract boolean isEmpty(String schemaName) throws SQLException;
    }


    /**
     * Metadata translation for TABLES
     *  - from {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])}
     *  - comply with {@link DBTableMetadataImpl}
     */
    public static class TablesResultSet extends WrappedCachedResultSet {
        TablesResultSet(@Nullable CachedResultSet inner) {
            super(inner);
        }

        @Override
        public String getString(@NonNls String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "TABLE_NAME": return inner.getString("TABLE_NAME"); // redundant (for clarity)
                case "IS_TEMPORARY":
                    String tableType = inner.getString("TABLE_TYPE");
                    boolean temporary = tableType != null && Strings.containsIgnoreCase(tableType, "TEMPORARY");
                    return literalBoolean(temporary);

                default: return null;
            }
        }
    }

    /**
     * Metadata translation for VIEWS
     *  - from {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])}
     *  - comply with {@link DBViewMetadataImpl}
     */
    public static class ViewsResultSet extends WrappedCachedResultSet {
        ViewsResultSet(@Nullable CachedResultSet inner) {
            super(inner);
        }

        @Override
        public String getString(@NonNls String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "VIEW_NAME": return inner.getString("TABLE_NAME");

                case "IS_SYSTEM_VIEW": {
                    String tableType = inner.getString("TABLE_TYPE");
                    boolean systemView = tableType != null &&
                            Strings.containsIgnoreCase(tableType, "SYSTEM") &&
                            Strings.containsIgnoreCase(tableType, "VIEW");
                    return literalBoolean(systemView);
                }

                case "VIEW_TYPE": return null;
                case "VIEW_TYPE_OWNER": return null;
                default: return null;
            }
        }
    }

    /**
     * Abstract metadata translation for COLUMNS and ARGUMENTS
     *  - from {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)}
     *  - comply with {@link DBDataTypeMetadataImpl}
     */

    public static abstract class DataTypeResultSet extends WrappedCachedResultSet {
        DataTypeResultSet(@Nullable CachedResultSet inner) {
            super(inner);
        }

        @Override
        public String getString(@NonNls String columnLabel) throws SQLException {
            return switch (columnLabel) {
                case "DATA_TYPE_NAME" -> resolve(
                        () -> {
                            int dataType = inner.getInt("DATA_TYPE");
                            return DATA_TYPE_NAMES.get().get(dataType);
                        },
                        () -> inner.getString("TYPE_NAME"));
                case "DECL_TYPE_NAME" -> null;
                case "DECL_TYPE_OWNER" -> null;
                case "DECL_TYPE_PROGRAM" -> null;
                case "IS_SET" -> literalBoolean(false);
                default -> null;
            };
        }

        @Override
        public long getLong(@NonNls String columnLabel) throws SQLException {
            if (columnLabel.equals("DATA_LENGTH")) {
                return resolve(() -> inner.getLong("COLUMN_SIZE"), () -> 0L);
            }
            return 0;
        }

        @Override
        public int getInt(@NonNls String columnLabel) throws SQLException {
            return switch (columnLabel) {
                case "DATA_PRECISION" -> resolve(() -> inner.getInt("COLUMN_SIZE"), () -> 0);
                case "DATA_SCALE" -> resolve(() -> inner.getInt("DECIMAL_DIGITS"), () -> 0);
                default -> 0;
            };
        }
    }

    /**
     * Metadata translation for COLUMNS
     *  - from {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)}
     *  - comply with {@link DBColumnMetadataImpl}
     *            and {@link DBDataTypeMetadataImpl}
     *
     */
    public static abstract class ColumnsResultSet extends DataTypeResultSet {
        ColumnsResultSet(@Nullable CachedResultSet inner) {
            super(inner);
        }

        @Override
        public String getString(@NonNls String columnLabel) throws SQLException {
            return switch (columnLabel) {
                case "COLUMN_NAME" -> inner.getString("COLUMN_NAME");
                case "DATASET_NAME" -> inner.getString("TABLE_NAME");
                case "IS_PRIMARY_KEY" -> literalBoolean(
                        isPrimaryKey(
                                resolveOwner(inner, "TABLE_CAT", "TABLE_SCHEM"),
                                inner.getString("TABLE_NAME"),
                                inner.getString("COLUMN_NAME")));
                case "IS_FOREIGN_KEY" -> literalBoolean(
                        isForeignKey(
                                resolveOwner(inner, "TABLE_CAT", "TABLE_SCHEM"),
                                inner.getString("TABLE_NAME"),
                                inner.getString("COLUMN_NAME")));
                case "IS_UNIQUE_KEY" -> literalBoolean(
                        isUniqueKey(
                                resolveOwner(inner, "TABLE_CAT", "TABLE_SCHEM"),
                                inner.getString("TABLE_NAME"),
                                inner.getString("COLUMN_NAME")));
                case "IS_NULLABLE" -> {
                    boolean nullable = Objects.equals("YES", inner.getString("IS_NULLABLE"));
                    yield literalBoolean(nullable);
                }
                case "IS_HIDDEN" -> literalBoolean(false);
                case "IS_IDENTITY" -> literalBoolean(false);
                default -> super.getString(columnLabel);
            };
        }

        protected abstract boolean isPrimaryKey(String ownerName, String datasetName, String columnName) throws SQLException;

        protected abstract boolean isForeignKey(String ownerName, String datasetName, String columnName) throws SQLException;

        protected abstract boolean isUniqueKey(String ownerName, String datasetName, String columnName) throws SQLException;
    }


    /**
     * Metadata translation for INDEXES
     *  - from {@link java.sql.DatabaseMetaData#getIndexInfo(String, String, String, boolean, boolean)}
     *  - comply with {@link DBIndexMetadataImpl}
     */
    public static class IndexesResultSet extends WrappedCachedResultSet {
        IndexesResultSet(@Nullable CachedResultSet inner) {
            super(inner);
        }

        @NonNls
        @Override
        public String getString(@NonNls String columnLabel) throws SQLException {
            return switch (columnLabel) {
                case "INDEX_NAME" -> resolve(
                        () -> inner.getString("INDEX_NAME"),
                        () -> inner.getString("TABLE_NAME") + "_INDEX_STATISTIC");
                case "TABLE_NAME" -> inner.getString("TABLE_NAME");
                case "IS_UNIQUE" -> {
                    boolean unique = !resolve(
                            () -> inner.getBoolean("NON_UNIQUE"),
                            () -> true);
                    yield literalBoolean(unique);
                }
                case "IS_VALID" -> "Y";
                default -> null;
            };
        }
    }

    /**
     * Metadata translation for INDEX_COLUMN relations
     *  - from {@link java.sql.DatabaseMetaData#getIndexInfo(String, String, String, boolean, boolean)}
     *  - comply with {@link DBIndexColumnMetadataImpl}
     */
    public static class IndexColumnResultSet extends WrappedCachedResultSet {
        IndexColumnResultSet(@Nullable CachedResultSet inner) {
            super(inner);
        }

        @Override
        public String getString(@NonNls String columnLabel) throws SQLException {
            return switch (columnLabel) {
                case "INDEX_NAME" -> resolve(
                        () -> inner.getString("INDEX_NAME"),
                        () -> inner.getString("TABLE_NAME") + "_INDEX_STATISTIC");
                case "COLUMN_NAME" -> inner.getString("COLUMN_NAME");
                case "TABLE_NAME" -> inner.getString("TABLE_NAME");
                default -> super.getString(columnLabel);
            };
        }
    }

    /**
     * Metadata translation for PRIMARY KEYS
     *  - from {@link java.sql.DatabaseMetaData#getPrimaryKeys(String, String, String)}
     *  - comply with {@link DBConstraintMetadataImpl}
     */
    public static class PrimaryKeysResultSet extends WrappedCachedResultSet {
        PrimaryKeysResultSet(@Nullable CachedResultSet inner) {
            super(inner);
        }

        @Override
        public String getString(@NonNls String columnLabel) throws SQLException {
            return switch (columnLabel) {
                case "CONSTRAINT_NAME" -> resolve(
                        () -> inner.getString("PK_NAME"),
                        () -> generateUniqueKeyName(inner.getString("TABLE_NAME"), null/*TODO what about multiple unique keys (find the additional discriminator)*/));
                case "CONSTRAINT_TYPE" -> {
                    String pkName = inner.getString("PK_NAME");
                    yield pkName == null ? "UNIQUE" : "PRIMARY KEY";
                }
                case "DATASET_NAME" -> inner.getString("TABLE_NAME");
                case "FK_CONSTRAINT_OWNER" -> null;
                case "FK_CONSTRAINT_NAME" -> null;
                case "CHECK_CONDITION" -> "";
                case "IS_ENABLED" -> "Y";
                default -> null;
            };

        }
    }

    /**
     * Metadata translation for PRIMARY KEY COLUMN relations
     *  - from {@link java.sql.DatabaseMetaData#getPrimaryKeys(String, String, String)}
     *  - comply with {@link DBConstraintColumnMetadataImpl}
     */
    public static class PrimaryKeyRelationsResultSet extends WrappedCachedResultSet {
        PrimaryKeyRelationsResultSet(@Nullable CachedResultSet inner) {
            super(inner);
        }

        @Override
        public String getString(@NonNls String columnLabel) throws SQLException {
            return switch (columnLabel) {
                case "CONSTRAINT_NAME" -> resolve(
                        () -> inner.getString("PK_NAME"),
                        () -> generateUniqueKeyName(inner.getString("TABLE_NAME"), null/*TODO what about multiple unique keys (find the additional discriminator)*/));
                case "COLUMN_NAME" -> inner.getString("COLUMN_NAME");
                case "DATASET_NAME" -> inner.getString("TABLE_NAME");
                default -> super.getString(columnLabel);
            };
        }

        @Override
        public int getInt(String columnLabel) throws SQLException {
            if (columnLabel.equals("POSITION")) {
                return inner.getInt("KEY_SEQ");
            }
            return 0;
        }
    }

    /**
     * Metadata translation for FOREIGN KEYS
     *  - from {@link java.sql.DatabaseMetaData#getImportedKeys(String, String, String)}
     *  - comply with {@link DBConstraintMetadataImpl}
     */
    public static class ForeignKeysResultSet extends WrappedCachedResultSet {
        ForeignKeysResultSet(@Nullable CachedResultSet inner) {
            super(inner);
        }

        @NonNls
        @Override
        public String getString(@NonNls String columnLabel) throws SQLException {
            return switch (columnLabel) {
                case "CONSTRAINT_NAME" -> resolve(
                        () -> inner.getString("FK_NAME"),
                        () -> generateForeignKeyName(inner.getString("FKTABLE_NAME"), null));
                case "CONSTRAINT_TYPE" -> "FOREIGN KEY";
                case "DATASET_NAME" -> inner.getString("FKTABLE_NAME");
                case "FK_CONSTRAINT_OWNER" -> resolveOwner(inner, "PKTABLE_CAT", "PKTABLE_SCHEM");
                case "FK_CONSTRAINT_NAME" -> resolve(
                        () -> inner.getString("PK_NAME"),
                        () -> generateUniqueKeyName(
                                inner.getString("PKTABLE_NAME"),
                                inner.getString("PKCOLUMN_NAME")));
                case "CHECK_CONDITION" -> "";
                case "IS_ENABLED" -> literalBoolean(true);
                default -> null;
            };
        }
    }

    /**
     * Metadata translation for FOREIGN KEY COLUMN relations
     *  - from {@link java.sql.DatabaseMetaData#getImportedKeys(String, String, String)}
     *  - comply with {@link DBConstraintColumnMetadataImpl}
     */
    public static class ForeignKeyRelationsResultSet extends WrappedCachedResultSet {
        ForeignKeyRelationsResultSet(@Nullable CachedResultSet inner) {
            super(inner);
        }

        @NonNls
        @Override
        public String getString(@NonNls String columnLabel) throws SQLException {
            return switch (columnLabel) {
                case "CONSTRAINT_NAME" -> resolve(
                        () -> inner.getString("FK_NAME"),
                        () -> generateForeignKeyName(inner.getString("PKTABLE_NAME"), null));
                case "COLUMN_NAME" -> inner.getString("FKCOLUMN_NAME");
                case "DATASET_NAME" -> inner.getString("FKTABLE_NAME");
                default -> null;
            };
        }

        @Override
        public int getInt(String columnLabel) throws SQLException {
            if (columnLabel.equals("POSITION")) {
                return inner.getInt("KEY_SEQ");
            }
            return 0;
        }
    }

    /**
     * Metadata translation for PROCEDURES and FUNCTIONS
     *  - from {@link java.sql.DatabaseMetaData#getProcedures(String, String, String)}
     *     and {@link java.sql.DatabaseMetaData#getFunctions(String, String, String)}
     *  - comply with {@link DBProcedureMetadataImpl}
     *            and {@link DBFunctionMetadataImpl}
     *            and {@link DBMethodMetadataImpl}
     */
    public static abstract class MethodsResultSet extends WrappedCachedResultSet {
        MethodsResultSet(@Nullable CachedResultSet inner) {
            super(inner);
        }

        @Override
        public String getString(String columnLabel) throws SQLException {
            return switch (columnLabel) {
                case "FUNCTION_NAME",
                     "PROCEDURE_NAME" -> resolveMethodName(inner);
                case "METHOD_TYPE" -> getMethodType();
                case "IS_VALID" -> literalBoolean(true);
                case "IS_DEBUG",
                     "IS_DETERMINISTIC" -> literalBoolean(false);
                case "LANGUAGE" -> "SQL";
                default -> null;
            };
        }

        @Override
        public int getInt(String columnLabel) throws SQLException {
            return switch (columnLabel) {
                case "OVERLOAD" -> inner.getInt("METHOD_OVERLOAD");
                case "POSITION" -> 0;
                default -> 0;
            };
        }

        public abstract String getMethodType();
    }

    /**
     * Metadata translation for PROCEDURE and FUNCTION ARGUMENTS
     *  - from {@link java.sql.DatabaseMetaData#getProcedureColumns(String, String, String, String)}
     *     and {@link java.sql.DatabaseMetaData#getFunctionColumns(String, String, String, String)}
     *  - comply with {@link DBArgumentMetadataImpl}
     *            and {@link DBDataTypeMetadataImpl}
     */
    public static abstract class MethodArgumentsResultSet extends DataTypeResultSet {
        MethodArgumentsResultSet(@Nullable CachedResultSet inner) {
            super(inner);
        }
        @Override
        public String getString(@NonNls String columnLabel) throws SQLException {
            return switch (columnLabel) {
                case "ARGUMENT_NAME" -> resolve(
                        () -> emptyToNull(inner.getString("COLUMN_NAME")),
                        () -> "return");
                case "METHOD_NAME" -> resolveMethodName(inner);
                case "METHOD_TYPE" -> getMethodType(
                        resolveMethodName(inner),
                        inner.getString("SPECIFIC_NAME"));
                case "PROGRAM_NAME" -> null;
                case "IN_OUT" -> resolve(
                        () -> {
                            int columnType = inner.getInt("COLUMN_TYPE");
                            return switch (columnType) {
                                case functionColumnUnknown -> "IN";
                                case functionColumnIn -> "IN";
                                case functionColumnOut -> "OUT";
                                case functionColumnInOut -> "IN/OUT";
                                case functionColumnResult -> "OUT";
                                case functionReturn -> "OUT";

                                //case procedureColumnUnknown: return "IN";
                                //case procedureColumnIn: return "IN";
                                //case procedureColumnOut: return "OUT";
                                //case procedureColumnInOut: return "IN/OUT";
                                //case procedureColumnResult: return "OUT";
                                //case procedureColumnReturn: return "OUT";
                                default -> "IN";
                            };
                        },
                        () -> "IN");
                default -> super.getString(columnLabel);
            };
        }
        @Override
        public int getInt(String columnLabel) throws SQLException {
            return switch (columnLabel) {
                case "SEQUENCE" -> 0;
                case "OVERLOAD" -> getMethodOverload(
                        resolveMethodName(inner),
                        inner.getString("SPECIFIC_NAME"));
                case "POSITION" -> inner.getInt("ORDINAL_POSITION");
                default -> super.getInt(columnLabel);
            };
        }

        abstract String getMethodType(String methodName, String methodSpecificName) throws SQLException;

        abstract int getMethodOverload(String methodName, String methodSpecificName) throws SQLException;
    }

    /**************************************************************
     *                    Static utilities                        *
     **************************************************************/
    @NonNls
    static String generateUniqueKeyName(String tableName, String qualifier) {
        return "unq_" + tableName;
    }

    @NonNls
    static String generateForeignKeyName(String tableName, String qualifier) {
        return "fk_" + tableName;
    }

    static String resolveOwner(ResultSet resultSet, @NonNls String catalogCol,  @NonNls String schemaCol) throws SQLException {
        return resolve(
                () -> resultSet.getString(schemaCol),
                () -> resultSet.getString(catalogCol));
    }

    static String resolveOwner(CachedResultSetRow row, String catalogCol, String schemaCol) throws SQLException {
        return resolve(
                () -> (String) row.get(schemaCol),
                () -> (String) row.get(catalogCol));
    }

    static String resolveMethodName(ResultSet resultSet) throws SQLException {
        return resolve(
                () -> resultSet.getString("METHOD_NAME"),
                () -> resultSet.getString("SPECIFIC_NAME"));
    }

    @Nullable
    static String resolveMethodType(ResultSet resultSet, MetadataSource source) throws SQLException {
        if (source == MetadataSource.PROCEDURES) {
            // getProcedures may return functions
            String methodType = resolve(
                    () -> resultSet.getString("METHOD_TYPE"),
                    () -> resultSet.getString("PROCEDURE_TYPE"),
                    () -> resultSet.getString("FUNCTION_TYPE"),
                    () -> "0");
            int procedureType = Integer.parseInt(methodType);
            return switch (procedureType) {
                case procedureNoResult -> "PROCEDURE";
                case procedureReturnsResult -> "FUNCTION";
                case procedureResultUnknown -> "PROCEDURE";
                default -> "PROCEDURE";
            };
        }

        if (source == MetadataSource.FUNCTIONS) {
            // getFunctions may return procedures
            String methodType = resolve(
                    () -> resultSet.getString("METHOD_TYPE"),
                    () -> resultSet.getString("FUNCTION_TYPE"),
                    () -> resultSet.getString("PROCEDURE_TYPE"),
                    () -> "0");
            int functionType = Integer.parseInt(methodType);
            return switch (functionType) {
                case functionNoTable -> "FUNCTION";
                case functionReturnsTable -> "FUNCTION";
                case functionResultUnknown -> "FUNCTION";
                default -> "FUNCTION";
            };
        }
        return null;
    }


    @SafeVarargs
    static <T> T resolve(Callable<T>... resolvers) throws SQLException {
        for (int i = 0; i < resolvers.length; i++) {
            Callable<T> resolver = resolvers[i];
            try {
                T value = resolver.call();
                if (value != null) {
                    return value;
                }
            } catch (Throwable e) {
                conditionallyLog(e);
                if (i == resolvers.length -1) {
                    throw toSqlException(e, "Operation failed");
                }
            }
        }
        return null;
    }

    static String emptyToNull(String string) {
        return Strings.isEmpty(string) ? null : string.trim();
    }

    static String literalBoolean(boolean bool) {
        return bool ? "Y" : "N";
    }


}
