/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.assistant.tool.feature;

import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.assistant.tool.execution.AssistantToolRequest;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBConsole;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.util.Modality.nonModal;

public class ConsoleUpdateDiffFeature implements AssistantToolFeature {
    private static final String UPDATE_SQL_CONSOLE_CONTENT = "UPDATE_SQL_CONSOLE_CONTENT";

    @Override
    public @NotNull String getName() {
        return "Show Diff";
    }

    @Override
    public boolean supports(@NotNull AssistantToolType toolType, @NotNull String toolName) {
        return toolType == AssistantToolType.SQL_CONSOLE_EDITORS &&
                Objects.equals(toolName, UPDATE_SQL_CONSOLE_CONTENT);
    }

    @Override
    public @NotNull Duration getApprovalTimeoutExtension() {
        return Duration.ofMinutes(10);
    }

    @Override
    public void execute(
            @NotNull AssistantToolRequest toolRequest,
            @NotNull ChatContext chatContext,
            @NotNull AssistantState assistantState) {
        ConnectionHandler connection = assistantState.getConnection();
        Project project = connection.getProject();
        List<?> argumentValues = toolRequest.getToolArgumentValues();
        if (argumentValues.size() < 2) {
            Messages.showErrorDialog(project, "Show Diff", "Could not resolve SQL console update arguments.");
            return;
        }

        String consoleName = Objects.toString(argumentValues.get(0), "");
        String requestedContent = Objects.toString(argumentValues.get(1), "").replace("\\n", "\n");

        DBConsole console = connection.getConsoleBundle().getConsole(consoleName);
        if (console == null) {
            Messages.showErrorDialog(project, "Show Diff", "Could not find SQL console \"" + consoleName + "\".");
            return;
        }

        String currentContent = console.getVirtualFile().getContent().getText().toString();
        showDiff(project, consoleName, currentContent, requestedContent, console.getVirtualFile().getFileType());
    }

    private static void showDiff(
            Project project,
            String consoleName,
            String currentContent,
            String requestedContent,
            FileType fileType) {
        String title = "SQL Console Update - " + consoleName;
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        DiffContent current = contentFactory.create(project, currentContent, fileType);
        DiffContent requested = contentFactory.create(project, requestedContent, fileType);

        SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                title,
                current,
                requested,
                "Current content",
                "Requested content");

        Dispatch.run(nonModal(), () -> DiffManager.getInstance().showDiff(project, diffRequest));
    }
}
