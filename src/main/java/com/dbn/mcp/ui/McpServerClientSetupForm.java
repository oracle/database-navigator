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
import com.dbn.mcp.build.McpClientConfiguration;
import com.dbn.mcp.build.McpRunCommands;
import com.dbn.mcp.registry.McpServerRecord;
import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.mcp.ui.McpServerPresentation.copyToClipboard;
import static com.dbn.mcp.ui.McpServerPresentation.initIconButton;
import static com.dbn.nls.NlsResources.txt;

/**
 * One configuration at a time, chosen from the client selector - the snippets are largely
 * redundant variants of each other, so showing them all at once only adds scrolling.
 */
public class McpServerClientSetupForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel clientLabel;
    private JButton clientCopyButton;
    private JComboBox<ClientConfiguration> clientComboBox;
    private JBTextArea clientTextArea;

    private final McpServerRecord record;

    /** A named, copyable configuration snippet offered in the client selector. */
    record ClientConfiguration(String name, String content) {
        @Override
        public String toString() {
            return name;
        }
    }

    public McpServerClientSetupForm(@NotNull DBNForm parent, @NotNull McpServerRecord record) {
        super(parent);
        this.record = record;

        clientLabel.setText(txt("msg.mcp.text.Client"));
        clientTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        clientTextArea.setBorder(JBUI.Borders.empty(6, 8));

        clientComboBox.setModel(new DefaultComboBoxModel<>(configurations().toArray(new ClientConfiguration[0])));
        clientComboBox.addActionListener(e -> showSelectedConfiguration());
        initIconButton(clientCopyButton, AllIcons.Actions.Copy,
                txt("app.mcp.button.CopyToClipboard"), () -> copyToClipboard(clientTextArea.getText()));
        showSelectedConfiguration();
    }

    private List<ClientConfiguration> configurations() {
        McpClientConfiguration configuration = new McpClientConfiguration(record.getDefinition());
        boolean http = record.getTransportType().isHttp();

        List<ClientConfiguration> configurations = new ArrayList<>();
        configurations.add(new ClientConfiguration(
                http ? txt("app.mcp.title.Claude") : txt("app.mcp.title.McpConfig"),
                configuration.buildClaudeJson(resolveArtifact())));
        if (http) {
            configurations.add(new ClientConfiguration(txt("app.mcp.title.Cline"), configuration.buildClineJson()));
        }
        if (record.getImplementation().isContainer() && record.getImageName() != null) {
            configurations.add(new ClientConfiguration(txt("app.mcp.title.RunCommand"),
                    McpRunCommands.containerRunCommand(record.getDefinition(), record.getImageName())));
        }
        return configurations;
    }

    private void showSelectedConfiguration() {
        ClientConfiguration selected = (ClientConfiguration) clientComboBox.getSelectedItem();
        clientTextArea.setText(selected == null ? "" : selected.content());
        clientTextArea.setCaretPosition(0);
    }

    private String resolveArtifact() {
        if (record.getArtifactPath() == null) return record.getImageName();
        return record.getOutputPath().resolve(record.getArtifactPath()).toString();
    }

    @Override
    protected @NotNull JComponent getMainComponent() {
        return mainPanel;
    }
}
