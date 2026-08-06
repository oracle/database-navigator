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
import com.dbn.mcp.deploy.McpContainerRuntimeSupport;
import com.dbn.mcp.deploy.McpDeploymentStep;
import com.dbn.mcp.registry.McpServerRecord;
import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Date;

import static com.dbn.nls.NlsResources.txt;

/** One line of deployment progress: what the step does, and whether it has happened. */
public class McpServerDeploymentStepForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel statusIconLabel;
    private JLabel titleLabel;
    private JLabel statusLabel;
    private JLabel detailLabel;

    private final McpServerRecord record;
    private final McpDeploymentStep step;

    public McpServerDeploymentStepForm(
            @NotNull DBNForm parent,
            @NotNull McpServerRecord record,
            @NotNull McpDeploymentStep step) {
        super(parent);
        this.record = record;
        this.step = step;

        titleLabel.setText(step.getTitle());
        detailLabel.setText(detailText());
        detailLabel.setForeground(JBColor.GRAY);
        refresh();
    }

    /**
     * The push is the one step depending on something outside the IDE, so it names the exact
     * command instead of the generic description - DB Navigator never handles those credentials.
     */
    private String detailText() {
        if (step != McpDeploymentStep.PUSH_IMAGE) return step.getDetail();

        String runtime = McpContainerRuntimeSupport.findContainerRuntime();
        if (runtime == null) return txt("msg.mcp.text.ContainerRuntimeMissing");

        String command = Path.of(runtime).getFileName() + " login " + registryHost();
        return txt("msg.mcp.text.RegistryLoginRequired", command);
    }

    private String registryHost() {
        var deployment = record.getDeployment();
        String region = deployment == null || deployment.getRegionKey() == null ? "iad" : deployment.getRegionKey();
        return region + ".ocir.io";
    }

    public void refresh() {
        long completedAt = step.completedAt(record);
        boolean completed = completedAt > 0;

        statusIconLabel.setIcon(completed ? AllIcons.General.InspectionsOK : AllIcons.General.TodoDefault);
        statusLabel.setText(completed
                ? DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(completedAt))
                : "");
        statusLabel.setForeground(JBColor.GRAY);
        titleLabel.setForeground(completed ? null : JBColor.GRAY);
    }

    @Override
    protected @NotNull JComponent getMainComponent() {
        return mainPanel;
    }
}
