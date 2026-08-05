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
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Date;

import static com.dbn.common.icon.Icons.WINDOW_MCP_SERVERS;
import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.nls.NlsResources.txt;

public class McpServerDetailsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JPanel headerPanel;
    private JPanel hintsPanel;
    private JPanel imagePanel;
    private JPanel endpointPanel;
    private JPanel readmePanel;
    private JPanel tabsPanel;
    private JLabel implBadgeLabel;
    private JLabel statusBadgeLabel;
    private JLabel descriptionLabel;
    private JLabel imageLabel;
    private JLabel folderLabel;
    private JLabel builtOnLabel;
    private JLabel endpointLabel;
    private JBTextField imageField;
    private JBTextField folderField;
    private JButton imageCopyButton;
    private JButton endpointCopyButton;
    private HyperlinkLabel revealLink;
    private HyperlinkLabel endpointLink;

    private final McpServerRecord record;

    public McpServerDetailsForm(@NotNull DBNForm parent, @NotNull McpServerRecord record) {
        super(parent);
        this.record = record;

        initHeader();
        initBadges();
        initDescription();
        initHints();
        initImageRow();
        initFolderRow();
        initBuiltOnLabel();
        initEndpointRow();
        initReadmeHint();
        initConfigTabs();
    }

    private void initHeader() {
        String connectionName = connectionName();
        String title = connectionName.isEmpty() ? record.getServerName() :
                record.getServerName() + "  |  " + connectionName;
        DBNHeaderForm header = new DBNHeaderForm(this, title, WINDOW_MCP_SERVERS.get());

        ActionGroup actionGroup = (ActionGroup) ActionManager.getInstance().getAction("DBN.ActionGroup.McpServerDashboard");
        if (actionGroup != null) header.setActions(actionGroup);
        headerPanel.add(header.getComponent(), BorderLayout.CENTER);
    }

    private void initBadges() {
        initBadge(implBadgeLabel, implementationName(),
                new JBColor(new Color(225, 235, 248), new Color(55, 65, 80)));
        initBadge(statusBadgeLabel, statusName(), statusBackground());
    }

    private static void initBadge(JLabel label, String text, Color background) {
        label.setText(text);
        label.setOpaque(true);
        label.setBackground(background);
        label.setBorder(JBUI.Borders.empty(3, 7));
    }

    private Color statusBackground() {
        return switch (record.getStatus()) {
            case BUILT -> new JBColor(new Color(235, 235, 235), new Color(70, 70, 70));
            case DEPLOYED -> new JBColor(new Color(220, 242, 224), new Color(45, 80, 50));
            case STALE -> new JBColor(new Color(252, 232, 200), new Color(95, 65, 30));
        };
    }

    /**
     * Short per-implementation description of what was generated. Unlike the retired build-result
     * dialog, the dashboard does not repeat output paths here - the output folder row and the
     * README cover those.
     */
    private void initDescription() {
        McpServerImplementation implementation = record.getImplementation();
        String description =
                implementation.isContainer() ? txt("msg.mcp.text.ServerDescriptionContainer") :
                implementation.isNative() ? txt("msg.mcp.text.ServerDescriptionNative") :
                txt("msg.mcp.text.ServerDescriptionJar");

        descriptionLabel.setText("<html>" + description + "</html>");
        descriptionLabel.setForeground(JBColor.GRAY);
    }

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
        if (!record.getImplementation().isContainer() || record.getImageName() == null) {
            imagePanel.setVisible(false);
            return;
        }

        String imageName = record.getImageName();
        imageLabel.setText(txt("msg.mcp.text.ContainerImage") + ":");
        imageField.setText(imageName);
        imageField.setBackground(new JBColor(new Color(246, 242, 220), new Color(70, 65, 45)));
        imageField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, imageField.getFont().getSize()));
        initCopyButton(imageCopyButton, imageName);
    }

    private void initFolderRow() {
        Path outputPath = record.getOutputPath();
        folderLabel.setText(txt("msg.mcp.text.OutputFolder") + ":");
        folderField.setText(outputPath.toString());
        Hyperlinks.initHyperlink(revealLink, txt("msg.mcp.button.RevealFolder"),
                () -> RevealFileAction.openFile(outputPath.toFile()));
    }

    private void initBuiltOnLabel() {
        if (record.getBuildTimestamp() <= 0) {
            builtOnLabel.setVisible(false);
            return;
        }
        String builtOn = DateFormat.getDateTimeInstance().format(new Date(record.getBuildTimestamp()));
        builtOnLabel.setText(txt("msg.mcp.text.BuiltOn") + ": " + builtOn);
        builtOnLabel.setForeground(JBColor.GRAY);
    }

    private void initEndpointRow() {
        String endpoint = record.isDeployed() ? record.getDeployment().getEndpoint() : null;
        if (endpoint == null) {
            endpointPanel.setVisible(false);
            return;
        }

        endpointLabel.setText(txt("msg.mcp.text.Endpoint") + ":");
        Hyperlinks.initHyperlink(endpointLink, endpoint, endpoint);
        initCopyButton(endpointCopyButton, endpoint);
    }

    private void initReadmeHint() {
        DBNHintForm hintForm = new DBNHintForm(
                this,
                TextContent.plain(txt("msg.mcp.text.ReadmeCallout")),
                MessageType.INFO,
                true,
                txt("msg.mcp.button.OpenReadme"),
                this::openReadme);
        readmePanel.add(hintForm.getComponent(), BorderLayout.CENTER);
    }

    private void openReadme() {
        Path readmePath = record.getReadmePath();
        if (!Files.isRegularFile(readmePath)) return;

        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(readmePath);
        if (file != null) FileEditorManager.getInstance(ensureProject()).openFile(file, true);
    }

    private void initConfigTabs() {
        String artifact = resolveArtifact();
        McpClientConfiguration configuration = new McpClientConfiguration(record.getDefinition());

        JBTabbedPane tabs = new JBTabbedPane();
        boolean http = record.getTransportType().isHttp();
        tabs.addTab(http ? txt("app.mcp.title.Claude") : txt("app.mcp.title.McpConfig"),
                createConfigTab(configuration.buildClaudeJson(artifact)));
        if (http) {
            tabs.addTab(txt("app.mcp.title.Cline"), createConfigTab(configuration.buildClineJson()));
        }
        if (record.getImplementation().isContainer() && record.getImageName() != null) {
            tabs.addTab(txt("app.mcp.title.RunCommand"), createConfigTab(
                    McpRunCommands.containerRunCommand(record.getDefinition(), record.getImageName())));
        }
        tabsPanel.add(tabs, BorderLayout.CENTER);
    }

    private String resolveArtifact() {
        if (record.getArtifactPath() == null) return record.getImageName();
        return record.getOutputPath().resolve(record.getArtifactPath()).toString();
    }

    private JComponent createConfigTab(String content) {
        JBTextArea textArea = new JBTextArea(content);
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JBScrollPane(textArea), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copyButton = new JButton(txt("app.mcp.button.CopyToClipboard"));
        initCopyButton(copyButton, content);
        buttons.add(copyButton);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private static void initCopyButton(JButton button, String content) {
        button.addActionListener(e -> CopyPasteManager.getInstance().setContents(new StringSelection(content)));
    }

    private String connectionName() {
        ConnectionHandler connection = ConnectionHandler.get(record.getConnectionId());
        return connection == null ? "" : connection.getName();
    }

    private String implementationName() {
        McpServerImplementation implementation = record.getImplementation();
        if (implementation.isContainer()) return txt("msg.mcp.text.ImplementationContainer");
        if (implementation.isNative()) return txt("msg.mcp.text.ImplementationNative");
        return txt("msg.mcp.text.ImplementationJar");
    }

    private String statusName() {
        return switch (record.getStatus()) {
            case BUILT -> txt("msg.mcp.text.StatusBuilt");
            case DEPLOYED -> txt("msg.mcp.text.StatusDeployed");
            case STALE -> txt("msg.mcp.text.StatusStale");
        };
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
