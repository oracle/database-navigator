package com.dbn.common.action;

import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.credential.remote.ui.CredentialManagementForm;
import com.dbn.assistant.profile.ui.ProfileManagementForm;
import com.dbn.connection.config.ui.ConnectionBundleSettingsForm;
import com.dbn.data.editor.ui.array.ArrayEditorPopupProviderForm;
import com.dbn.data.editor.ui.calendar.CalendarPopupProviderForm;
import com.dbn.data.editor.ui.text.TextEditorPopupProviderForm;
import com.dbn.diagnostics.ui.ParserDiagnosticsForm;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.session.SessionBrowser;
import com.dbn.execution.explain.result.ExplainPlanResult;
import com.dbn.execution.logging.DatabaseLoggingResult;
import com.dbn.execution.method.result.MethodExecutionResult;
import com.dbn.execution.method.result.ui.MethodExecutionCursorResultForm;
import com.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.dbn.object.DBArgument;
import com.intellij.openapi.actionSystem.DataKey;

public interface DataKeys {
    DataKey<DatasetEditor> DATASET_EDITOR = DataKey.create("DBNavigator.DatasetEditor");
    DataKey<ConnectionBundleSettingsForm> CONNECTION_BUNDLE_SETTINGS = DataKey.create("DBNavigator.ConnectionSettingsEditor");
    DataKey<SessionBrowser> SESSION_BROWSER = DataKey.create("DBNavigator.SessionBrowser");
    DataKey<StatementExecutionCursorResult> STATEMENT_EXECUTION_CURSOR_RESULT = DataKey.create("DBNavigator.StatementExecutionCursorResult");
    DataKey<MethodExecutionResult> METHOD_EXECUTION_RESULT = DataKey.create("DBNavigator.MethodExecutionResult");
    DataKey<MethodExecutionCursorResultForm> METHOD_EXECUTION_CURSOR_RESULT_FORM = DataKey.create("DBNavigator.MethodExecutionCursorResult");
    DataKey<DBArgument> METHOD_EXECUTION_ARGUMENT = DataKey.create("DBNavigator.MethodExecutionArgument");
    DataKey<ExplainPlanResult> EXPLAIN_PLAN_RESULT = DataKey.create("DBNavigator.ExplainPlanResult");
    DataKey<DatabaseLoggingResult> DATABASE_LOG_OUTPUT = DataKey.create("DBNavigator.DatabaseLogOutput");
    DataKey<ParserDiagnosticsForm> PARSER_DIAGNOSTICS_FORM = DataKey.create("DBNavigator.ParserDiagnosticsForm");
    DataKey<ChatBoxForm> ASSISTANT_CHAT_BOX = DataKey.create("DBNavigator.AssistantChatBox");
    DataKey<CredentialManagementForm> CREDENTIAL_MANAGEMENT_FORM = DataKey.create("DBNavigator.CredentialManagementForm");
    DataKey<ProfileManagementForm> PROFILE_MANAGEMENT_FORM = DataKey.create("DBNavigator.ProfileManagementForm");

    DataKey<CalendarPopupProviderForm> CALENDAR_POPUP_PROVIDER_FORM = DataKey.create("DBNavigator.CalendarPopupProviderForm");
    DataKey<ArrayEditorPopupProviderForm> ARRAY_EDITOR_POPUP_PROVIDER_FORM = DataKey.create("DBNavigator.ArrayEditorPopupProviderForm");
    DataKey<TextEditorPopupProviderForm> TEXT_EDITOR_POPUP_PROVIDER_FORM = DataKey.create("DBNavigator.TextEditorPopupProviderForm");
}
