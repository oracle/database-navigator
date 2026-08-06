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
import com.dbn.mcp.deploy.McpDeploymentStep;
import com.dbn.mcp.registry.McpServerRecord;
import com.dbn.mcp.registry.McpServerRegistryListener;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.EnumMap;
import java.util.Map;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.nls.NlsResources.txt;

/**
 * Explains what deploying to Graal involves and lets each step be run on its own. The steps are
 * separate because they fail for unrelated reasons - a compilation error, a missing registry
 * login, a database permission - and because the image build takes minutes that should not be
 * repeated to retry the step after it.
 */
public class McpServerDeploymentForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel stepsPanel;
    private JLabel introLabel;

    private final McpServerRecord record;
    private final Map<McpDeploymentStep, McpServerDeploymentStepForm> stepForms =
            new EnumMap<>(McpDeploymentStep.class);

    public McpServerDeploymentForm(@NotNull DBNForm parent, @NotNull McpServerRecord record) {
        super(parent);
        this.record = record;

        initIntro();
        initSteps();

        ProjectEvents.subscribe(ensureProject(), this, McpServerRegistryListener.TOPIC,
                new McpServerRegistryListener() {
                    @Override
                    public void recordUpdated(McpServerRecord updated) {
                        if (!record.getOutputDirectory().equals(updated.getOutputDirectory())) return;
                        dispatch(() -> refresh());
                    }
                });
    }

    private void initIntro() {
        introLabel.setText("<html>" + txt("msg.mcp.text.DeploymentIntro") + "</html>");
        introLabel.setForeground(JBColor.GRAY);
    }

    private void initSteps() {
        JPanel stack = new JPanel();
        verticalBoxLayout(stack);

        for (McpDeploymentStep step : McpDeploymentStep.values()) {
            McpServerDeploymentStepForm form =
                    new McpServerDeploymentStepForm(this, record, step, this::runStep, this::openDeployDialog);
            stepForms.put(step, form);
            stack.add(form.getComponent());
        }
        stepsPanel.add(stack, BorderLayout.CENTER);
        refreshStepAvailability();
    }

    private void runStep(@NotNull McpDeploymentStep step) {
        McpServerDeploymentActions.run(ensureProject(), record, step, imageOcid());
    }

    private void refresh() {
        stepForms.values().forEach(McpServerDeploymentStepForm::refresh);
        refreshStepAvailability();
    }

    private void refreshStepAvailability() {
        String ocid = imageOcid();
        stepForms.forEach((step, form) -> form.setRunnable(step.isRunnable(record, ocid)));
    }

    /** The identifier lives on the step that consumes it. */
    private @Nullable String imageOcid() {
        McpServerDeploymentStepForm form = stepForms.get(McpDeploymentStep.CREATE_APPLICATION);
        return form == null ? null : form.getImageOcid();
    }

    private void openDeployDialog() {
        McpServerDeploymentActions.openRegistrySettings(ensureProject(), record);
    }

    @Override
    protected @NotNull JComponent getMainComponent() {
        return mainPanel;
    }
}
