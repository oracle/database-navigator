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
import com.dbn.common.ui.util.Fonts;
import com.dbn.mcp.deploy.McpContainerRuntimeSupport;
import com.dbn.mcp.deploy.McpDeploymentStep;
import com.dbn.mcp.registry.McpDeploymentInfo;
import com.dbn.mcp.registry.McpServerRecord;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Date;
import java.util.function.Consumer;

import static com.dbn.mcp.ui.McpServerPresentation.monospaced;
import static com.dbn.nls.NlsResources.txt;

/**
 * One deployment step. Each carries the context it alone needs - the image it produces, the login
 * it depends on, the identifier it consumes - so nothing has to be established up front.
 */
public class McpServerDeploymentStepForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel contextPanel;
    private JLabel statusIconLabel;
    private JLabel titleLabel;
    private JLabel statusLabel;
    private JLabel detailLabel;
    private JButton runButton;

    private final McpServerRecord record;
    private final McpDeploymentStep step;
    private final Runnable configureHandler;

    /** Only the application step has an input, so it owns the field rather than the tab. */
    private JBTextField ocidField;

    public McpServerDeploymentStepForm(
            @NotNull DBNForm parent,
            @NotNull McpServerRecord record,
            @NotNull McpDeploymentStep step,
            @NotNull Consumer<McpDeploymentStep> runHandler,
            @NotNull Runnable configureHandler) {
        super(parent);
        this.record = record;
        this.step = step;
        this.configureHandler = configureHandler;

        titleLabel.setText(step.getTitle());
        titleLabel.setFont(Fonts.regularBold());
        detailLabel.setText(step.getDetail());
        detailLabel.setForeground(JBColor.GRAY);

        runButton.addActionListener(e -> runHandler.accept(step));

        initContext();
        refresh();
    }

    private void initContext() {
        JComponent context = switch (step) {
            case BUILD_IMAGE -> createTargetContext();
            case PUSH_IMAGE -> createLoginContext();
            case CREATE_APPLICATION -> createOcidContext();
        };
        contextPanel.add(context, BorderLayout.CENTER);
    }

    /** The image this step produces, and the two things worth reading before producing it. */
    private JComponent createTargetContext() {
        JPanel row = new JPanel();
        row.setLayout(new javax.swing.BoxLayout(row, javax.swing.BoxLayout.X_AXIS));

        JLabel image = new JLabel(targetImage());
        image.setFont(monospaced(image));
        row.add(image);
        row.add(javax.swing.Box.createHorizontalStrut(12));
        row.add(link(txt("msg.mcp.button.EditRegistry"), configureHandler));

        if (Files.isRegularFile(dockerfilePath())) {
            row.add(javax.swing.Box.createHorizontalStrut(12));
            row.add(link(txt("msg.mcp.button.ViewDockerfile"), this::openDockerfile));
        }
        return row;
    }

    /**
     * The push is the one step that depends on something outside the IDE, so it states the exact
     * command - DB Navigator never handles those credentials itself.
     */
    private JComponent createLoginContext() {
        String runtime = McpContainerRuntimeSupport.findContainerRuntime();
        if (runtime == null) {
            return warning(txt("msg.mcp.text.ContainerRuntimeMissing"));
        }

        String command = Path.of(runtime).getFileName() + " login " + registryHost();
        JLabel label = warning(txt("msg.mcp.text.RegistryLoginRequired", command));
        label.setToolTipText(runtime);
        return label;
    }

    private JComponent createOcidContext() {
        ocidField = new JBTextField();
        ocidField.getEmptyText().setText(txt("msg.mcp.text.ContainerImageOcidHint"));

        McpDeploymentInfo deployment = record.getDeployment();
        if (deployment != null && deployment.getImageOcid() != null) {
            ocidField.setText(deployment.getImageOcid());
        }
        return ocidField;
    }

    public @Nullable String getImageOcid() {
        return ocidField == null ? null : ocidField.getText().trim();
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

    private String targetImage() {
        McpDeploymentInfo deployment = record.getDeployment();
        if (deployment == null || deployment.getNamespace() == null) {
            return txt("msg.mcp.text.RegistryNotConfigured");
        }
        return registryHost() + "/" + deployment.getNamespace()
                + "/" + deployment.getRepository() + ":" + deployment.getTag();
    }

    private String registryHost() {
        McpDeploymentInfo deployment = record.getDeployment();
        String region = deployment == null || deployment.getRegionKey() == null ? "iad" : deployment.getRegionKey();
        return region + ".ocir.io";
    }

    private Path dockerfilePath() {
        return record.getSourceProjectPath().resolve("Dockerfile.graal");
    }

    private void openDockerfile() {
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(dockerfilePath());
        if (file != null) FileEditorManager.getInstance(ensureProject()).openFile(file, true);
    }

    private static HyperlinkLabel link(String text, Runnable action) {
        HyperlinkLabel label = new HyperlinkLabel();
        Hyperlinks.initHyperlink(label, text, action);
        return label;
    }

    private static JLabel warning(String text) {
        JLabel label = new JLabel(text, AllIcons.General.ShowWarning, SwingConstants.LEADING);
        label.setIconTextGap(6);
        label.setForeground(JBColor.GRAY);
        return label;
    }

    @Override
    protected @NotNull JComponent getMainComponent() {
        return mainPanel;
    }
}
