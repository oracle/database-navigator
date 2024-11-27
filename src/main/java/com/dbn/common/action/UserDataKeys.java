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

package com.dbn.common.action;

import com.dbn.common.notification.NotificationGroup;
import com.dbn.common.outcome.MessageOutcomeHandler;
import com.dbn.common.outcome.NotificationOutcomeHandler;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.connection.mapping.FileConnectionContext;
import com.dbn.diagnostics.data.DiagnosticCategory;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.language.common.DBLanguageDialect;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;
import java.util.Map;

public interface UserDataKeys {
    Key<Boolean> INVALID_ENTITY = Key.create("DBNavigator.InvalidEntity");
    Key<String> ACTION_PLACE = Key.create("DBNavigator.ActionPlace");
    Key<Boolean> PROJECT_SETTINGS_LOADED = Key.create("DBNavigator.ProjectSettingsLoaded");
    Key<ProjectRef> PROJECT_REF = Key.create("DBNavigator.ProjectRef");
    Key<List<StatementExecutionProcessor>> STATEMENT_EXECUTION_PROCESSORS = Key.create("DBNavigator.StatementExecutionProcessors");
    Key<FileConnectionContext> FILE_CONNECTION_MAPPING = Key.create("DBNavigator.FileConnectionMapping");
    Key<Boolean> HAS_CONNECTIVITY_CONTEXT = Key.create("DBNavigator.HasConnectivityContext");
    Key<DBLanguageDialect> LANGUAGE_DIALECT = Key.create("DBNavigator.LanguageDialect");
    Key<String> GUARDED_BLOCK_REASON = Key.create("DBNavigator.GuardedBlockReason");
    Key<DiagnosticCategory> DIAGNOSTIC_CONTENT_CATEGORY = Key.create("DBNavigator.DiagnosticContentType");
    Key<DBNForm> DIAGNOSTIC_CONTENT_FORM = Key.create("DBNavigator.DiagnosticContentForm");
    Key<Integer> BREAKPOINT_ID = Key.create("DBNavigator.BreakpointId");
    Key<VirtualFile> BREAKPOINT_FILE = Key.create("DBNavigator.BreakpointFile");
    Key<Boolean> SKIP_BROWSER_AUTOSCROLL = Key.create("DBNavigator.SkipEditorScroll");
    Key<Long> LAST_ANNOTATION_REFRESH = Key.create("DBNavigator.LastAnnotationRefresh");


    Key<MessageOutcomeHandler> MESSAGE_OUTCOME_HANDLER = Key.create("DBNavigator.MessageOutcomeHandler");
    Key<Map<NotificationGroup, NotificationOutcomeHandler>> NOTIFICATION_OUTCOME_HANDLERS = Key.create("DBNavigator.NotificationOutcomeHandlers");
}
