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
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.link.Hyperlinks;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.build.McpClientConfiguration;
import com.dbn.mcp.build.McpRunCommands;
import com.dbn.mcp.registry.McpServerRecord;
import com.dbn.mcp.registry.McpServerStatus;
import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.io.FileUtil;
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
import javax.swing.SwingConstants;
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
    private JPanel headerActionsPanel;
    private JPanel imageRowPanel;
    private JPanel endpointRowPanel;
    private JLabel iconLabel;
    private JLabel nameLabel;
    private JLabel subtitleLabel;
    private JLabel statusLabel;
    private JLabel statusValueLabel;
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
        initHints();
        initStatusRow();
        initImageRow();
        initEndpointRow();
        initFolderRow();
        initBuiltRow();
        initDocumentationRow();
        initClientSetup();
    }

    private void initHeader() {
        iconLabel.setIcon(McpServerPresentation.icon(record.getImplementation()));
        nameLabel.setText(record.getServerName());
        nameLabel.setFont(Fonts.regularBold(1));

        ActionToolbar actionToolbar = Actions.createActionToolbar(
                headerActionsPanel, true, "DBN.ActionGroup.McpServerDashboard");
        headerActionsPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);
    }

    /** Identity metadata directly under the name: where it came from and what it is. */
    private void initSubtitle() {
        StringBuilder subtitle = new StringBuilder();

        // the connection icon identifies the source database (and its state) at a glance,
        // where the header icon identifies the built artifact
        ConnectionHandler connection = ConnectionHandler.get(record.getConnectionId());
        if (connection != null) {
            subtitleLabel.setIcon(connection.getIcon());
            subtitleLabel.setIconTextGap(6);
            subtitle.append(connection.getName()).append(" · ");
        }

        subtitle.append(McpServerPresentation.implementationName(record.getImplementation()))
                .append(" · ")
                .append(record.getTransportType());

        subtitleLabel.setText(subtitle.toString());
        subtitleLabel.setForeground(JBColor.GRAY);
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
        if (stale) stack.add(createHint(txt("msg.mcp.text.StaleServerHint"), AllIcons.General.ShowWarning));
        if (incomplete) stack.add(createHint(txt("msg.mcp.text.ImportedDefinitionIncomplete"), AllIcons.General.ShowInfos));
        hintsPanel.add(stack, BorderLayout.CENTER);
    }

    /**
     * A single compact line rather than a boxed hint form - at this size the framed variant spends
     * more space on padding around its icon than on the message itself.
     */
    private static JComponent createHint(String text, Icon icon) {
        JLabel hint = new JLabel(text, icon, SwingConstants.LEADING);
        hint.setIconTextGap(6);
        hint.setForeground(JBColor.GRAY);
        hint.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        return hint;
    }

    private void initImageRow() {
        String imageName = record.getImageName();
        if (!record.getImplementation().isContainer() || imageName == null) {
            setRowVisible(false, imageLabel, imageRowPanel);
            return;
        }

        imageLabel.setText(txt("msg.mcp.text.ContainerImage"));
        imageValueLabel.setText(imageName);
        imageValueLabel.setFont(monospaced(imageValueLabel));
        initIconButton(imageCopyButton, AllIcons.Actions.Copy,
                txt("app.mcp.button.CopyToClipboard"), () -> copyToClipboard(imageName));
    }

    private void initStatusRow() {
        McpServerStatus status = record.getStatus();
        statusLabel.setText(txt("msg.mcp.text.Status"));
        statusValueLabel.setText(McpServerPresentation.statusName(status));
        statusValueLabel.setIcon(McpServerPresentation.statusIcon(status));
        statusValueLabel.setIconTextGap(5);
        if (status != McpServerStatus.BUILT) {
            statusValueLabel.setForeground(McpServerPresentation.statusColor(status));
        }
    }

    /**
     * The deployed endpoint when the server runs on Graal, otherwise the local address an HTTP
     * server serves on - for an HTTP server that is the most operationally useful value here.
     */
    private void initEndpointRow() {
        String endpoint =
                record.isDeployed() ? record.getDeployment().getEndpoint() :
                record.getTransportType().isHttp() ? McpClientConfiguration.localEndpoint(record.getDefinition()) :
                null;

        if (endpoint == null) {
            setRowVisible(false, endpointLabel, endpointRowPanel);
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
        // home-relative keeps the value column narrow, so the row actions stay near their values
        folderValueLabel.setText(FileUtil.getLocationRelativeToUserHome(outputPath.toString()));
        folderValueLabel.setToolTipText(outputPath.toString());
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
