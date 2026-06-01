package com.dbn.mcp.build;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpTransportType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;

public class McpBuildResultForm extends DBNFormBase {
    private final JPanel mainPanel;
    private final McpServerDefinition definition;
    private final McpBuilderResult result;

    public McpBuildResultForm(
            @NotNull Disposable parent,
            @NotNull McpServerDefinition definition,
            @NotNull McpBuilderResult result) {
        super(parent);
        this.definition = definition;
        this.result = result;

        mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.add(createHeaderLabel(), BorderLayout.NORTH);
        mainPanel.add(createConfigTabs(), BorderLayout.CENTER);
    }

    @NotNull
    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private JLabel createHeaderLabel() {
        boolean httpTransport = isHttpTransport();
        String transportSteps = httpTransport
                ? "1. Start the JAR so it serves HTTP (see README for transport/httpPort).<br>"
                + "2. Copy the JSON below into your MCP client configuration.<br>"
                + "3. Keep the <code>wallet/</code> folder private and secure.<br>"
                : "1. Copy the JSON below into your MCP client configuration (e.g. Claude Desktop).<br>"
                + "2. Keep the <code>wallet/</code> folder private.<br>";
        String readmeStep = httpTransport ? "4." : "3.";
        String readmeMessage = httpTransport
                ? " See <code>README.md</code> in the output folder for full run details and HTTP customization (including <code>httpPort</code> changes)."
                : " See <code>README.md</code> in the output folder for full run and customization details.";

        String headerHtml = "<html>"
                + "<b>MCP server built successfully.</b><br><br>"
                + "Built JAR: " + escapeHtml(result.getServerJar().toString()) + "<br>"
                + "Config: " + escapeHtml(result.getConfigFile().toString()) + "<br>"
                + "Wallet: " + escapeHtml(result.getWalletPath()) + "<br>"
                + "Source project: " + escapeHtml(result.getProjectPath()) + "<br><br>"
                + "<b>Next steps:</b><br>"
                + transportSteps
                + readmeStep + readmeMessage
                + "</html>";
        return new JLabel(headerHtml);
    }

    private boolean isHttpTransport() {
        return definition.getTransportType() == McpTransportType.HTTP;
    }

    private JBTabbedPane createConfigTabs() {
        boolean httpTransport = isHttpTransport();
        JBTabbedPane tabs = new JBTabbedPane();
        tabs.addTab(httpTransport ? "Claude" : "MCP Config", createConfigTab(result.getClaudeSnippetJson()));
        String clineSnippetJson = result.getClineSnippetJson();
        if (httpTransport && clineSnippetJson != null) {
            tabs.addTab("Cline", createConfigTab(clineSnippetJson));
        }
        return tabs;
    }

    private JComponent createConfigTab(String content) {
        JBTextArea textArea = new JBTextArea(content);
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JBScrollPane scrollPane = new JBScrollPane(textArea);

        JPanel container = new JPanel(new BorderLayout());
        container.add(scrollPane, BorderLayout.CENTER);
        container.add(createCopyButtonPanel(content), BorderLayout.SOUTH);
        return container;
    }

    private JPanel createCopyButtonPanel(String content) {
        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e ->
                CopyPasteManager.getInstance().setContents(new StringSelection(content))
        );
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(copyButton);
        return buttonPanel;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
