package com.dbn.database.oracle;

import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseCompatibilityInterfaceImpl;
import com.dbn.database.DatabaseFeature;
import com.dbn.editor.session.SessionStatus;
import com.dbn.language.common.QuoteDefinition;
import com.dbn.language.common.QuotePair;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.dbn.database.DatabaseFeature.*;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public class OracleCompatibilityInterface extends DatabaseCompatibilityInterfaceImpl {
    public static final QuoteDefinition IDENTIFIER_QUOTE_DEFINITION = new QuoteDefinition(new QuotePair('"', '"'));

    @Override
    public boolean supportsObjectType(DatabaseObjectTypeId objectTypeId) {
        return true;
    }

    @Override
    public List<DatabaseObjectTypeId> getSupportedObjectTypes() {
        return Collections.emptyList(); // default implementation not used (all object types are supported)
    }

    @Override
    public List<DatabaseFeature> getSupportedFeatures() {
        return Arrays.asList(
                OBJECT_INVALIDATION,
                OBJECT_DEPENDENCIES,
                OBJECT_REPLACING,
                OBJECT_DDL_EXTRACTION,
                OBJECT_DISABLING,
                OBJECT_CHANGE_MONITORING,
                OBJECT_SOURCE_EDITING,
                AUTHID_METHOD_EXECUTION,
                FUNCTION_OUT_ARGUMENTS,
                DEBUGGING,
                EXPLAIN_PLAN,
                DATABASE_LOGGING,
                SESSION_BROWSING,
                SESSION_INTERRUPTION_TIMING,
                SESSION_DISCONNECT,
                SESSION_KILL,
                SESSION_CURRENT_SQL,
                CONNECTION_ERROR_RECOVERY,
                UPDATABLE_RESULT_SETS,
                CURRENT_SCHEMA,
                USER_SCHEMA,
                CONSTRAINT_MANIPULATION,
                READONLY_CONNECTIVITY,
                AI_ASSISTANT);
    }

    @Override
    public QuoteDefinition getIdentifierQuotes() {
        return IDENTIFIER_QUOTE_DEFINITION;
    }

    @Override
    public String getDefaultAlternativeStatementDelimiter() {
        return null;
    }

    @Override
    public SessionStatus getSessionStatus(String statusName) {
        try{
            return SessionStatus.valueOf(statusName);
        } catch (Exception e) {
            conditionallyLog(e);
            log.error("Invalid session status {}", statusName, e);
            return SessionStatus.INACTIVE;
        }
    }

    @Override
    public String getExplainPlanStatementPrefix() {
        return "explain plan for ";
    }

    @Override
    public String getDatabaseLogName() {
        return txt("app.logging.label.LogName_ORACLE");
    }
}