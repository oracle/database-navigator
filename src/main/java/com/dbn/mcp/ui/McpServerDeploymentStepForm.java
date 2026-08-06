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
import com.dbn.common.ui.link.Hyperlinks;
import com.dbn.mcp.deploy.McpDeploymentStep;
import com.dbn.mcp.registry.McpServerRecord;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Date;
import java.util.function.Consumer;

import static com.dbn.nls.NlsResources.txt;

/** One deployment step: what it does, whether it has run, and a button to run just that step. */
public class McpServerDeploymentStepForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel statusIconLabel;
    private JLabel titleLabel;
    private JLabel statusLabel;
    private JLabel detailLabel;
    private JButton runButton;
    private HyperlinkLabel extraLink;

    private final McpServerRecord record;
    private final McpDeploymentStep step;

    public McpServerDeploymentStepForm(
            @NotNull DBNForm parent,
            @NotNull McpServerRecord record,
            @NotNull McpDeploymentStep step,
            @NotNull Consumer<McpDeploymentStep> runHandler) {
        super(parent);
        this.record = record;
        this.step = step;

        titleLabel.setText(step.getTitle());
        detailLabel.setText("<html>" + step.getDetail() + "</html>");
        detailLabel.setForeground(JBColor.GRAY);

        runButton.setText(txt("msg.mcp.button.RunStep"));
        runButton.addActionListener(e -> runHandler.accept(step));

        initExtraLink();
        refresh();
    }

    /**
     * The build step links to the Dockerfile it will use, so the image being produced is not a
     * black box - the file is generated, and reading it is the fastest way to see what runs.
     */
    private void initExtraLink() {
        if (step != McpDeploymentStep.BUILD_IMAGE) {
            extraLink.setVisible(false);
            return;
        }
        Hyperlinks.initHyperlink(extraLink, txt("msg.mcp.button.ViewDockerfile"), this::openDockerfile);
    }

    private void openDockerfile() {
        Path dockerfile = record.getSourceProjectPath().resolve("Dockerfile.graal");
        if (!Files.isRegularFile(dockerfile)) return;

        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(dockerfile);
        if (file != null) FileEditorManager.getInstance(ensureProject()).openFile(file, true);
    }

    public void refresh() {
        long completedAt = step.completedAt(record);
        boolean completed = completedAt > 0;

        statusIconLabel.setIcon(completed ? AllIcons.General.InspectionsOK : AllIcons.General.TodoDefault);
        statusLabel.setText(completed
                ? DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(completedAt))
                : "");
        statusLabel.setForeground(JBColor.GRAY);
        runButton.setText(completed ? txt("msg.mcp.button.RunStepAgain") : txt("msg.mcp.button.RunStep"));
    }

    public void setRunnable(boolean runnable) {
        runButton.setEnabled(runnable);
    }

    @Override
    protected @NotNull JComponent getMainComponent() {
        return mainPanel;
    }
}
