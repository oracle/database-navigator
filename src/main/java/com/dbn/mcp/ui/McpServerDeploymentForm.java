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

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.link.Hyperlinks;
import com.dbn.mcp.deploy.McpDeploymentStep;
import com.dbn.mcp.registry.McpDeploymentInfo;
import com.dbn.mcp.registry.McpServerRecord;
import com.dbn.mcp.registry.McpServerRegistryListener;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.mcp.ui.McpServerPresentation.monospaced;
import static com.dbn.nls.NlsResources.txt;

/**
 * Deploying is one action of three parts. The parts are shown as progress rather than as separate
 * controls, and deploying always resumes at the first that has not succeeded - so a failed push
 * never rebuilds an image that already exists.
 */
public class McpServerDeploymentForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel stepsPanel;
    private JPanel ocidPanel;
    private JLabel introLabel;
    private JLabel targetLabel;
    private JLabel ocidLabel;
    private JButton deployButton;
    private JBTextField ocidField;
    private HyperlinkLabel configureLink;

    private final McpServerRecord record;
    private final List<McpServerDeploymentStepForm> stepForms = new ArrayList<>();

    public McpServerDeploymentForm(@NotNull DBNForm parent, @NotNull McpServerRecord record) {
        super(parent);
        this.record = record;

        introLabel.setText("<html>" + txt("msg.mcp.text.DeploymentIntro") + "</html>");
        introLabel.setForeground(JBColor.GRAY);

        targetLabel.setFont(monospaced(targetLabel));
        Hyperlinks.initHyperlink(configureLink, txt("msg.mcp.button.EditRegistry"), this::configureTarget);

        ocidLabel.setText(txt("msg.mcp.label.ContainerImageOcid"));
        ocidField.getEmptyText().setText(txt("msg.mcp.text.ContainerImageOcidHint"));

        deployButton.addActionListener(e -> deploy());
        initSteps();
        refresh();

        ProjectEvents.subscribe(ensureProject(), this, McpServerRegistryListener.TOPIC,
                new McpServerRegistryListener() {
                    @Override
                    public void recordUpdated(McpServerRecord updated) {
                        if (!record.getOutputDirectory().equals(updated.getOutputDirectory())) return;
                        dispatch(() -> refresh());
                    }
                });
    }

    private void initSteps() {
        JPanel stack = new JPanel();
        verticalBoxLayout(stack);

        for (McpDeploymentStep step : McpDeploymentStep.values()) {
            McpServerDeploymentStepForm form = new McpServerDeploymentStepForm(this, record, step);
            stepForms.add(form);
            stack.add(form.getComponent());
        }
        stepsPanel.add(stack, BorderLayout.CENTER);
    }

    private void refresh() {
        stepForms.forEach(McpServerDeploymentStepForm::refresh);
        targetLabel.setText(targetImage());

        // the identifier is only asked for once the image exists and could not be resolved for it
        boolean ocidNeeded = McpDeploymentStep.PUSH_IMAGE.isCompleted(record) && storedOcid() == null;
        ocidPanel.setVisible(ocidNeeded);
        if (!ocidNeeded && storedOcid() != null) ocidField.setText(storedOcid());

        boolean configured = record.getDeployment() != null && record.getDeployment().getNamespace() != null;
        deployButton.setEnabled(configured);
        deployButton.setText(nextStep() == McpDeploymentStep.BUILD_IMAGE
                ? txt("msg.mcp.button.Deploy")
                : txt("msg.mcp.button.ResumeDeploy"));
    }

    /** Deploying picks up at the first step that has not succeeded. */
    private McpDeploymentStep nextStep() {
        for (McpDeploymentStep step : McpDeploymentStep.values()) {
            if (!step.isCompleted(record)) return step;
        }
        return McpDeploymentStep.CREATE_APPLICATION;
    }

    private void deploy() {
        String ocid = ocidField.getText().trim();
        McpServerDeploymentActions.deploy(ensureProject(), record, nextStep(), ocid.isEmpty() ? storedOcid() : ocid);
    }

    private @Nullable String storedOcid() {
        McpDeploymentInfo deployment = record.getDeployment();
        return deployment == null ? null : deployment.getImageOcid();
    }

    private String targetImage() {
        McpDeploymentInfo deployment = record.getDeployment();
        if (deployment == null || deployment.getNamespace() == null) {
            return txt("msg.mcp.text.RegistryNotConfigured");
        }
        return deployment.getRegionKey() + ".ocir.io/" + deployment.getNamespace()
                + "/" + deployment.getRepository() + ":" + deployment.getTag();
    }

    private void configureTarget() {
        McpServerDeploymentActions.openRegistrySettings(ensureProject(), record);
    }

    @Override
    protected @NotNull JComponent getMainComponent() {
        return mainPanel;
    }
}
