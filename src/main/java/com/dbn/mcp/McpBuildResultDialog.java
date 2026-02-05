package com.dbn.mcp;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Dialog displayed after a successful MCP server build.
 * Shows the configuration snippets needed for Claude Desktop integration.
 */
public class McpBuildResultDialog extends DialogWrapper {

    private final String propsPath;
    private final String jarPath;
    private final String fullJson;
    private final String fragmentJson;

    public McpBuildResultDialog(@Nullable Project project, 
                                 String propsPath, 
                                 String jarPath, 
                                 String fullJson, 
                                 String fragmentJson) {
        super(project);
        this.propsPath = propsPath;
        this.jarPath = jarPath;
        this.fullJson = fullJson;
        this.fragmentJson = fragmentJson;
        setTitle("MCP Build Complete");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        panel.add(createHeaderLabel(), BorderLayout.NORTH);
        panel.add(createConfigTabs(), BorderLayout.CENTER);

        return panel;
    }

    private JLabel createHeaderLabel() {
        String headerHtml = "<html>"
                + "<b>Here is the MCP server entry to add in your MCP client config.</b><br>"
                + "Saved properties: " + escapeHtml(propsPath) + "<br>"
                + "Built JAR: " + escapeHtml(jarPath)
                + "</html>";
        return new JLabel(headerHtml);
    }

    private JBTabbedPane createConfigTabs() {
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
