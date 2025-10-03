/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.tool.config.ui;

import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.AssistantToolCache;
import com.dbn.assistant.tool.AssistantToolCategory;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.assistant.tool.config.AssistantToolSettings;
import com.dbn.common.text.TextContent;
import com.dbn.common.text.TextResources;
import com.dbn.common.ui.Layouts;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.connection.ConnectionHandler;
import com.intellij.util.containers.ContainerUtil;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.util.Map;

import static com.dbn.assistant.tool.AssistantToolCategory.USER_INTERACTION;
import static com.dbn.assistant.tool.AssistantToolData.getToolCategories;
import static com.dbn.common.ui.util.ClientProperty.HORIZONTAL_SCROLL_POLICY;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;

public class AssistantToolApprovalForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel toolsPanel;
    private JScrollPane toolsScrollPane;

    private final AssistantToolSettings settings;
    private final Map<AssistantToolCategory, AssistantToolApprovalCategoryForm> toolCategoryForms = ContainerUtil.createConcurrentWeakValueMap();

    public AssistantToolApprovalForm(AssistantToolApprovalDialog dialog, AssistantToolSettings settings) {
        super(dialog);
        this.settings = settings;

        initHeaderPanel();
        initHintPanel();
        initToolsPanel();

        whenShown(() -> toolsScrollPane.getVerticalScrollBar().setValue(0));
    }

    private void initHeaderPanel() {
        ConnectionHandler connection = settings.getConnection();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        this.headerPanel.add(headerForm.getComponent());
    }

    private void initHintPanel() {
        String hintContent = TextResources.get(AssistantToolApprovalForm.class, "assistant_tool_approval.html.ft");
        TextContent hintText = TextContent.html(hintContent);
        hintText.initFonts();

        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }

    private void initToolsPanel() {
        HORIZONTAL_SCROLL_POLICY.set(toolsScrollPane, HORIZONTAL_SCROLLBAR_NEVER);
        Layouts.verticalBoxLayout(toolsPanel);

        AssistantToolCategory[] toolCategories = getToolCategories();
        for (AssistantToolCategory toolCategory : toolCategories) {
            // ignore interactive tools as the approval is implied by choosing one of the dynamic options
            if (toolCategory == USER_INTERACTION) continue;

            AssistantToolApprovalCategoryForm toolCategoryForm = new AssistantToolApprovalCategoryForm(this, toolCategory);
            toolCategoryForms.put(toolCategory, toolCategoryForm);
            toolsPanel.add(toolCategoryForm.getComponent());
        }

    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public AssistantToolApprovals getToolApprovals() {
        return settings.getApprovals();
    }

    public AssistantToolCache getToolCache() {
        AssistantState assistantState = settings.getAssistantState();
        return AssistantToolCache.get(assistantState);
    }
}
