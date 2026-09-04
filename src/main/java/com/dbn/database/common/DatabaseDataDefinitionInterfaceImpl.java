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
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.editor.code.content.GuardedBlockMarker;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.language.common.quotes.QuotePair;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBConstraintType;
import org.jetbrains.annotations.NonNls;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.language.common.quotes.QuoteEscaping.DATABASE;

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
     *                   CHANGE statements                   *
     *********************************************************/
    @Override
    public void updateView(String ownerName, String viewName, String code, boolean editionable, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "change-view", ownerName, viewName, code);
    }

    @Override
    public void updateJsonView(String ownerName, String viewName, String code, boolean editionable, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "change-json-view", ownerName, viewName, code);
    }

    /*********************************************************
     *                   DROP statements                     *
     *********************************************************/
    @Override
    public void dropObject(String objectType, String ownerName, String objectName, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "drop-object", objectType, ownerName, objectName);
    }

    public void dropObjectIfExists(String objectType, String objectOwner, String objectName, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "drop-object-if-exists", objectType, objectOwner, objectName);
    }

    @Override
    public void dropConstraint(String ownerName, String tableName, String constraintName, DBConstraintType constraintType, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "drop-constraint", ownerName, tableName, constraintName);
    }

    @Override
    public void dropObjectBody(String objectType, String ownerName, String objectName, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "drop-object-body", objectType, ownerName, objectName);
    }

    protected String updateNameQualification(String code, boolean qualified, String objectType, String schemaName, String objectName, CodeStyleCaseSettings caseSettings) {
        CodeStyleCaseOption kco = caseSettings.getKeywordCaseOption();

        StringBuilder buffer = new StringBuilder();
        QuotePair quotes = getInterfaces().getCompatibilityInterface().getDefaultIdentifierQuotes();

        String rawSchemaName = quotes.unquote(schemaName, DATABASE);
        String rawObjectName = quotes.unquote(objectName, DATABASE);
        Pattern pattern = createNameQualificationPattern(objectType, rawSchemaName, rawObjectName, quotes);
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            String replacement = kco.format(objectType) + " " + (qualified ? schemaName + "." : "") + objectName;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            matcher.appendTail(buffer);
            code = buffer.toString();
        }
        return code;
    }

    static Pattern createNameQualificationPattern(String objectType, String rawSchemaName, String rawObjectName, QuotePair quotes) {
        String bq = "(" + Pattern.quote(quotes.beginQuote()) + ")?";
        String eq = "(" + Pattern.quote(quotes.endQuote()) + ")?";
        String regex = objectType + "\\s+(" + bq + Pattern.quote(rawSchemaName) + eq + "\\s*\\.)?\\s*" + bq + Pattern.quote(rawObjectName) + eq;

        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
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

    @Override
    public void compileObject(String ownerName, String objectName, String objectType, boolean debug, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "compile-object", ownerName, objectName, objectType, debug ? "DEBUG" : "");
    }

    @Override
    public void compileObjectBody(String ownerName, String objectName, String objectType, boolean debug, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "compile-object-body", ownerName, objectName, objectType, debug ? "DEBUG" : "");
    }

    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    @Override
    public void createMethod(DBObjectSpec methodSpec, DBNConnection connection) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void createTable(DBObjectSpec tableSpec, DBNConnection connection) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void createIndex(DBObjectSpec indexSpec, DBNConnection connection) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
