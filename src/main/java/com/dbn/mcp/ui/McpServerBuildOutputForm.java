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

package com.dbn.mcp.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.mcp.registry.McpBuildLogListener;
import com.dbn.mcp.registry.McpServerRecord;
import com.dbn.mcp.registry.McpServerRegistry;
import com.dbn.mcp.registry.McpServerStatus;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Date;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.mcp.ui.McpServerPresentation.copyToClipboard;
import static com.dbn.mcp.ui.McpServerPresentation.initIconButton;
import static com.dbn.nls.NlsResources.txt;

/**
 * The output of the most recent build attempt. A running build streams into it; otherwise it is
 * read back from the log written beside the generated server, so the output survives restarts and
 * is available for failed builds - which is when it matters most.
 */
public class McpServerBuildOutputForm extends DBNFormBase {
    // the build output is line-tagged by its source; the console colors it accordingly
    private static final @NonNls String STDERR_PREFIX = "[STDERR]";
    private static final @NonNls String SYSTEM_PREFIX = "[SYSTEM]";

    private JPanel mainPanel;
    private JPanel consolePanel;
    private JLabel outcomeLabel;
    private JButton copyButton;

    private final McpServerRecord record;
    private final ConsoleView console;
    /** Mirrors what was printed, since a console view does not offer its text back. */
    private final StringBuilder printed = new StringBuilder();

    public McpServerBuildOutputForm(@NotNull DBNForm parent, @NotNull McpServerRecord record) {
        super(parent);
        this.record = record;

        this.console = TextConsoleBuilderFactory.getInstance().createBuilder(ensureProject()).getConsole();
        Disposer.register(this, console);
        consolePanel.add(console.getComponent(), BorderLayout.CENTER);

        if (record.getStatus() == McpServerStatus.BUILDING) {
            initLiveOutput();
        } else {
            initRecordedOutput();
        }
    }

    /**
     * While a build runs the log file does not exist yet, so the view starts from whatever the
     * running build has already produced and follows it from there.
     */
    private void initLiveOutput() {
        McpServerRegistry registry = McpServerRegistry.getInstance(ensureProject());
        String key = record.getOutputDirectory();

        initOutcomeLabel(true);
        printAll(registry.getBuildLogs().snapshot(key));

        ProjectEvents.subscribe(ensureProject(), this, McpBuildLogListener.TOPIC, new McpBuildLogListener() {
            @Override
            public void logAppended(String outputDirectory, String line) {
                if (!key.equals(outputDirectory)) return;
                dispatch(() -> print(line));
            }

            @Override
            public void logCleared(String outputDirectory) {
                if (!key.equals(outputDirectory)) return;
                dispatch(() -> {
                    printed.setLength(0);
                    console.clear();
                });
            }
        });
    }

    private void initRecordedOutput() {
        String output = readBuildLog();
        initOutcomeLabel(output != null);

        if (output == null) {
            // servers imported from an older output folder predate the build log
            console.print(txt("msg.mcp.text.BuildOutputUnavailable"), ConsoleViewContentType.SYSTEM_OUTPUT);
            return;
        }
        printAll(output);
    }

    private void printAll(String output) {
        if (output.isEmpty()) return;
        for (String line : output.split("\n", -1)) {
            if (!line.isEmpty()) print(line);
        }
    }

    private void print(String line) {
        printed.append(line).append('\n');
        console.print(line + "\n", contentType(line));
        if (console instanceof ConsoleViewImpl consoleView) consoleView.requestScrollingToEnd();
    }

    private static ConsoleViewContentType contentType(String line) {
        if (line.startsWith(STDERR_PREFIX)) return ConsoleViewContentType.ERROR_OUTPUT;
        if (line.startsWith(SYSTEM_PREFIX)) return ConsoleViewContentType.SYSTEM_OUTPUT;
        return ConsoleViewContentType.NORMAL_OUTPUT;
    }

    private void initOutcomeLabel(boolean hasOutput) {
        McpServerStatus status = record.getStatus();
        boolean highlighted = status == McpServerStatus.FAILED || status == McpServerStatus.BUILDING;

        StringBuilder outcome = new StringBuilder(McpServerPresentation.statusName(status));
        if (record.getBuildTimestamp() > 0) {
            outcome.append("  ·  ").append(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(new Date(record.getBuildTimestamp())));
        }
        if (record.getBuildDuration() > 0) {
            outcome.append("  ·  ").append(txt("msg.mcp.text.BuildDuration", record.getBuildDuration() / 1000d));
        }

        outcomeLabel.setText(outcome.toString());
        outcomeLabel.setIcon(McpServerPresentation.statusIcon(status));
        outcomeLabel.setIconTextGap(6);
        if (highlighted) outcomeLabel.setForeground(McpServerPresentation.statusColor(status));

        copyButton.setVisible(hasOutput);
        if (hasOutput) {
            initIconButton(copyButton, AllIcons.Actions.Copy,
                    txt("app.mcp.button.CopyToClipboard"), () -> copyToClipboard(printed.toString()));
        }
    }

    private String readBuildLog() {
        Path buildLog = record.getBuildLogPath();
        try {
            if (!Files.isRegularFile(buildLog)) return null;
            String content = Files.readString(buildLog, StandardCharsets.UTF_8);
            return content.isBlank() ? null : content;
        } catch (IOException e) {
            conditionallyLog(e);
            return null;
        }
    }

    @Override
    protected @NotNull JComponent getMainComponent() {
        return mainPanel;
    }
}
