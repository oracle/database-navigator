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
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.registry.McpServerRecord;
import com.dbn.mcp.registry.McpServerStatus;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTabbedPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.nls.NlsResources.txt;

public class McpServerDetailsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel headerActionsPanel;
    private JPanel hintsPanel;
    private JBTabbedPane contentTabs;
    private JLabel iconLabel;
    private JLabel nameLabel;
    private JLabel subtitleLabel;

    private final McpServerRecord record;

    public McpServerDetailsForm(@NotNull DBNForm parent, @NotNull McpServerRecord record) {
        super(parent);
        this.record = record;

        initHeader();
        initSubtitle();
        initHints();
        initContentTabs();
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

    /** Tabs are populated here rather than in the form, as every other DBN tabbed form does. */
    private void initContentTabs() {
        McpServerOverviewForm overviewForm = new McpServerOverviewForm(this, record);
        McpServerClientSetupForm clientSetupForm = new McpServerClientSetupForm(this, record);
        McpServerBuildOutputForm buildOutputForm = new McpServerBuildOutputForm(this, record);

        contentTabs.addTab(txt("app.mcp.title.Overview"), overviewForm.getComponent());
        contentTabs.addTab(txt("app.mcp.title.ClientSetup"), clientSetupForm.getComponent());
        contentTabs.addTab(txt("app.mcp.title.BuildOutput"), buildOutputForm.getComponent());

        // a failed build is only actionable through its output, so open on it
        if (record.getStatus() == McpServerStatus.FAILED) {
            contentTabs.setSelectedComponent(buildOutputForm.getComponent());
        }
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
