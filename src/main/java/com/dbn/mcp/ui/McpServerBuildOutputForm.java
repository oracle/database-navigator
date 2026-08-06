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

import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.mcp.registry.McpServerRecord;
import com.dbn.mcp.registry.McpServerStatus;
import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Font;
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
 * The output of the most recent build attempt, read back from the log written beside the
 * generated server - so it survives restarts and is available for failed builds, which is when
 * it matters most.
 */
public class McpServerBuildOutputForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel outcomeLabel;
    private JButton copyButton;
    private JScrollPane outputScrollPane;
    private JBTextArea outputTextArea;

    private final McpServerRecord record;

    public McpServerBuildOutputForm(@NotNull DBNForm parent, @NotNull McpServerRecord record) {
        super(parent);
        this.record = record;

        outputTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputTextArea.setBorder(JBUI.Borders.empty(6, 8));

        String output = readBuildLog();
        initOutcomeLabel(output != null);
        initOutput(output);
    }

    private void initOutcomeLabel(boolean hasOutput) {
        McpServerStatus status = record.getStatus();
        boolean failed = status == McpServerStatus.FAILED;

        StringBuilder outcome = new StringBuilder(McpServerPresentation.statusName(status));
        if (record.getBuildTimestamp() > 0) {
            outcome.append("  ·  ").append(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(new Date(record.getBuildTimestamp())));
        }
        if (record.getBuildDuration() > 0) {
            outcome.append("  ·  ").append(txt("msg.mcp.text.BuildDuration",
                    record.getBuildDuration() / 1000d));
        }

        outcomeLabel.setText(outcome.toString());
        outcomeLabel.setIcon(McpServerPresentation.statusIcon(status));
        outcomeLabel.setIconTextGap(6);
        if (failed) outcomeLabel.setForeground(McpServerPresentation.statusColor(status));

        copyButton.setVisible(hasOutput);
        if (hasOutput) {
            initIconButton(copyButton, AllIcons.Actions.Copy,
                    txt("app.mcp.button.CopyToClipboard"), () -> copyToClipboard(outputTextArea.getText()));
        }
    }

    private void initOutput(String output) {
        if (output == null) {
            // servers imported from an older output folder predate the build log
            outputTextArea.setText(txt("msg.mcp.text.BuildOutputUnavailable"));
            outputTextArea.setForeground(JBColor.GRAY);
            return;
        }
        outputTextArea.setText(output);
        outputTextArea.setCaretPosition(0);
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
