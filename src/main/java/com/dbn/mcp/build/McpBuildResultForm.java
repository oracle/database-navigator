package com.dbn.mcp.build;

import com.dbn.common.ui.form.DBNFormBase;
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

    public McpBuildResultForm(@NotNull Disposable parent,
                               String configPath,
                               String jarPath,
                               String walletPath,
                               String sourceProjectPath,
                               boolean httpTransport,
                               String claudeSnippetJson,
                               String clineSnippetJson) {
        super(parent);
        mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.add(createHeaderLabel(configPath, jarPath, walletPath, sourceProjectPath, httpTransport), BorderLayout.NORTH);
        mainPanel.add(createConfigTabs(httpTransport, claudeSnippetJson, clineSnippetJson), BorderLayout.CENTER);
    }

    @NotNull
    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private JLabel createHeaderLabel(String configPath, String jarPath, String walletPath, String sourceProjectPath, boolean httpTransport) {
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
                + "Built JAR: " + escapeHtml(jarPath) + "<br>"
                + "Config: " + escapeHtml(configPath) + "<br>"
                + "Wallet: " + escapeHtml(walletPath) + "<br>"
                + "Source project: " + escapeHtml(sourceProjectPath) + "<br><br>"
                + "<b>Next steps:</b><br>"
                + transportSteps
                + readmeStep + readmeMessage
                + "</html>";
        return new JLabel(headerHtml);
    }

    private JBTabbedPane createConfigTabs(boolean httpTransport, String claudeSnippetJson, String clineSnippetJson) {
        JBTabbedPane tabs = new JBTabbedPane();
        tabs.addTab(httpTransport ? "Claude" : "MCP Config", createConfigTab(claudeSnippetJson));
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
