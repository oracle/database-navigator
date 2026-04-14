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
                               String fullJson,
                               String fragmentJson) {
        super(parent);
        mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.add(createHeaderLabel(configPath, jarPath, walletPath, sourceProjectPath), BorderLayout.NORTH);
        mainPanel.add(createConfigTabs(fullJson, fragmentJson), BorderLayout.CENTER);
    }

    @NotNull
    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private JLabel createHeaderLabel(String configPath, String jarPath, String walletPath, String sourceProjectPath) {
        String headerHtml = "<html>"
                + "<b>MCP server built successfully.</b><br><br>"
                + "Built JAR: " + escapeHtml(jarPath) + "<br>"
                + "Config: " + escapeHtml(configPath) + "<br>"
                + "Wallet: " + escapeHtml(walletPath) + "<br>"
                + "Source project: " + escapeHtml(sourceProjectPath) + "<br><br>"
                + "<b>Next steps:</b><br>"
                + "1. Copy the JSON below into your MCP client configuration (e.g. Claude Desktop).<br>"
                + "2. Keep the <code>wallet/</code> folder private.<br>"
                + "3. See <code>README.md</code> in the output folder for full run and customization details."
                + "</html>";
        return new JLabel(headerHtml);
    }

    private JBTabbedPane createConfigTabs(String fullJson, String fragmentJson) {
        JBTabbedPane tabs = new JBTabbedPane();
        tabs.addTab("Full file", createConfigTab(fullJson));
        tabs.addTab("Fragment", createConfigTab(fragmentJson));
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
