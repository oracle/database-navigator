package com.dbn.mcp.build;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpTransportType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;

import static com.dbn.nls.NlsResources.txt;

public class McpBuildResultForm extends DBNFormBase {
    private static final @NonNls String CLAUDE_TAB_NAME = "Claude";
    private static final @NonNls String CLINE_TAB_NAME = "Cline";

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
        String headerHtml = txt(httpTransport ?
                        "msg.mcp.text.BuildResultHttp" :
                        "msg.mcp.text.BuildResultStdio",
                escapeHtml(result.getServerJar().toString()),
                escapeHtml(result.getConfigFile().toString()),
                escapeHtml(result.getWalletDirectory().toString()),
                escapeHtml(result.getSourceDirectory().toString()));
        return new JLabel(headerHtml);
    }

    private boolean isHttpTransport() {
        return definition.getTransportType() == McpTransportType.HTTP;
    }

    private JBTabbedPane createConfigTabs() {
        boolean httpTransport = isHttpTransport();
        JBTabbedPane tabs = new JBTabbedPane();
        tabs.addTab(httpTransport ? CLAUDE_TAB_NAME : txt("app.mcp.title.McpConfig"), createConfigTab(result.getClaudeSnippetJson()));
        String clineSnippetJson = result.getClineSnippetJson();
        if (httpTransport && clineSnippetJson != null) {
            tabs.addTab(CLINE_TAB_NAME, createConfigTab(clineSnippetJson));
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
        JButton copyButton = new JButton(txt("app.mcp.button.CopyToClipboard"));
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
