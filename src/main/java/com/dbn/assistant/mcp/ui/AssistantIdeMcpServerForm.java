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

package com.dbn.assistant.mcp.ui;

import com.dbn.assistant.mcp.ide.IdeMcpServerAvailability;
import com.dbn.assistant.mcp.ide.IdeMcpServerManager;
import com.dbn.common.icon.Icons;
import com.dbn.common.text.TextContent;
import com.dbn.common.text.TextResources;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.PluginsAdvertiser;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Set;

import static com.dbn.assistant.mcp.ide.IdeMcpServerAvailability.PLUGIN_UNAVAILABLE;
import static com.dbn.assistant.mcp.ide.IdeMcpServerAvailability.SERVER_DISABLED;
import static com.dbn.assistant.mcp.ide.IdeMcpServerAvailability.SERVER_ENABLED;
import static com.dbn.assistant.mcp.ide.IdeMcpServerManager.MCP_SERVER_PLUGIN_ID;
import static com.dbn.common.thread.Dispatch.async;
import static com.dbn.common.ui.link.Hyperlinks.onHyperlinkAccess;
import static com.dbn.common.ui.util.CheckBoxes.onSelectionChange;

public class AssistantIdeMcpServerForm extends DBNFormBase {
    private JPanel mainPanel;
    private JCheckBox enableCheckBox;
    private DBNInfoLabel infoLabel;
    private DBNHyperlinkLabel installHyperlink;
    private DBNHyperlinkLabel configHyperlink;
    private JLabel serverStatusLabel;

    public AssistantIdeMcpServerForm(@Nullable Disposable parent) {
        super(parent);
        initInfoLabel();
        initAvailabilityLinks();

        updateAvailabilityLinks();


    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public boolean isServerEnabled() {
        return enableCheckBox.isSelected();
    }

    public void setServerEnabled(boolean enabled) {
        enableCheckBox.setSelected(enabled);
    }

    private void initInfoLabel() {
        String infoRawContent = TextResources.get(getClass(), "assistant_mcp_workspace_integration.html.ft");
        TextContent infoContent = TextContent.html(infoRawContent);
        infoLabel.setContent(infoContent);
        infoLabel.setHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
    }

    private void initAvailabilityLinks() {
        serverStatusLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        installHyperlink.setHyperlinkText("Install");
        configHyperlink.setHyperlinkText("Enable");
        onHyperlinkAccess(installHyperlink, e -> installMcpServerPlugin());
        onHyperlinkAccess(configHyperlink, e -> enableMcpServer());
        onSelectionChange(enableCheckBox, e -> updateAvailabilityLinks());
    }

    private void updateAvailabilityLinks() {
        installHyperlink.setVisible(false);
        configHyperlink.setVisible(false);
        serverStatusLabel.setVisible(false);

        if (!isServerEnabled()) return;

        async(mainPanel,
                () -> evaluateMcpServerAvailability(),
                a -> handleMcpServerAvailability(a));
    }

    private void handleMcpServerAvailability(IdeMcpServerAvailability a) {
        boolean enabled = isServerEnabled();
        if (a == PLUGIN_UNAVAILABLE) {
            installHyperlink.setVisible(enabled);

            serverStatusLabel.setVisible(enabled);
            serverStatusLabel.setText("MCP Server plugin not installed");
            serverStatusLabel.setIcon(Icons.COMMON_STATUS_ERROR);
        } else if (a == SERVER_DISABLED) {
            configHyperlink.setVisible(enabled);
            configHyperlink.setHyperlinkText("Enable");

            serverStatusLabel.setVisible(enabled);
            serverStatusLabel.setText("MCP Server not enabled");
            serverStatusLabel.setIcon(Icons.COMMON_STATUS_ERROR);
        } else if (a == SERVER_ENABLED) {
            configHyperlink.setVisible(enabled);
            configHyperlink.setHyperlinkText("Configure");

            serverStatusLabel.setVisible(enabled);
            serverStatusLabel.setText("MCP Server active");
            serverStatusLabel.setIcon(Icons.COMMON_STATUS_SUCCESS);
        }
    }

    private IdeMcpServerAvailability evaluateMcpServerAvailability() {
        IdeMcpServerManager serverManager = IdeMcpServerManager.getInstance();
        return serverManager.getMcpServerAvailability(true);
    }

    private void installMcpServerPlugin() {
        PluginsAdvertiser.installAndEnable(
                getProject(),
                Set.of(MCP_SERVER_PLUGIN_ID),
                true,
                true,
                () -> installHyperlink.setVisible(false)
        );
    }

    private void enableMcpServer() {
        ShowSettingsUtil.getInstance().showSettingsDialog(getProject(), "MCP Server");
        updateAvailabilityLinks();
    }
}
