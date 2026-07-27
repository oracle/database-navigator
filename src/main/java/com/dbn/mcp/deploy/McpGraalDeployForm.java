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

package com.dbn.mcp.deploy;

import com.dbn.common.message.MessageType;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.mcp.build.McpBuilderResult;
import com.dbn.mcp.model.McpServerDefinition;
import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.nls.NlsResources.txt;

/**
 * Collects the values needed to publish the built container image to OCIR and to create the
 * Graal application. Input and validation only - the actual Docker and database work is done
 * by {@link McpGraalDeployTask}, never from a Swing listener.
 */
public class McpGraalDeployForm extends DBNFormBase {
    private static final @NonNls String DEFAULT_REGION_KEY = "iad";
    private static final @NonNls String DEFAULT_TAG = "latest";

    private final JPanel mainPanel;

    private final JBTextField applicationNameField = new JBTextField();
    private final JBTextField regionKeyField = new JBTextField(DEFAULT_REGION_KEY);
    private final JBTextField namespaceField = new JBTextField();
    private final JBTextField repositoryField = new JBTextField();
    private final JBTextField tagField = new JBTextField(DEFAULT_TAG);
    private final JBTextField ocidField = new JBTextField();

    private final JLabel localImageLabel = new JLabel();
    private final JLabel fullImageNameLabel = new JLabel();

    private Runnable inputChangeHandler = () -> {};

    public McpGraalDeployForm(
            @NotNull Disposable parent,
            @NotNull McpServerDefinition definition,
            @NotNull McpBuilderResult result) {
        super(parent);

        String serverName = definition.getServerName();
        applicationNameField.setText(serverName);
        repositoryField.setText(serverName);
        localImageLabel.setText(result.getImageName());

        mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.add(createHintPanel(), BorderLayout.NORTH);
        mainPanel.add(createFieldsPanel(), BorderLayout.CENTER);

        initChangeListeners();
        updateFullImageName();
    }

    private JComponent createHintPanel() {
        TextContent hint = TextContent.plain(txt("msg.mcp.text.GraalDeploymentPrerequisites"));
        return new DBNHintForm(this, hint, MessageType.INFO, true).getComponent();
    }

    private JComponent createFieldsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        addRow(panel, row++, txt("msg.mcp.label.LocalImage"), localImageLabel);
        addRow(panel, row++, txt("msg.mcp.label.GraalApplicationName"), applicationNameField);
        addRow(panel, row++, txt("msg.mcp.label.OcirRegion"), regionKeyField);
        addRow(panel, row++, txt("msg.mcp.label.OcirNamespace"), namespaceField);
        addRow(panel, row++, txt("msg.mcp.label.OcirRepository"), repositoryField);
        addRow(panel, row++, txt("msg.mcp.label.ImageTag"), tagField);
        addRow(panel, row++, txt("msg.mcp.label.FullImageName"), fullImageNameLabel);
        addRow(panel, row, txt("msg.mcp.label.ContainerImageOcid"), ocidField);
        return panel;
    }

    private static void addRow(JPanel panel, int row, String labelText, JComponent field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(4, 4, 4, 8);
        panel.add(new JLabel(labelText), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(4, 0, 4, 4);
        panel.add(field, fieldConstraints);
    }

    private void initChangeListeners() {
        onTextChange(regionKeyField, e -> onInputChanged());
        onTextChange(namespaceField, e -> onInputChanged());
        onTextChange(repositoryField, e -> onInputChanged());
        onTextChange(tagField, e -> onInputChanged());
        onTextChange(applicationNameField, e -> onInputChanged());
        onTextChange(ocidField, e -> onInputChanged());
    }

    private void onInputChanged() {
        updateFullImageName();
        inputChangeHandler.run();
    }

    private void updateFullImageName() {
        fullImageNameLabel.setText(getDeploymentInput().getFullImageName());
    }

    /** Invoked whenever any field changes, so the dialog can re-evaluate action availability. */
    void setInputChangeHandler(@NotNull Runnable handler) {
        this.inputChangeHandler = handler;
    }

    /** Populates the OCID resolved from the registry after a successful push. */
    void setContainerImageOcid(@NotNull String ocid) {
        ocidField.setText(ocid);
    }

    @NotNull
    McpGraalDeploymentInput getDeploymentInput() {
        return new McpGraalDeploymentInput(
                getText(applicationNameField).trim(),
                getText(regionKeyField).trim(),
                getText(namespaceField).trim(),
                getText(repositoryField).trim(),
                getText(tagField).trim(),
                getText(ocidField).trim());
    }

    @NotNull
    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
