package com.dbn.database.generic;

import com.dbn.common.util.Commons;
import com.dbn.common.util.Unsafe;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseCompatibility;
import com.dbn.database.JdbcProperty;
import com.dbn.database.common.DatabaseMetadataInterfaceImpl;
import com.dbn.database.common.util.CachedResultSet;
import com.dbn.database.common.util.CachedResultSet.Columns;
import com.dbn.database.common.util.CachedResultSetRow;
import com.dbn.database.common.util.MultipartResultSet;
import com.dbn.database.common.util.ResultSetCondition;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.language.common.QuotePair;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.cache.CacheKey.key;
import static com.dbn.database.common.util.CachedResultSet.Condition;
import static com.dbn.database.generic.GenericMetadataLoaders.loadAllColumnsRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadAllFunctionArgumentsRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadAllProcedureArgumentsRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadAllPseudoColumnsRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadCatalogsRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadColumnsRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadForeignKeysRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadFunctionArgumentsRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadFunctionsRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadIndexesRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadPrimaryKeysRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadProcedureArgumentsRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadProceduresRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadPseudoColumnsRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadSchemasRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadTablesRaw;
import static com.dbn.database.generic.GenericMetadataLoaders.loadViewsRaw;
import static com.dbn.database.generic.GenericMetadataTranslators.ColumnsResultSet;
import static com.dbn.database.generic.GenericMetadataTranslators.ForeignKeyRelationsResultSet;
import static com.dbn.database.generic.GenericMetadataTranslators.ForeignKeysResultSet;
import static com.dbn.database.generic.GenericMetadataTranslators.IndexColumnResultSet;
import static com.dbn.database.generic.GenericMetadataTranslators.IndexesResultSet;
import static com.dbn.database.generic.GenericMetadataTranslators.MetadataSource.FUNCTIONS;
import static com.dbn.database.generic.GenericMetadataTranslators.MetadataSource.PROCEDURES;
import static com.dbn.database.generic.GenericMetadataTranslators.MethodArgumentsResultSet;
import static com.dbn.database.generic.GenericMetadataTranslators.MethodsResultSet;
import static com.dbn.database.generic.GenericMetadataTranslators.PrimaryKeyRelationsResultSet;
import static com.dbn.database.generic.GenericMetadataTranslators.PrimaryKeysResultSet;
import static com.dbn.database.generic.GenericMetadataTranslators.SchemasResultSet;
import static com.dbn.database.generic.GenericMetadataTranslators.TablesResultSet;
import static com.dbn.database.generic.GenericMetadataTranslators.ViewsResultSet;
import static com.dbn.database.generic.GenericMetadataTranslators.resolveMethodType;
import static com.dbn.database.generic.GenericMetadataTranslators.resolveOwner;


public class GenericMetadataInterface extends DatabaseMetadataInterfaceImpl {

    // TODO review (ORACLE behavior - package methods come back with FUNCTION_CAT / PROCEDURE_CAT as package name)
    //  filtering them out for now

    private static final ResultSetCondition IS_FUNCTION    = rs -> Objects.equals("FUNCTION", resolveMethodType(rs, FUNCTIONS));
    private static final ResultSetCondition IS_FUNCTION_P  = rs -> Objects.equals("FUNCTION", resolveMethodType(rs, PROCEDURES));
    private static final ResultSetCondition IS_PROCEDURE   = rs -> Objects.equals("PROCEDURE", resolveMethodType(rs, PROCEDURES));
    private static final ResultSetCondition IS_PROCEDURE_F = rs -> Objects.equals("PROCEDURE", resolveMethodType(rs, FUNCTIONS));

    private static final Columns IDX_IDENTIFIER = () -> new String[]{
            "TABLE_CAT",
            "TABLE_SCHEM",
            "TABLE_NAME",
            "NON_UNIQUE",
            "INDEX_QUALIFIER",
            "INDEX_NAME",
            "TYPE"};

    private static final Columns PK_IDENTIFIER = () -> new String[]{
            "TABLE_CAT",
            "TABLE_SCHEM",
            "TABLE_NAME",
            "PK_NAME"};

    private static final Columns FK_IDENTIFIER = () -> new String[]{
            "PKTABLE_CAT",
            "PKTABLE_SCHEM",
            "PKTABLE_NAME",
            "FKTABLE_CAT",
            "FKTABLE_SCHEM",
            "FKTABLE_NAME",
            "FK_NAME",
            "PK_NAME"};

    private static final Columns METHOD_SIMPLE_IDENTIFIER = () -> new String[]{
            "METHOD_CAT",
            "METHOD_SCHEM",
            "METHOD_NAME"};

    private static final Columns METHOD_QUALIFIED_IDENTIFIER = () -> new String[]{
            "METHOD_CAT",
            "METHOD_SCHEM",
            "METHOD_NAME",
            "SPECIFIC_NAME"};

    private static final Columns METHOD_ARGUMENT_IDENTIFIER = () -> new String[]{
            "METHOD_CAT",
            "METHOD_SCHEM",
            "METHOD_NAME",
            "SPECIFIC_NAME",
            "COLUMN_NAME"};

    GenericMetadataInterface(DatabaseInterfaces provider) {
        super("generic_metadata_interface.xml", provider);
    }

    @Override
    public ResultSet loadCompileObjectErrors(String ownerName, String objectName, DBNConnection connection) {
        return null;
    }

    // TODO
    @Override
    public String createDateString(Date date) {
        String dateString = META_DATE_FORMAT.get().format(date);
        return "str_to_date('" + dateString + "', '%Y-%m-%d %T')";
    }

    @Override
    public boolean isValid(DBNConnection connection) {
        return false;
    }

    @Override
    public void setCurrentSchema(String schemaName, DBNConnection connection) {
        QuotePair quotePair = getInterfaces().getCompatibilityInterface().getDefaultIdentifierQuotes();
        String schema = quotePair.isQuoted(schemaName) ? quotePair.unquote(schemaName) : schemaName;
        Unsafe.silent(() -> {
            try {
                connection.setSchema(schema);
            } finally {
                if (!schema.equalsIgnoreCase(connection.getSchema())) {
                    connection.setCatalog(schema);
                }
            }
        });
    }



    @Override
    public ResultSet loadSchemas(DBNConnection connection) throws SQLException {
        CachedResultSet schemasRs = loadSchemasRaw(connection).open();
        if (schemasRs.isEmpty()) {
            CachedResultSet catalogsRs = loadCatalogsRaw(connection).open();
            getCompatibility().set(JdbcProperty.CATALOG_AS_OWNER, !catalogsRs.isEmpty());
            schemasRs = catalogsRs;
        }

        return new SchemasResultSet(schemasRs) {
            @Override
            protected boolean isEmpty(String schemaName) {
                return false;
/*
                TODO does not scale for large dbs (find another way or disable option)
                return
                    loadTablesRaw(schemaName, connection).isEmpty() &&
                    loadFunctionsRaw(schemaName, connection).isEmpty() &&
                    loadProceduresRaw(schemaName, connection).isEmpty();
*/
            }
        };
    }

    @Override
    public ResultSet loadTables(String ownerName, DBNConnection connection) throws SQLException {
        CachedResultSet tablesRs = loadTablesRaw(ownerName, connection).open();
        return new TablesResultSet(tablesRs);
    }

    @Override
    public ResultSet loadViews(String ownerName, DBNConnection connection) throws SQLException {
        CachedResultSet viewsRs = loadViewsRaw(ownerName, connection).open();
        return new ViewsResultSet(viewsRs);
    }

    @Override
    public ResultSet loadColumns(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        CachedResultSet columnsRs = loadColumnsRaw(ownerName, datasetName, connection).open();
        CachedResultSet pseudoColumnsRs = loadPseudoColumnsRaw(ownerName, datasetName, connection).open();

        return MultipartResultSet.create(
                createColumnsResultSet(columnsRs, connection),
                createColumnsResultSet(pseudoColumnsRs, connection));
    }

    @Override
    public ResultSet loadAllColumns(String ownerName, DBNConnection connection) throws SQLException {
        CachedResultSet columnsRs = loadAllColumnsRaw(ownerName, connection).open();
        CachedResultSet pseudoColumnsRs = loadAllPseudoColumnsRaw(ownerName, connection).open();

        return MultipartResultSet.create(
                createColumnsResultSet(columnsRs, connection),
                createColumnsResultSet(pseudoColumnsRs, connection));
    }

    @NotNull
    private static ColumnsResultSet createColumnsResultSet(CachedResultSet columnsRs, DBNConnection connection) {
        return new ColumnsResultSet(columnsRs) {
            @Override
            protected boolean isPrimaryKey(String ownerName, String datasetName, String columnName) throws SQLException {
                CachedResultSet primaryKeysRs = loadPrimaryKeysRaw(ownerName, datasetName, connection);
                return primaryKeysRs.exists(row ->
                        row.get("PK_NAME") != null &&
                        match(ownerName,   resolveOwner(row, "TABLE_CAT", "TABLE_SCHEM")) &&
                        match(datasetName, row.get("TABLE_NAME")) &&
                        match(columnName,  row.get("COLUMN_NAME")));
            }

            @Override
            protected boolean isForeignKey(String ownerName, String datasetName, String columnName) throws SQLException {
                CachedResultSet foreignKeysRs = loadForeignKeysRaw(ownerName, datasetName, connection);
                return foreignKeysRs.exists(row ->
                        match(ownerName,   resolveOwner(row, "FKTABLE_CAT", "FKTABLE_SCHEM")) &&
                        match(datasetName, row.get("FKTABLE_NAME")) &&
                        match(columnName,  row.get("FKCOLUMN_NAME")));
            }

            @Override
            protected boolean isUniqueKey(String ownerName, String datasetName, String columnName) throws SQLException {
                CachedResultSet primaryKeysRs = loadPrimaryKeysRaw(ownerName, datasetName, connection);
                return primaryKeysRs.exists(row ->
                        row.get("PK_NAME") == null &&
                                match(ownerName,   resolveOwner(row, "TABLE_CAT", "TABLE_SCHEM")) &&
                                match(datasetName, row.get("TABLE_NAME")) &&
                                match(columnName,  row.get("COLUMN_NAME")));
            }
        };
    }


    @Override
    public ResultSet loadIndexes(String ownerName, String tableName, DBNConnection connection) throws SQLException {
        CachedResultSet indexesRs = loadIndexesRaw(ownerName, tableName, connection).groupBy(IDX_IDENTIFIER);
        return new IndexesResultSet(indexesRs.open());
    }

    @Override
    public ResultSet loadAllIndexes(String ownerName, DBNConnection connection) throws SQLException {
        MultipartResultSet allIndexesRs = new MultipartResultSet();

        List<String> tableNames = loadTablesRaw(ownerName, connection).list("TABLE_NAME", String.class);
        for (String tableName : tableNames) {
            ResultSet indexesRs = loadIndexes(ownerName, tableName, connection);
            allIndexesRs.add(indexesRs);
        }

        return allIndexesRs;
    }

    @Override
    public ResultSet loadIndexRelations(String ownerName, String tableName, DBNConnection connection) throws SQLException {
        CachedResultSet indexesRs = loadIndexesRaw(ownerName, tableName, connection).open();
        return new IndexColumnResultSet(indexesRs);
    }

    @Override
    public ResultSet loadAllIndexRelations(String ownerName, DBNConnection connection) throws SQLException {
        MultipartResultSet allIndexRelationsRs = new MultipartResultSet();

        List<String> tableNames = loadTablesRaw(ownerName, connection).list("TABLE_NAME", String.class);
        for (String tableName : tableNames) {
            ResultSet indexRelationsRs = loadIndexRelations(ownerName, tableName, connection);
            allIndexRelationsRs.add(indexRelationsRs);
        }

        return allIndexRelationsRs;
    }

    @Override
    public ResultSet loadConstraints(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        CachedResultSet primaryKeysRs = loadPrimaryKeysRaw(ownerName, datasetName, connection).groupBy(PK_IDENTIFIER);
        CachedResultSet foreignKeysRs = loadForeignKeysRaw(ownerName, datasetName, connection).groupBy(FK_IDENTIFIER);

        return MultipartResultSet.create(
                new PrimaryKeysResultSet(primaryKeysRs.open()),
                new ForeignKeysResultSet(foreignKeysRs.open()));
    }


    @Override
    public ResultSet loadAllConstraints(String ownerName, DBNConnection connection) throws SQLException {
        MultipartResultSet allConstraintsRs = new MultipartResultSet();

        List<String> tableNames = loadTablesRaw(ownerName, connection).list("TABLE_NAME", String.class);
        for (String tableName : tableNames) {
            ResultSet constraintsRs = loadConstraints(ownerName, tableName, connection);
            allConstraintsRs.add(constraintsRs);
        }
        return allConstraintsRs;
    }

    @Override
    public ResultSet loadConstraintRelations(String ownerName, String datasetName, DBNConnection connection) throws SQLException {
        CachedResultSet primaryKeysRs = loadPrimaryKeysRaw(ownerName, datasetName, connection).open();
        CachedResultSet foreignKeysRs = loadForeignKeysRaw(ownerName, datasetName, connection).open();

        return MultipartResultSet.create(
                new PrimaryKeyRelationsResultSet(primaryKeysRs),
                new ForeignKeyRelationsResultSet(foreignKeysRs));
    }

    @Override
    public ResultSet loadAllConstraintRelations(String ownerName, DBNConnection connection) throws SQLException {
        MultipartResultSet allConstraintRelationsRs = new MultipartResultSet();

        List<String> tableNames = loadTablesRaw(ownerName, connection).list("TABLE_NAME", String.class);
        for (String tableName : tableNames) {
            ResultSet constraintRelationsRs = loadConstraintRelations(ownerName, tableName, connection);
            allConstraintRelationsRs.add(constraintRelationsRs);
        }

        return allConstraintRelationsRs;
    }

    @Override
    public ResultSet loadFunctions(String ownerName, DBNConnection connection) throws SQLException {
        CachedResultSet functionsRs = loadMethodsRaw(ownerName, connection, "FUNCTION");
        return createMethodsResultSet(functionsRs.open(), "FUNCTION");
    }

    @Override
    public ResultSet loadProcedures(String ownerName, DBNConnection connection) throws SQLException {
        CachedResultSet proceduresRs = loadMethodsRaw(ownerName, connection, "PROCEDURE");
        return createMethodsResultSet(proceduresRs.open(), "PROCEDURE");
    }

    @NotNull
    private MethodsResultSet createMethodsResultSet(CachedResultSet functionsRs, String methodType) {
        return new MethodsResultSet(functionsRs) {

            @Override
            public String getMethodType() {
                return methodType;
            }
        };
    }

    @Override
    public ResultSet loadMethodArguments(String ownerName, String methodName, String methodType, short overload, DBNConnection connection) throws SQLException {
        CachedResultSet functionArgumentsRs = loadFunctionArgumentsRaw(ownerName, methodName, connection);
        CachedResultSet procedureArgumentsRs = loadProcedureArgumentsRaw(ownerName, methodName, connection);
        procedureArgumentsRs = procedureArgumentsRs.filter(Condition.notIn(functionArgumentsRs, METHOD_ARGUMENT_IDENTIFIER));

        CachedResultSet methodArgumentsRs = functionArgumentsRs.unionAll(procedureArgumentsRs);
        return createArgumentsResultSet(methodArgumentsRs.open(), ownerName, connection);
    }

    @Override
    public ResultSet loadAllMethodArguments(String ownerName, DBNConnection connection) throws SQLException {
        CachedResultSet functionArgumentsRs = loadAllFunctionArgumentsRaw(ownerName, connection);
        CachedResultSet procedureArgumentsRs = loadAllProcedureArgumentsRaw(ownerName, connection);
        procedureArgumentsRs = procedureArgumentsRs.filter(Condition.notIn(functionArgumentsRs, METHOD_ARGUMENT_IDENTIFIER));

        CachedResultSet methodArgumentsRs = functionArgumentsRs.unionAll(procedureArgumentsRs);
        return createArgumentsResultSet(methodArgumentsRs.open(), ownerName, connection);
    }

    @NotNull
    private static MethodArgumentsResultSet createArgumentsResultSet(CachedResultSet argumentsRs, String ownerName, DBNConnection connection) {
        return new MethodArgumentsResultSet(argumentsRs) {
            @Override
            String getMethodType(String methodName, String methodSpecificName) throws SQLException {
                CachedResultSetRow function = findMethod(ownerName, methodName, methodSpecificName, "FUNCTION", connection);
                if (function != null) {
                    return "FUNCTION";
                }

                CachedResultSetRow procedure = findMethod(ownerName, methodName, methodSpecificName, "PROCEDURE", connection);
                if (procedure != null) {
                    return "PROCEDURE";
                }

                return null;
            }

            @Override
            int getMethodOverload(String methodName, String methodSpecificName) throws SQLException {
                CachedResultSetRow function = findMethod(ownerName, methodName, methodSpecificName, "FUNCTION", connection);
                if (function != null) {
                    return (int) function.get("METHOD_OVERLOAD");
                }

                CachedResultSetRow procedure = findMethod(ownerName, methodName, methodSpecificName, "PROCEDURE", connection);
                if (procedure != null) {
                    return (int) procedure.get("METHOD_OVERLOAD");
                }

                return 0;
            }
        };
    }

    private static CachedResultSetRow findMethod(String ownerName, String methodName, String methodSpecificName, String methodType, DBNConnection connection) throws SQLException {
        CachedResultSet functionsRs = loadMethodsRaw(ownerName, connection, methodType);
        return functionsRs.first(row ->
                match(ownerName, resolveOwner(row, "METHOD_CAT", "METHOD_SCHEM")) &&
                        match(methodName, row.get("METHOD_NAME")) &&
                        match(methodSpecificName, row.get("SPECIFIC_NAME")));
    }


    private static CachedResultSet loadMethodsRaw(String ownerName, DBNConnection connection, String methodType) throws SQLException {
        switch (methodType) {
            case "FUNCTION":
                return DatabaseInterfaceInvoker.cached(
                        key("UNSCRAMBLED_FUNCTIONS", ownerName),
                        () -> {
                            CachedResultSet functionsRs = loadFunctionsRaw(ownerName, connection);
                            functionsRs = functionsRs.filter(IS_FUNCTION);

                            CachedResultSet proceduresRs = loadProceduresRaw(ownerName, connection);
                            proceduresRs = proceduresRs.filter(IS_FUNCTION_P);
                            proceduresRs = proceduresRs.filter(Condition.notIn(functionsRs, METHOD_QUALIFIED_IDENTIFIER));

                            CachedResultSet methodsRs = functionsRs.unionAll(proceduresRs);
                            methodsRs = methodsRs.enrich("METHOD_OVERLOAD", methodOverloadEnricher());

                            return methodsRs;
                        });

            case "PROCEDURE":
                return DatabaseInterfaceInvoker.cached(
                        key("UNSCRAMBLED_PROCEDURES", ownerName),
                        () -> {
                            CachedResultSet proceduresRs = loadProceduresRaw(ownerName, connection);
                            proceduresRs = proceduresRs.filter(IS_PROCEDURE);

                            CachedResultSet functionsRs = loadFunctionsRaw(ownerName, connection);
                            functionsRs = functionsRs.filter(IS_PROCEDURE_F);
                            functionsRs = functionsRs.filter(Condition.notIn(proceduresRs, METHOD_QUALIFIED_IDENTIFIER));

                            CachedResultSet methodsRs = proceduresRs.unionAll(functionsRs);
                            methodsRs = methodsRs.enrich("METHOD_OVERLOAD", methodOverloadEnricher());

                            return methodsRs;
                        });
        }
        throw new IllegalArgumentException("Method type " + methodType + " not supported");
    }

    @NotNull
    private static CachedResultSet.ColumnValue methodOverloadEnricher() {
        return (resultSet, index) -> {
            CachedResultSetRow currentRow = resultSet.rowAt(index);
            int count = resultSet.count(row -> row.matches(currentRow, METHOD_SIMPLE_IDENTIFIER));
            if (count > 1) {
                CachedResultSetRow previousRow = resultSet.previous(row -> row.matches(currentRow, METHOD_SIMPLE_IDENTIFIER), index);
                int previousOverload = previousRow == null ? 0 : (int) previousRow.get("METHOD_OVERLOAD");
                return previousOverload + 1;
            }
            return 0;
        };
    }

    static DatabaseCompatibility getCompatibility() {
        return ConnectionHandler.local().getCompatibility();
    }

    private static boolean match(Object value1, Object value2) {
        return Commons.match(value1, value2);
    }
}
