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
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Date;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.nls.NlsResources.txt;

public class McpServerDetailsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JPanel headerPanel;
    private JPanel hintsPanel;
    private JPanel snippetsPanel;
    private JLabel subtitleLabel;
    private JLabel descriptionLabel;
    private JLabel imageLabel;
    private JLabel imageValueLabel;
    private JLabel folderLabel;
    private JLabel folderValueLabel;
    private JLabel endpointLabel;
    private JButton imageCopyButton;
    private JButton endpointCopyButton;
    private JButton revealButton;
    private HyperlinkLabel endpointLink;
    private HyperlinkLabel readmeLink;

    private final McpServerRecord record;

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
        initReadmeLink();
        initSnippets();
    }

    private void initHeader() {
        String connectionName = connectionName();
        String title = connectionName.isEmpty() ? record.getServerName() :
                record.getServerName() + "  |  " + connectionName;
        DBNHeaderForm header = new DBNHeaderForm(this, title,
                McpServerPresentation.icon(record.getImplementation()));

        ActionGroup actionGroup = (ActionGroup) ActionManager.getInstance().getAction("DBN.ActionGroup.McpServerDashboard");
        if (actionGroup != null) header.setActions(actionGroup);
        headerPanel.add(header.getComponent(), BorderLayout.CENTER);
    }

    /** Implementation, transport and build state condensed into one muted line, IDE-style. */
    private void initSubtitle() {
        StringBuilder subtitle = new StringBuilder()
                .append(McpServerPresentation.implementationName(record.getImplementation()))
                .append(" · ")
                .append(record.getTransportType())
                .append(" · ")
                .append(McpServerPresentation.statusName(record.getStatus()));

        if (record.getBuildTimestamp() > 0) {
            subtitle.append(' ').append(DateFormat.getDateInstance(DateFormat.MEDIUM)
                    .format(new Date(record.getBuildTimestamp())));
        }
        subtitleLabel.setText(subtitle.toString());
        subtitleLabel.setForeground(record.getStatus() == McpServerStatus.BUILT ?
                JBColor.GRAY : McpServerPresentation.statusColor(record.getStatus()));
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
        folderValueLabel.setFont(monospaced(folderValueLabel));
        initIconButton(revealButton, AllIcons.Actions.MenuOpen,
                txt("msg.mcp.button.RevealFolder"), () -> RevealFileAction.openFile(outputPath.toFile()));
    }

    private void initReadmeLink() {
        Hyperlinks.initHyperlink(readmeLink, txt("msg.mcp.button.OpenReadme"), this::openReadme);
    }

    private void openReadme() {
        Path readmePath = record.getReadmePath();
        if (!Files.isRegularFile(readmePath)) return;

        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(readmePath);
        if (file != null) FileEditorManager.getInstance(ensureProject()).openFile(file, true);
    }

    /** Stacked labeled code blocks with an icon copy action, per the dashboard design. */
    private void initSnippets() {
        String artifact = resolveArtifact();
        McpClientConfiguration configuration = new McpClientConfiguration(record.getDefinition());
        boolean http = record.getTransportType().isHttp();

        JPanel stack = new JPanel();
        verticalBoxLayout(stack);

        addSnippetBlock(stack, http ? txt("app.mcp.title.Claude") : txt("app.mcp.title.McpConfig"),
                configuration.buildClaudeJson(artifact));
        if (http) {
            addSnippetBlock(stack, txt("app.mcp.title.Cline"), configuration.buildClineJson());
        }
        if (record.getImplementation().isContainer() && record.getImageName() != null) {
            addSnippetBlock(stack, txt("app.mcp.title.RunCommand"),
                    McpRunCommands.containerRunCommand(record.getDefinition(), record.getImageName()));
        }
        snippetsPanel.add(stack, BorderLayout.CENTER);
    }

    private void addSnippetBlock(JPanel stack, String title, String content) {
        JPanel headerRow = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(JBColor.GRAY);
        headerRow.add(titleLabel, BorderLayout.CENTER);

        JButton copyButton = new JButton();
        initIconButton(copyButton, AllIcons.Actions.Copy,
                txt("app.mcp.button.CopyToClipboard"), () -> copyToClipboard(content));
        headerRow.add(copyButton, BorderLayout.EAST);

        JBTextArea textArea = new JBTextArea(content);
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBorder(JBUI.Borders.empty(6, 8));

        JPanel block = new JPanel(new BorderLayout(0, 2));
        block.add(headerRow, BorderLayout.NORTH);
        block.add(new JBScrollPane(textArea), BorderLayout.CENTER);
        block.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, block.getPreferredSize().height));

        if (stack.getComponentCount() > 0) stack.add(Box.createVerticalStrut(10));
        stack.add(block);
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
