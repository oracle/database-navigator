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

import com.dbn.common.action.DataKeys;
import com.dbn.common.message.MessageType;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.Hyperlinks;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.build.McpClientConfiguration;
import com.dbn.mcp.build.McpRunCommands;
import com.dbn.mcp.model.McpServerImplementation;
import com.dbn.mcp.registry.McpServerRecord;
import com.dbn.mcp.registry.McpServerStatus;
import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.nls.NlsResources.txt;

public class McpServerDetailsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel overviewPanel;
    private JPanel clientPanel;
    private JPanel hintsPanel;
    private JTabbedPane tabbedPane;
    private JLabel subtitleLabel;
    private JLabel descriptionLabel;
    private JLabel imageLabel;
    private JLabel imageValueLabel;
    private JLabel folderLabel;
    private JLabel folderValueLabel;
    private JLabel endpointLabel;
    private JLabel builtLabel;
    private JLabel builtValueLabel;
    private JLabel documentationLabel;
    private JLabel clientLabel;
    private JButton imageCopyButton;
    private JButton endpointCopyButton;
    private JButton revealButton;
    private JButton clientCopyButton;
    private JComboBox<ClientConfiguration> clientComboBox;
    private JBTextArea clientTextArea;
    private HyperlinkLabel endpointLink;
    private HyperlinkLabel readmeLink;

    private final McpServerRecord record;

    /** A named, copyable configuration snippet offered in the client selector. */
    private record ClientConfiguration(String name, String content) {
        @Override
        public String toString() {
            return name;
        }
    }

    public McpServerDetailsForm(@NotNull DBNForm parent, @NotNull McpServerRecord record) {
        super(parent);
        this.record = record;

        initHeader();
        initSubtitle();
        initDescription();
        initHints();
        initImageRow();
        initEndpointRow();
        initFolderRow();
        initBuiltRow();
        initDocumentationRow();
        initClientSetup();
    }

    private void initHeader() {
        DBNHeaderForm header = new DBNHeaderForm(this, record.getServerName(),
                McpServerPresentation.icon(record.getImplementation()));

        ActionGroup actionGroup = (ActionGroup) ActionManager.getInstance().getAction("DBN.ActionGroup.McpServerDashboard");
        if (actionGroup != null) header.setActions(actionGroup);
        headerPanel.add(header.getComponent(), BorderLayout.CENTER);
    }

    /** Identity metadata directly under the name: where it came from and what it is. */
    private void initSubtitle() {
        StringBuilder subtitle = new StringBuilder();
        String connectionName = connectionName();
        if (!connectionName.isEmpty()) subtitle.append(connectionName).append(" · ");

        subtitle.append(McpServerPresentation.implementationName(record.getImplementation()))
                .append(" · ")
                .append(record.getTransportType());

        McpServerStatus status = record.getStatus();
        if (status != McpServerStatus.BUILT) {
            subtitle.append(" · ").append(McpServerPresentation.statusName(status));
        }
        subtitleLabel.setText(subtitle.toString());
        subtitleLabel.setForeground(status == McpServerStatus.BUILT ?
                JBColor.GRAY : McpServerPresentation.statusColor(status));
    }

    private void initDescription() {
        McpServerImplementation implementation = record.getImplementation();
        String description =
                implementation.isContainer() ? txt("msg.mcp.text.ServerDescriptionContainer") :
                implementation.isNative() ? txt("msg.mcp.text.ServerDescriptionNative") :
                txt("msg.mcp.text.ServerDescriptionJar");

        descriptionLabel.setText("<html>" + description + "</html>");
        descriptionLabel.setForeground(JBColor.GRAY);
    }

    /** Hints are reserved for actionable conditions - never for plain information. */
    private void initHints() {
        boolean stale = record.getStatus() == McpServerStatus.STALE;
        boolean incomplete = record.getDefinition().getTools().isEmpty();
        if (!stale && !incomplete) {
            hintsPanel.setVisible(false);
            return;
        }

        JPanel stack = new JPanel();
        verticalBoxLayout(stack);
        if (stale) stack.add(createHint(txt("msg.mcp.text.StaleServerHint"), MessageType.WARNING));
        if (incomplete) stack.add(createHint(txt("msg.mcp.text.ImportedDefinitionIncomplete"), MessageType.INFO));
        hintsPanel.add(stack, BorderLayout.CENTER);
    }

    private JComponent createHint(String text, MessageType messageType) {
        return new DBNHintForm(this, TextContent.plain(text), messageType, false).getComponent();
    }

    private void initImageRow() {
        String imageName = record.getImageName();
        if (!record.getImplementation().isContainer() || imageName == null) {
            setRowVisible(false, imageLabel, imageValueLabel, imageCopyButton);
            return;
        }

        imageLabel.setText(txt("msg.mcp.text.ContainerImage"));
        imageValueLabel.setText(imageName);
        imageValueLabel.setFont(monospaced(imageValueLabel));
        initIconButton(imageCopyButton, AllIcons.Actions.Copy,
                txt("app.mcp.button.CopyToClipboard"), () -> copyToClipboard(imageName));
    }

    private void initEndpointRow() {
        String endpoint = record.isDeployed() ? record.getDeployment().getEndpoint() : null;
        if (endpoint == null) {
            setRowVisible(false, endpointLabel, endpointLink, endpointCopyButton);
            return;
        }

        endpointLabel.setText(txt("msg.mcp.text.Endpoint"));
        Hyperlinks.initHyperlink(endpointLink, endpoint, endpoint);
        initIconButton(endpointCopyButton, AllIcons.Actions.Copy,
                txt("app.mcp.button.CopyToClipboard"), () -> copyToClipboard(endpoint));
    }

    private void initFolderRow() {
        Path outputPath = record.getOutputPath();
        folderLabel.setText(txt("msg.mcp.text.OutputFolder"));
        folderValueLabel.setText(outputPath.toString());
        initIconButton(revealButton, AllIcons.Actions.MenuOpen,
                txt("msg.mcp.button.RevealFolder"), () -> RevealFileAction.openFile(outputPath.toFile()));
    }

    private void initBuiltRow() {
        if (record.getBuildTimestamp() <= 0) {
            setRowVisible(false, builtLabel, builtValueLabel);
            return;
        }
        builtLabel.setText(txt("msg.mcp.text.BuiltOn"));
        builtValueLabel.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(record.getBuildTimestamp())));
    }

    private void initDocumentationRow() {
        documentationLabel.setText(txt("msg.mcp.text.Documentation"));
        Hyperlinks.initHyperlink(readmeLink, txt("msg.mcp.text.ReadmeFile"), this::openReadme);
    }

    private void openReadme() {
        Path readmePath = record.getReadmePath();
        if (!Files.isRegularFile(readmePath)) return;

        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(readmePath);
        if (file != null) FileEditorManager.getInstance(ensureProject()).openFile(file, true);
    }

    /**
     * One configuration at a time, chosen from the client selector - the snippets are largely
     * redundant variants of each other, so showing them all at once only adds scrolling.
     */
    private void initClientSetup() {
        clientLabel.setText(txt("msg.mcp.text.Client"));
        clientTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        clientTextArea.setBorder(JBUI.Borders.empty(6, 8));

        clientComboBox.setModel(new DefaultComboBoxModel<>(clientConfigurations().toArray(new ClientConfiguration[0])));
        clientComboBox.addActionListener(e -> showSelectedConfiguration());
        initIconButton(clientCopyButton, AllIcons.Actions.Copy,
                txt("app.mcp.button.CopyToClipboard"), () -> copyToClipboard(clientTextArea.getText()));
        showSelectedConfiguration();
    }

    private List<ClientConfiguration> clientConfigurations() {
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

    /** Borderless icon button - a secondary action must not compete with the value it belongs to. */
    private static void initIconButton(JButton button, Icon icon, String tooltip, Runnable action) {
        button.setIcon(icon);
        button.setToolTipText(tooltip);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(JBUI.Borders.empty(1, 4));
        button.addActionListener(e -> action.run());
    }

    private static void copyToClipboard(String content) {
        CopyPasteManager.getInstance().setContents(new StringSelection(content));
    }

    private static Font monospaced(JComponent component) {
        return new Font(Font.MONOSPACED, Font.PLAIN, component.getFont().getSize());
    }

    private static void setRowVisible(boolean visible, JComponent... components) {
        for (JComponent component : components) component.setVisible(visible);
    }

    private String connectionName() {
        ConnectionHandler connection = ConnectionHandler.get(record.getConnectionId());
        return connection == null ? "" : connection.getName();
    }

    @Override
    public @Nullable Object getData(@NotNull String dataId) {
        if (DataKeys.MCP_SERVER_RECORD.is(dataId)) return record;
        return null;
    }

    @Override
    protected @NotNull JComponent getMainComponent() {
        return mainPanel;
    }
}
