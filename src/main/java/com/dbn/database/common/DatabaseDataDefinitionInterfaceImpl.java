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

package com.dbn.database.common;

import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.editor.code.content.GuardedBlockMarker;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.language.common.QuotePair;
import org.jetbrains.annotations.NonNls;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.common.util.Strings.cachedUpperCase;

@NonNls
public abstract class DatabaseDataDefinitionInterfaceImpl extends DatabaseInterfaceBase implements DatabaseDataDefinitionInterface {
    public static final @NonNls String TEMP_OBJECT_NAME = "DBN_TEMPORARY_{0}_0001";

    public static String getTempObjectName(String objectType) {
        return MessageFormat.format(TEMP_OBJECT_NAME, cachedUpperCase(objectType));
    }

    public DatabaseDataDefinitionInterfaceImpl(String fileName, DatabaseInterfaces provider) {
        super(fileName, provider);
    }

    @Override
    public boolean includesTypeAndNameInSourceContent(DatabaseObjectTypeId objectTypeId) {
        return
                objectTypeId == DatabaseObjectTypeId.FUNCTION ||
                objectTypeId == DatabaseObjectTypeId.PROCEDURE ||
                objectTypeId == DatabaseObjectTypeId.PACKAGE ||
                objectTypeId == DatabaseObjectTypeId.TRIGGER ||
                objectTypeId == DatabaseObjectTypeId.TYPE;

    }

    protected final String getSingleValue(DBNConnection connection, String loaderId, Object... arguments) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = executeQuery(connection, loaderId, arguments);
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        } finally {
            Resources.close(resultSet);
        }
        return null;
    }

    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    @Override
    public void createView(String viewName, String code, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "create-view", viewName, code);
    }

    @Override
    public void createObject(String code, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "create-object", code);
    }


   /*********************************************************
    *                   DROP statements                     *
    *********************************************************/
   @Override
   public void dropObject(String objectType, String ownerName, String objectName, DBNConnection connection) throws SQLException {
       executeUpdate(connection, "drop-object", objectType, ownerName, objectName);
   }

   @Override
   public void dropObjectBody(String objectType, String ownerName, String objectName, DBNConnection connection) throws SQLException {
       executeUpdate(connection, "drop-object-body", objectType, ownerName, objectName);
   }

    @Override
    public void dropJavaClass(String ownerName, String objectName, DBNConnection connection) throws SQLException {
       // TODO move to OracleDataDefinitionInterface (too specific for this level)
        executeUpdate(connection, "drop-java-object", ownerName, objectName);
    }

    protected String updateNameQualification(String code, boolean qualified, String objectType, String schemaName, String objectName, CodeStyleCaseSettings caseSettings) {
        CodeStyleCaseOption kco = caseSettings.getKeywordCaseOption();
        CodeStyleCaseOption oco = caseSettings.getObjectCaseOption();

        StringBuilder buffer = new StringBuilder();
        QuotePair quotes = getInterfaces().getCompatibilityInterface().getDefaultIdentifierQuotes();
        String bq = "\\" + quotes.beginChar() + "?";
        String eq = "\\" + quotes.endChar() + "?";
        String regex = objectType + "\\s+(" + bq + schemaName + eq + "\\s*\\.)?\\s*" + bq + objectName + eq;
        if (qualified) {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(code);
            if (matcher.find()) {
                String replacement = kco.format(objectType) + " " + oco.format(schemaName + "." + objectName);
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
                matcher.appendTail(buffer);
                code = buffer.toString();
            }
        } else {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(code);
            if (matcher.find()) {
                String replacement = kco.format(objectType) + " " + oco.format(objectName);
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
                matcher.appendTail(buffer);
                code = buffer.toString();
            }
        }
        return code;
    }

    @Override
    public void computeSourceCodeOffsets(SourceCodeContent content, DatabaseObjectTypeId objectTypeId, String objectName) {
        String sourceCode = content.getText().toString();
        int gbEndOffset = sourceCode.indexOf(GuardedBlockMarker.END_OFFSET_IDENTIFIER);
        if (gbEndOffset > -1) {
            content.getOffsets().addGuardedBlock(0, gbEndOffset);
            sourceCode =
                    sourceCode.substring(0, gbEndOffset) +
                    sourceCode.substring(gbEndOffset + GuardedBlockMarker.END_OFFSET_IDENTIFIER.length());
            content.setText(sourceCode);
        }
    }

    protected String quoted(String identifier) {
        QuotePair quotes = getInterfaces().getCompatibilityInterface().getDefaultIdentifierQuotes();
        return quotes.quote(identifier);
    }
}
