package com.dbn.database.oracle;

import com.dbn.code.common.style.DBLCodeStyleManager;
import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.code.psql.style.PSQLCodeStyle;
import com.dbn.common.util.Strings;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseDataDefinitionInterfaceImpl;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.ddl.options.DDLFileSettings;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.factory.ArgumentFactoryInput;
import com.dbn.object.factory.MethodFactoryInput;
import com.intellij.openapi.project.Project;

import java.sql.SQLException;

import static com.dbn.common.util.Strings.cachedLowerCase;

public class OracleDataDefinitionInterface extends DatabaseDataDefinitionInterfaceImpl {
    public OracleDataDefinitionInterface(DatabaseInterfaces provider) {
        super("oracle_ddl_interface.xml", provider);
    }

    @Override
    public String createDDLStatement(Project project, DatabaseObjectTypeId objectTypeId, String userName, String schemaName, String objectName, DBContentType contentType, String code, String alternativeDelimiter) {
        DDLFileSettings ddlFileSettings = DDLFileSettings.getInstance(project);
        boolean useQualified = ddlFileSettings.getGeneralSettings().isUseQualifiedObjectNames();
        boolean makeRerunnable = ddlFileSettings.getGeneralSettings().isMakeScriptsRerunnable();

        CodeStyleCaseSettings styleCaseSettings = DBLCodeStyleManager.getInstance(project).getCodeStyleCaseSettings(SQLLanguage.INSTANCE);
        CodeStyleCaseOption kco = styleCaseSettings.getKeywordCaseOption();
        CodeStyleCaseOption oco = styleCaseSettings.getObjectCaseOption();

        if (objectTypeId.isOneOf(DatabaseObjectTypeId.DATABASE_TRIGGER, DatabaseObjectTypeId.DATASET_TRIGGER)) {
            objectTypeId = DatabaseObjectTypeId.TRIGGER;
        }

        if (objectTypeId == DatabaseObjectTypeId.VIEW) {
            return kco.format("create" + (makeRerunnable ? " or replace" : "") + " view ") + oco.format((useQualified ? schemaName + "." : "") + objectName) + kco.format(" as\n") + code + "\n/";
        } else {
            String objectType = cachedLowerCase(objectTypeId.toString());
            if (contentType == DBContentType.CODE_BODY) {
                objectType = objectType + " body";
            }
            code = updateNameQualification(code, useQualified, objectType, schemaName, objectName, styleCaseSettings);
            return kco.format("create" + (makeRerunnable ? " or replace" : "") + " ") + code + "\n/";
        }
    }

    @Override
    public void computeSourceCodeOffsets(SourceCodeContent content, DatabaseObjectTypeId objectTypeId, String objectName) {
        String sourceCode = content.getText().toString();
        if (Strings.isEmpty(sourceCode)) return;

        if (objectTypeId == DatabaseObjectTypeId.DATASET_TRIGGER || objectTypeId == DatabaseObjectTypeId.DATABASE_TRIGGER) {
            if (!sourceCode.isEmpty()) {
                int startIndex = Strings.indexOfIgnoreCase(sourceCode, objectName, 0) + objectName.length();
                int headerEndOffset = Strings.indexOfIgnoreCase(sourceCode, "declare", startIndex);
                if (headerEndOffset == -1) headerEndOffset = Strings.indexOfIgnoreCase(sourceCode, "begin", startIndex);
                if (headerEndOffset == -1) headerEndOffset = Strings.indexOfIgnoreCase(sourceCode, "call", startIndex);
                if (headerEndOffset == -1) headerEndOffset = 0;
                content.getOffsets().setHeaderEndOffset(headerEndOffset);
            }
        }

        if (objectTypeId != DatabaseObjectTypeId.VIEW && objectTypeId != DatabaseObjectTypeId.MATERIALIZED_VIEW) {
            int nameIndex = Strings.indexOfIgnoreCase(sourceCode, objectName, 0);
            if (nameIndex > -1) {
                int guardedBlockEndOffset = nameIndex + objectName.length();
                if (guardedBlockEndOffset < sourceCode.length()) {
                    if (sourceCode.charAt(guardedBlockEndOffset) == '"'){
                        guardedBlockEndOffset++;
                    }
                    content.getOffsets().addGuardedBlock(0, guardedBlockEndOffset);
                }
            }
        }
    }

    /*********************************************************
     *                   CHANGE statements                   *
     *********************************************************/
    @Override
    public void updateView(String viewName, String code, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "change-view", viewName, code);
    }

    @Override
    public void updateTrigger(String tableOwner, String tableName, String triggerName, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        updateObject(triggerName, "trigger", oldCode, newCode, connection);
    }

    @Override
    public void updateObject(String objectName, String objectType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        // code contains object type and name
        executeUpdate(connection, "change-object", newCode);
    }

    public void updateJavaObject(String objectName, String code, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "change-java-object", objectName.replace(".","/"), code.replace("'","''"));
    }

    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    @Override
    public void createMethod(MethodFactoryInput method, DBNConnection connection) throws SQLException {
        Project project = method.getSchema().getProject();
        CodeStyleCaseSettings styleCaseSettings = PSQLCodeStyle.caseSettings(project);
        CodeStyleCaseOption kco = styleCaseSettings.getKeywordCaseOption();
        CodeStyleCaseOption oco = styleCaseSettings.getObjectCaseOption();
        CodeStyleCaseOption dco = styleCaseSettings.getDatatypeCaseOption();

        StringBuilder buffer = new StringBuilder();
        String methodType = method.isFunction() ? "function " : "procedure ";
        buffer.append(kco.format(methodType));
        buffer.append(oco.format(method.getObjectName()));
        buffer.append("(");
        
        int maxArgNameLength = 0;
        int maxArgDirectionLength = 0;
        for (ArgumentFactoryInput argument : method.getArguments()) {
            maxArgNameLength = Math.max(maxArgNameLength, argument.getObjectName().length());
            maxArgDirectionLength = Math.max(maxArgDirectionLength,
                    argument.isInput() && argument.isOutput() ? 6 :
                    argument.isInput() ? 2 :
                    argument.isOutput() ? 3 : 0);
        }


        for (ArgumentFactoryInput argument : method.getArguments()) {
            buffer.append("\n    ");
            buffer.append(oco.format(argument.getObjectName()));
            buffer.append(Strings.repeatSymbol(' ', maxArgNameLength - argument.getObjectName().length() + 1));
            String direction =
                    argument.isInput() && argument.isOutput() ? kco.format("in out") :
                    argument.isInput() ? kco.format("in") :
                    argument.isOutput() ? kco.format("out") : "";
            buffer.append(direction);
            buffer.append(Strings.repeatSymbol(' ', maxArgDirectionLength - direction.length() + 1));
            buffer.append(dco.format(argument.getDataType()));
            if (argument != method.getArguments().get(method.getArguments().size() -1)) {
                buffer.append(",");
            }
        }

        buffer.append(")\n");
        if (method.isFunction()) {
            buffer.append(kco.format("return "));
            buffer.append(dco.format(method.getReturnArgument().getDataType()));
            buffer.append("\n");
        }
        buffer.append(kco.format("is\nbegin\n\n"));
        if (method.isFunction()) buffer.append(kco.format("    return null;\n\n"));
        buffer.append("end;");
        createObject(buffer.toString(), connection);
    }
}