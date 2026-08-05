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
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Date;

import static com.dbn.common.icon.Icons.WINDOW_MCP_SERVERS;
import static com.dbn.nls.NlsResources.txt;
import static java.awt.Component.LEFT_ALIGNMENT;

public class McpServerDetailsForm extends DBNFormBase {
    private final JPanel mainPanel = new JPanel(new BorderLayout());
    private final McpServerRecord record;

    public McpServerDetailsForm(@NotNull DBNForm parent, @NotNull McpServerRecord record) {
        super(parent);
        this.record = record;

        mainPanel.add(createHeader(), BorderLayout.NORTH);
        mainPanel.add(new JBScrollPane(createContent()), BorderLayout.CENTER);
    }

    private JComponent createHeader() {
        String connectionName = connectionName();
        String title = connectionName.isEmpty() ? record.getServerName() :
                record.getServerName() + "  |  " + connectionName;
        DBNHeaderForm header = new DBNHeaderForm(this, title, WINDOW_MCP_SERVERS.get());

        ActionGroup actionGroup = (ActionGroup) ActionManager.getInstance().getAction("DBN.ActionGroup.McpServerDashboard");
        if (actionGroup != null) header.setActions(actionGroup);
        return header.getComponent();
    }

    private JComponent createContent() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(JBUI.Borders.empty(12));

        // BoxLayout positions each child by its own alignmentX - one center-aligned child
        // shifts the whole column, so every child is normalized to LEFT_ALIGNMENT
        content.add(left(createBadgePanel()));
        content.add(Box.createVerticalStrut(10));
        content.add(left(createDescriptionLabel()));

        if (record.getStatus() == McpServerStatus.STALE) {
            content.add(Box.createVerticalStrut(8));
            content.add(left(createHint(txt("msg.mcp.text.StaleServerHint"), MessageType.WARNING)));
        }
        if (record.getDefinition().getTools().isEmpty()) {
            content.add(Box.createVerticalStrut(8));
            content.add(left(createHint(txt("msg.mcp.text.ImportedDefinitionIncomplete"), MessageType.INFO)));
        }

        content.add(Box.createVerticalStrut(12));
        if (record.getImplementation().isContainer()) {
            content.add(left(createValueRow(txt("msg.mcp.text.ContainerImage"), record.getImageName(), true)));
            content.add(Box.createVerticalStrut(6));
        }
        content.add(left(createOutputFolderRow()));

        if (record.getBuildTimestamp() > 0) {
            content.add(Box.createVerticalStrut(6));
            String builtOn = DateFormat.getDateTimeInstance().format(new Date(record.getBuildTimestamp()));
            content.add(left(createLabelRow(txt("msg.mcp.text.BuiltOn"), builtOn)));
        }

        if (record.isDeployed() && record.getDeployment().getEndpoint() != null) {
            content.add(Box.createVerticalStrut(6));
            content.add(left(createEndpointRow(record.getDeployment().getEndpoint())));
        }

        content.add(Box.createVerticalStrut(12));
        content.add(left(createReadmeHint()));
        content.add(Box.createVerticalStrut(12));
        content.add(left(createConfigTabs()));
        return content;
    }

    private JPanel createBadgePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panel.add(createBadge(implementationName(), new JBColor(new Color(225, 235, 248), new Color(55, 65, 80))));
        panel.add(createBadge(statusName(), statusBackground()));
        return fixedHeight(panel);
    }

    private JLabel createBadge(String text, Color background) {
        JBLabel label = new JBLabel(text);
        label.setOpaque(true);
        label.setBackground(background);
        label.setBorder(JBUI.Borders.empty(3, 7));
        return label;
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
    private JComponent createDescriptionLabel() {
        McpServerImplementation implementation = record.getImplementation();
        String description =
                implementation.isContainer() ? txt("msg.mcp.text.ServerDescriptionContainer") :
                implementation.isNative() ? txt("msg.mcp.text.ServerDescriptionNative") :
                txt("msg.mcp.text.ServerDescriptionJar");

        JBLabel label = new JBLabel("<html>" + description + "</html>");
        label.setForeground(JBColor.GRAY);
        return fixedHeight(label);
    }

    private JComponent createOutputFolderRow() {
        Path outputPath = record.getOutputPath();
        JPanel row = createValueRow(txt("msg.mcp.text.OutputFolder"), outputPath.toString(), false);
        HyperlinkLabel revealLink = new HyperlinkLabel();
        Hyperlinks.initHyperlink(revealLink, txt("msg.mcp.button.RevealFolder"),
                () -> RevealFileAction.openFile(outputPath.toFile()));
        row.add(revealLink, BorderLayout.EAST);
        return fixedHeight(row);
    }

    private JComponent createEndpointRow(String endpoint) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.add(new JBLabel(txt("msg.mcp.text.Endpoint") + ":"), BorderLayout.WEST);

        HyperlinkLabel endpointLink = new HyperlinkLabel();
        Hyperlinks.initHyperlink(endpointLink, endpoint, endpoint);
        row.add(endpointLink, BorderLayout.CENTER);
        row.add(createCopyButton(endpoint), BorderLayout.EAST);
        return fixedHeight(row);
    }

    private JPanel createValueRow(String labelText, @Nullable String value, boolean highlighted) {
        String content = value == null ? "" : value;
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.add(new JBLabel(labelText + ":"), BorderLayout.WEST);

        JBTextField valueField = new JBTextField(content);
        valueField.setEditable(false);
        valueField.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        if (highlighted) {
            valueField.setBackground(new JBColor(new Color(246, 242, 220), new Color(70, 65, 45)));
            valueField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, valueField.getFont().getSize()));
        } else {
            // selectable but visually a plain value, not an input field
            valueField.setOpaque(false);
            valueField.setBackground(null);
        }
        row.add(valueField, BorderLayout.CENTER);
        if (highlighted) row.add(createCopyButton(content), BorderLayout.EAST);
        return fixedHeight(row);
    }

    /** A plain read-only label row for short values that need no selection or actions. */
    private JPanel createLabelRow(String labelText, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.add(new JBLabel(labelText + ":"), BorderLayout.WEST);
        JBLabel valueLabel = new JBLabel(value);
        valueLabel.setForeground(JBColor.GRAY);
        row.add(valueLabel, BorderLayout.CENTER);
        return fixedHeight(row);
    }

    private JComponent createReadmeHint() {
        DBNHintForm hintForm = new DBNHintForm(
                this,
                TextContent.plain(txt("msg.mcp.text.ReadmeCallout")),
                MessageType.INFO,
                true,
                txt("msg.mcp.button.OpenReadme"),
                this::openReadme);
        return hintForm.getComponent();
    }

    private JComponent createHint(String text, MessageType messageType) {
        return new DBNHintForm(this, TextContent.plain(text), messageType, false).getComponent();
    }

    private void openReadme() {
        Path readmePath = record.getReadmePath();
        if (!Files.isRegularFile(readmePath)) return;

        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(readmePath);
        if (file != null) FileEditorManager.getInstance(ensureProject()).openFile(file, true);
    }

    private JComponent createConfigTabs() {
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
        // height only - the width must follow the panel, a fixed width breaks the column layout
        tabs.setPreferredSize(new Dimension(0, 260));
        return tabs;
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
        buttons.add(createCopyButton(content));
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JButton createCopyButton(String content) {
        JButton button = new JButton(txt("app.mcp.button.CopyToClipboard"));
        button.addActionListener(e -> CopyPasteManager.getInstance().setContents(new StringSelection(content)));
        return button;
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

    /** Normalizes a BoxLayout child to left alignment - mixed alignments shift the whole column. */
    private static <T extends JComponent> T left(T component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        return component;
    }

    private static <T extends JComponent> T fixedHeight(T component) {
        Dimension preferredSize = component.getPreferredSize();
        component.setAlignmentX(LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height));
        return component;
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
