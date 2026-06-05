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

import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.assistant.tool.execution.AssistantToolRequest;
import com.dbn.common.action.BasicTextAction;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBConsole;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.WindowWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.awt.Window;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Modality.nonModal;

public class ConsoleUpdateDiffFeature implements AssistantToolFeature {
    private static final String UPDATE_SQL_CONSOLE_CONTENT = "UPDATE_SQL_CONSOLE_CONTENT";
    private static final Map<String, WindowWrapper> ACTIVE_DIFF_WINDOWS = new ConcurrentHashMap<>();
    private static final Set<String> OPENING_DIFF_WINDOWS = ConcurrentHashMap.newKeySet();

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
    public void execute(@NotNull AssistantToolFeatureContext context) {
        AssistantToolRequest toolRequest = context.getToolRequest();
        ConnectionHandler connection = context.getConnection();
        Project project = context.getProject();
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
        showDiff(
                project,
                createDiffWindowKey(project, toolRequest, consoleName),
                consoleName,
                currentContent,
                requestedContent,
                console.getVirtualFile().getFileType(),
                context::approve,
                context::deny);
    }

    private static void showDiff(
            Project project,
            String diffWindowKey,
            String consoleName,
            String currentContent,
            String requestedContent,
            FileType fileType,
            @NotNull Runnable onApprove,
            @NotNull Runnable onDeny) {
        if (focusDiffWindow(diffWindowKey)) return;
        if (!OPENING_DIFF_WINDOWS.add(diffWindowKey)) return;

        String title = consoleName + " (AI update)";
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        DiffContent current = contentFactory.create(project, currentContent, fileType);
        DiffContent requested = contentFactory.create(project, requestedContent, fileType);

        SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                title,
                current,
                requested,
                "Current content",
                "Requested content");

        AtomicBoolean resolved = new AtomicBoolean();
        diffRequest.putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, List.of(
                new ResolveUpdateAction("Apply Update", Icons.ACTION_CHECK, resolved, diffWindowKey, onApprove),
                new ResolveUpdateAction("Reject Update", Icons.ACTION_CLOSE, resolved, diffWindowKey, onDeny)));

        DiffDialogHints hints = new DiffDialogHints(WindowWrapper.Mode.NON_MODAL, null, wrapper -> {
            OPENING_DIFF_WINDOWS.remove(diffWindowKey);
            ACTIVE_DIFF_WINDOWS.put(diffWindowKey, wrapper);
            Disposer.register(wrapper, () -> {
                ACTIVE_DIFF_WINDOWS.remove(diffWindowKey, wrapper);
                OPENING_DIFF_WINDOWS.remove(diffWindowKey);
            });
        });

        Dispatch.run(nonModal(), () -> {
            try {
                DiffManager.getInstance().showDiff(project, diffRequest, hints);
            } catch (RuntimeException e) {
                OPENING_DIFF_WINDOWS.remove(diffWindowKey);
                ACTIVE_DIFF_WINDOWS.remove(diffWindowKey);
                throw e;
            }
        });
    }

    private static boolean focusDiffWindow(@NotNull String diffWindowKey) {
        if (OPENING_DIFF_WINDOWS.contains(diffWindowKey)) return true;

        WindowWrapper windowWrapper = ACTIVE_DIFF_WINDOWS.get(diffWindowKey);
        if (windowWrapper == null) return false;

        if (isNotValid(windowWrapper)) {
            ACTIVE_DIFF_WINDOWS.remove(diffWindowKey, windowWrapper);
            return false;
        }

        Window window = windowWrapper.getWindow();
        window.toFront();
        window.requestFocus();
        return true;
    }

    private static void closeDiffWindow(@NotNull String diffWindowKey) {
        OPENING_DIFF_WINDOWS.remove(diffWindowKey);

        WindowWrapper windowWrapper = ACTIVE_DIFF_WINDOWS.remove(diffWindowKey);
        if (isNotValid(windowWrapper)) return;

        windowWrapper.close();
    }

    private static String createDiffWindowKey(Project project, AssistantToolRequest toolRequest, String consoleName) {
        String requestId = Objects.toString(
                toolRequest.getRequestId(),
                toolRequest.getToolName() + ":" + consoleName);
        return project.hashCode() + ":" + requestId;
    }

    private static class ResolveUpdateAction extends BasicTextAction {
        private final AtomicBoolean resolved;
        private final String diffWindowKey;
        private final Runnable callback;

        private ResolveUpdateAction(
                @NotNull String text,
                @NotNull Icon icon,
                @NotNull AtomicBoolean resolved,
                @NotNull String diffWindowKey,
                @NotNull Runnable callback) {
            super(text, null, icon);
            this.resolved = resolved;
            this.diffWindowKey = diffWindowKey;
            this.callback = callback;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (resolved.compareAndSet(false, true)) {
                try {
                    callback.run();
                } finally {
                    closeDiffWindow(diffWindowKey);
                }
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            updateResolutionAction(e, resolved);
        }
    }

    private static void updateResolutionAction(@NotNull AnActionEvent e, @NotNull AtomicBoolean resolved) {
        boolean visible = !resolved.get();
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(visible);
        presentation.setVisible(visible);
    }
}
