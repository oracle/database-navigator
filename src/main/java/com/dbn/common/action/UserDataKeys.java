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

import com.dbn.common.data.Data;
import com.dbn.common.notification.NotificationCategory;
import com.dbn.common.outcome.MessageOutcomeHandler;
import com.dbn.common.outcome.NotificationOutcomeHandler;
import com.dbn.common.project.ModuleRef;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.connection.mapping.FileConnectionContext;
import com.dbn.diagnostics.data.DiagnosticCategory;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.language.common.DBLanguageDialect;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Map;

@UtilityClass
public class UserDataKeys {
    public static final Key<Boolean> INVALID_ENTITY = Key.create("DBNavigator.InvalidEntity");
    public static final Key<String> ACTION_PLACE = Key.create("DBNavigator.ActionPlace");
    public static final Key<Boolean> PROJECT_SETTINGS_LOADED = Key.create("DBNavigator.ProjectSettingsLoaded");
    public static final Key<ProjectRef> PROJECT_REF = Key.create("DBNavigator.ProjectRef");
    public static final Key<ModuleRef> MODULE_REF = Key.create("DBNavigator.ModuleRef");
    public static final Key<List<StatementExecutionProcessor>> STATEMENT_EXECUTION_PROCESSORS = Key.create("DBNavigator.StatementExecutionProcessors");
    public static final Key<FileConnectionContext> FILE_CONNECTION_MAPPING = Key.create("DBNavigator.FileConnectionMapping");
    public static final Key<Boolean> HAS_CONNECTIVITY_CONTEXT = Key.create("DBNavigator.HasConnectivityContext");
    public static final Key<DBLanguageDialect> LANGUAGE_DIALECT = Key.create("DBNavigator.LanguageDialect");
    public static final Key<String> GUARDED_BLOCK_REASON = Key.create("DBNavigator.GuardedBlockReason");
    public static final Key<DiagnosticCategory> DIAGNOSTIC_CONTENT_CATEGORY = Key.create("DBNavigator.DiagnosticContentType");
    public static final Key<DBNForm> DIAGNOSTIC_CONTENT_FORM = Key.create("DBNavigator.DiagnosticContentForm");
    public static final Key<DBNForm> EVENT_MONITOR_FORM = Key.create("DBNavigator.EventMonitorForm");
    public static final Key<Integer> BREAKPOINT_ID = Key.create("DBNavigator.BreakpointId");
    public static final Key<VirtualFile> BREAKPOINT_FILE = Key.create("DBNavigator.BreakpointFile");
    public static final Key<Boolean> SKIP_BROWSER_AUTOSCROLL = Key.create("DBNavigator.SkipEditorScroll");
    public static final Key<Long> LAST_ANNOTATION_REFRESH = Key.create("DBNavigator.LastAnnotationRefresh");

    public static final Key<MessageOutcomeHandler> MESSAGE_OUTCOME_HANDLER = Key.create("DBNavigator.MessageOutcomeHandler");
    public static final Key<Map<NotificationCategory, NotificationOutcomeHandler>> NOTIFICATION_OUTCOME_HANDLERS = Key.create("DBNavigator.NotificationOutcomeHandlers");

    public static boolean isUserData(UserDataHolder dataHolder, Key<Boolean> key) {
        return Data.asBooleanPrimitive( dataHolder.getUserData(key));
    }

    public static <T> void setUserData(UserDataHolder dataHolder, Key<T> key, T value) {
        dataHolder.putUserData(key, value);
    }

    public static <T> T getUserData(UserDataHolder dataHolder, Key<T> key) {
        return dataHolder.getUserData(key);
    }
}
