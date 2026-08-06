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
import com.dbn.mcp.deploy.McpContainerRuntimeSupport;
import com.dbn.mcp.deploy.McpDeploymentStep;
import com.dbn.mcp.registry.McpDeploymentInfo;
import com.dbn.mcp.registry.McpServerRecord;
import com.dbn.mcp.registry.McpServerRegistryListener;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.EnumMap;
import java.util.Map;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.mcp.ui.McpServerPresentation.monospaced;
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
    private JLabel registryLabel;
    private JLabel registryValueLabel;
    private JLabel runtimeLabel;
    private JLabel runtimeValueLabel;
    private JLabel ocidLabel;
    private JBTextField ocidField;
    private HyperlinkLabel registryEditLink;

    private final McpServerRecord record;
    private final Map<McpDeploymentStep, McpServerDeploymentStepForm> stepForms =
            new EnumMap<>(McpDeploymentStep.class);

    public McpServerDeploymentForm(@NotNull DBNForm parent, @NotNull McpServerRecord record) {
        super(parent);
        this.record = record;

        initIntro();
        initRegistryRow();
        initRuntimeRow();
        initOcidRow();
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

    private void initRegistryRow() {
        registryLabel.setText(txt("msg.mcp.text.TargetRegistry"));
        registryValueLabel.setFont(monospaced(registryValueLabel));
        Hyperlinks.initHyperlink(registryEditLink, txt("msg.mcp.button.EditRegistry"), this::openDeployDialog);
        updateRegistryValue();
    }

    private void updateRegistryValue() {
        McpDeploymentInfo deployment = record.getDeployment();
        String image = deployment == null ? null : imageName(deployment);
        registryValueLabel.setText(image == null ? txt("msg.mcp.text.RegistryNotConfigured") : image);
        registryValueLabel.setForeground(image == null ? JBColor.GRAY : null);
    }

    private static @Nullable String imageName(McpDeploymentInfo deployment) {
        if (deployment.getNamespace() == null || deployment.getRepository() == null) return null;
        return deployment.getRegionKey() + ".ocir.io/" + deployment.getNamespace()
                + "/" + deployment.getRepository() + ":" + deployment.getTag();
    }

    /** The runtime is resolved from fixed locations, so showing which one was found avoids guesswork. */
    private void initRuntimeRow() {
        runtimeLabel.setText(txt("msg.mcp.text.ContainerRuntime"));
        String runtime = McpContainerRuntimeSupport.findContainerRuntime();

        runtimeValueLabel.setText(runtime == null ? txt("msg.mcp.text.ContainerRuntimeMissing") : runtime);
        runtimeValueLabel.setFont(monospaced(runtimeValueLabel));
        if (runtime == null) runtimeValueLabel.setForeground(JBColor.RED);
    }

    private void initOcidRow() {
        ocidLabel.setText(txt("msg.mcp.label.ContainerImageOcid"));
        McpDeploymentInfo deployment = record.getDeployment();
        if (deployment != null && deployment.getImageOcid() != null) {
            ocidField.setText(deployment.getImageOcid());
        }
        ocidField.getEmptyText().setText(txt("msg.mcp.text.ContainerImageOcidHint"));
        onTextChange(ocidField, e -> refreshStepAvailability());
    }

    private void initSteps() {
        JPanel stack = new JPanel();
        verticalBoxLayout(stack);

        for (McpDeploymentStep step : McpDeploymentStep.values()) {
            McpServerDeploymentStepForm form = new McpServerDeploymentStepForm(this, record, step, this::runStep);
            stepForms.put(step, form);
            stack.add(form.getComponent());
        }
        stepsPanel.add(stack, BorderLayout.CENTER);
        refreshStepAvailability();
    }

    private void runStep(@NotNull McpDeploymentStep step) {
        McpServerDeploymentActions.run(ensureProject(), record, step, ocidField.getText().trim());
    }

    private void refresh() {
        updateRegistryValue();
        stepForms.values().forEach(McpServerDeploymentStepForm::refresh);
        refreshStepAvailability();
    }

    private void refreshStepAvailability() {
        String ocid = ocidField.getText().trim();
        stepForms.forEach((step, form) -> form.setRunnable(step.isRunnable(record, ocid)));
    }

    private void openDeployDialog() {
        McpServerDeploymentActions.openRegistrySettings(ensureProject(), record);
    }

    @Override
    protected @NotNull JComponent getMainComponent() {
        return mainPanel;
    }
}
