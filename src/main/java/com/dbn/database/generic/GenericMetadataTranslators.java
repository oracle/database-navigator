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
            switch (columnLabel) {
                case "SCHEMA_NAME": return schemaName;
                case "IS_PUBLIC": return literalBoolean(false);
                case "IS_SYSTEM": return literalBoolean("information_schema".equalsIgnoreCase(schemaName));
                case "IS_EMPTY": return literalBoolean(isEmpty(schemaName));
                default: return null;
            }
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
        public String getString(String columnLabel) throws SQLException {
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
        public String getString(String columnLabel) throws SQLException {
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
        public String getString(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "DATA_TYPE_NAME":
                    return resolve(
                            () -> {
                                int dataType = inner.getInt("DATA_TYPE");
                                return DATA_TYPE_NAMES.get().get(dataType);
                            },
                            () -> inner.getString("TYPE_NAME"));


                case "DECL_TYPE_NAME":   return null;
                case "DECL_TYPE_OWNER":   return null;
                case "DECL_TYPE_PROGRAM": return null;
                case "IS_SET":            return literalBoolean(false);
                default: return null;
            }
        }

        @Override
        public long getLong(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "DATA_LENGTH":    return resolve(() -> inner.getLong("COLUMN_SIZE"), () -> 0L);
                default: return 0;
            }
        }

        @Override
        public int getInt(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "DATA_PRECISION": return resolve(() -> inner.getInt("COLUMN_SIZE"), () -> 0);
                case "DATA_SCALE":     return resolve(() -> inner.getInt("DECIMAL_DIGITS"),() -> 0);
                default: return 0;
            }
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
        public String getString(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "COLUMN_NAME": return inner.getString("COLUMN_NAME");
                case "DATASET_NAME": return inner.getString("TABLE_NAME");
                case "IS_PRIMARY_KEY":
                    return literalBoolean(
                            isPrimaryKey(
                                    resolveOwner(inner, "TABLE_CAT", "TABLE_SCHEM"),
                                    inner.getString("TABLE_NAME"),
                                    inner.getString("COLUMN_NAME")));

                case "IS_FOREIGN_KEY":
                    return literalBoolean(
                            isForeignKey(
                                    resolveOwner(inner, "TABLE_CAT", "TABLE_SCHEM"),
                                    inner.getString("TABLE_NAME"),
                                    inner.getString("COLUMN_NAME")));

                case "IS_UNIQUE_KEY":
                    return literalBoolean(
                            isUniqueKey(
                                    resolveOwner(inner, "TABLE_CAT", "TABLE_SCHEM"),
                                    inner.getString("TABLE_NAME"),
                                    inner.getString("COLUMN_NAME")));

                case "IS_NULLABLE": {
                    boolean nullable = Objects.equals("YES", inner.getString("IS_NULLABLE"));
                    return literalBoolean(nullable);
                }
                case "IS_HIDDEN": return literalBoolean(false);
                case "IS_IDENTITY": return literalBoolean(false);
                default: return super.getString(columnLabel);
            }
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

        @Override
        public String getString(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "INDEX_NAME":
                    return resolve(
                            () -> inner.getString("INDEX_NAME"),
                            () -> inner.getString("TABLE_NAME") + "_INDEX_STATISTIC");

                case "TABLE_NAME": return inner.getString("TABLE_NAME");
                case "IS_UNIQUE": {
                    boolean unique = !resolve(
                            () -> inner.getBoolean("NON_UNIQUE"),
                            () -> true);
                    return literalBoolean(unique);
                }

                case "IS_VALID": return "Y";
                default: return null;
            }
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
        public String getString(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "INDEX_NAME":
                    return resolve(
                            () -> inner.getString("INDEX_NAME"),
                            () -> inner.getString("TABLE_NAME") + "_INDEX_STATISTIC");

                case "COLUMN_NAME": return inner.getString("COLUMN_NAME");
                case "TABLE_NAME":  return inner.getString("TABLE_NAME");

            }
            return super.getString(columnLabel);
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
        public String getString(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "CONSTRAINT_NAME":
                    return resolve(
                            () -> inner.getString("PK_NAME"),
                            () -> generateUniqueKeyName(inner.getString("TABLE_NAME"), null/*TODO what about multiple unique keys (find the additional discriminator)*/));


                case "CONSTRAINT_TYPE": {
                    String pkName = inner.getString("PK_NAME");
                    return pkName == null ? "UNIQUE" : "PRIMARY KEY";
                }

                case "DATASET_NAME": return inner.getString("TABLE_NAME");
                case "FK_CONSTRAINT_OWNER": return null;
                case "FK_CONSTRAINT_NAME": return null;
                case "CHECK_CONDITION": return "";
                case "IS_ENABLED": return "Y";
                default: return null;
            }

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
        public String getString(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "CONSTRAINT_NAME":
                    return resolve(
                            () -> inner.getString("PK_NAME"),
                            () -> generateUniqueKeyName(inner.getString("TABLE_NAME"), null/*TODO what about multiple unique keys (find the additional discriminator)*/));
                case "COLUMN_NAME": return inner.getString("COLUMN_NAME");
                case "DATASET_NAME": return inner.getString("TABLE_NAME");

            }
            return super.getString(columnLabel);
        }

        @Override
        public int getInt(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "POSITION": return inner.getInt("KEY_SEQ");
                default: return 0;
            }
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

        @Override
        public String getString(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "CONSTRAINT_NAME": {
                    return resolve(
                            () -> inner.getString("FK_NAME"),
                            () -> generateForeignKeyName(inner.getString("FKTABLE_NAME"), null));
                }

                case "CONSTRAINT_TYPE":     return "FOREIGN KEY";
                case "DATASET_NAME":        return inner.getString("FKTABLE_NAME");
                case "FK_CONSTRAINT_OWNER": return resolveOwner(inner, "PKTABLE_CAT", "PKTABLE_SCHEM");

                case "FK_CONSTRAINT_NAME":
                    return resolve(
                            () -> inner.getString("PK_NAME"),
                            () -> generateUniqueKeyName(
                                    inner.getString("PKTABLE_NAME"),
                                    inner.getString("PKCOLUMN_NAME")));

                case "CHECK_CONDITION": return "";
                case "IS_ENABLED":      return literalBoolean(true);
                default: return null;
            }
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

        @Override
        public String getString(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "CONSTRAINT_NAME":
                    return resolve(
                            () -> inner.getString("FK_NAME"),
                            () -> generateForeignKeyName(inner.getString("PKTABLE_NAME"), null));

                case "COLUMN_NAME":  return inner.getString("FKCOLUMN_NAME");
                case "DATASET_NAME": return inner.getString("FKTABLE_NAME");
                default:             return null;
            }
        }

        @Override
        public int getInt(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "POSITION": return inner.getInt("KEY_SEQ");
                default: return 0;
            }
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
            switch (columnLabel) {
                case "FUNCTION_NAME":
                case "PROCEDURE_NAME":   return resolveMethodName(inner);
                case "METHOD_TYPE":      return getMethodType();
                case "IS_VALID":         return literalBoolean(true);
                case "IS_DEBUG":
                case "IS_DETERMINISTIC": return literalBoolean(false);
                case "LANGUAGE":         return "SQL";
                case "TYPE_NAME":
                case "PACKAGE_NAME":
                default:                 return null;
            }
        }

        @Override
        public int getInt(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "OVERLOAD": return inner.getInt("METHOD_OVERLOAD");
                case "POSITION": return 0;
                default: return 0;
            }
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
        public String getString(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "ARGUMENT_NAME":
                    return resolve(
                            () -> emptyToNull(inner.getString("COLUMN_NAME")),
                            () -> "return");

                case "METHOD_NAME":  return resolveMethodName(inner);
                case "METHOD_TYPE":  return getMethodType(
                        resolveMethodName(inner),
                        inner.getString("SPECIFIC_NAME"));
                case "PROGRAM_NAME": return null;

                case "IN_OUT":
                    return resolve(
                            () -> {
                                int columnType = inner.getInt("COLUMN_TYPE");
                                switch (columnType) {
                                    case functionColumnUnknown: return "IN";
                                    case functionColumnIn: return "IN";
                                    case functionColumnOut: return "OUT";
                                    case functionColumnInOut: return "IN/OUT";
                                    case functionColumnResult: return "OUT";
                                    case functionReturn: return "OUT";

                                    //case procedureColumnUnknown: return "IN";
                                    //case procedureColumnIn: return "IN";
                                    //case procedureColumnOut: return "OUT";
                                    //case procedureColumnInOut: return "IN/OUT";
                                    //case procedureColumnResult: return "OUT";
                                    //case procedureColumnReturn: return "OUT";
                                    default: return "IN";
                                }
                            },
                            () -> "IN");

                default: return super.getString(columnLabel);
            }
        }
        @Override
        public int getInt(String columnLabel) throws SQLException {
            switch (columnLabel) {
                case "SEQUENCE":       return 0;
                case "OVERLOAD":       return getMethodOverload(
                        resolveMethodName(inner),
                        inner.getString("SPECIFIC_NAME"));
                case "POSITION":       return inner.getInt("ORDINAL_POSITION");
                default:            return super.getInt(columnLabel);
            }
        }

        abstract String getMethodType(String methodName, String methodSpecificName) throws SQLException;

        abstract int getMethodOverload(String methodName, String methodSpecificName) throws SQLException;
    }

    /**************************************************************
     *                    Static utilities                        *
     **************************************************************/
    static String generateUniqueKeyName(String tableName, String qualifier) {
        return "unq_" + tableName;
    }

    static String generateForeignKeyName(String tableName, String qualifier) {
        return "fk_" + tableName;
    }

    static String resolveOwner(ResultSet resultSet, String catalogCol, String schemaCol) throws SQLException {
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
            switch (procedureType) {
                case procedureNoResult: return "PROCEDURE";
                case procedureReturnsResult: return "FUNCTION";
                case procedureResultUnknown: return "PROCEDURE";
                default: return "PROCEDURE";
            }
        }

        if (source == MetadataSource.FUNCTIONS) {
            // getFunctions may return procedures
            String methodType = resolve(
                    () -> resultSet.getString("METHOD_TYPE"),
                    () -> resultSet.getString("FUNCTION_TYPE"),
                    () -> resultSet.getString("PROCEDURE_TYPE"),
                    () -> "0");
            int functionType = Integer.parseInt(methodType);
            switch (functionType) {
                case functionNoTable: return "FUNCTION";
                case functionReturnsTable: return "FUNCTION";
                case functionResultUnknown: return "FUNCTION";
                default: return "FUNCTION";
            }
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
