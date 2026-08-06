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
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.dbn.common.ui.util.Splitters;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.mcp.registry.McpServerRecord;
import com.dbn.mcp.registry.McpServerRegistry;
import com.dbn.mcp.registry.McpServerRegistryListener;
import com.dbn.mcp.registry.McpServerStatus;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.util.List;
import java.util.Map;

import static com.dbn.common.dispose.Disposer.disposeMap;
import static com.dbn.common.ui.util.Borderless.markBorderless;
import static com.dbn.nls.NlsResources.txt;

public class McpServersForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel detailsPanel;
    private JList<McpServerRecord> serversList;
    private JSplitPane splitPane;

    private final Map<String, McpServerDetailsForm> detailsForms = DisposableContainers.map(this);
    private final DBNHintForm emptyHintForm;

    @Getter
    private @Nullable McpServerRecord selectedRecord;

    public McpServersForm(@NotNull Project project) {
        super(null, project);

        emptyHintForm = new DBNHintForm(
                this,
                TextContent.plain(txt("msg.mcp.text.NoServersBuilt")),
                null,
                false);

        serversList.setCellRenderer(new ServerListCellRenderer());
        serversList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetails(serversList.getSelectedValue());
        });
        markBorderless(serversList);
        Splitters.setSplitPaneProportion(splitPane, 0.25);

        ProjectEvents.subscribe(project, this, McpServerRegistryListener.TOPIC, registryListener());
        rebuildModel();
    }

    public void selectRecord(@NotNull String outputDirectory) {
        McpServerRecord record = McpServerRegistry.getInstance(ensureProject()).getRecord(outputDirectory);
        if (record != null) serversList.setSelectedValue(record, true);
    }

    private void rebuildModel() {
        String selectedKey = selectedRecord == null ? null : selectedRecord.getOutputDirectory();
        disposeMap(detailsForms);

        DefaultListModel<McpServerRecord> model = new DefaultListModel<>();
        List<McpServerRecord> records = McpServerRegistry.getInstance(ensureProject()).getRecords();
        records.forEach(model::addElement);
        serversList.setModel(model);

        if (selectedKey != null) {
            selectRecord(selectedKey);
        }
        if (serversList.getSelectedIndex() < 0 && !records.isEmpty()) {
            serversList.setSelectedIndex(0);
        }
        if (records.isEmpty()) showDetails(null);
    }

    private void showDetails(@Nullable McpServerRecord record) {
        selectedRecord = record;
        detailsPanel.removeAll();
        if (record == null) {
            detailsPanel.add(emptyHintForm.getComponent());
        } else {
            McpServerDetailsForm detailsForm = detailsForms.computeIfAbsent(
                    record.getOutputDirectory(),
                    key -> new McpServerDetailsForm(this, record));
            detailsPanel.add(detailsForm.getComponent());
        }
        UserInterface.repaint(detailsPanel);
    }

    private McpServerRegistryListener registryListener() {
        return new McpServerRegistryListener() {
            @Override
            public void recordAdded(McpServerRecord record) {
                dispatch(() -> rebuildAndSelect(record));
            }

            @Override
            public void recordUpdated(McpServerRecord record) {
                dispatch(() -> rebuildAndSelect(record));
            }

            @Override
            public void recordRemoved(McpServerRecord record) {
                dispatch(McpServersForm.this::rebuildModel);
            }

            @Override
            public void registryReloaded() {
                dispatch(McpServersForm.this::rebuildModel);
            }
        };
    }

    private void rebuildAndSelect(McpServerRecord record) {
        rebuildModel();
        selectRecord(record.getOutputDirectory());
    }

    @Override
    public @Nullable Object getData(@NotNull String dataId) {
        if (DataKeys.MCP_SERVER_RECORD.is(dataId)) return selectedRecord;
        return null;
    }

    @Override
    public @NotNull JPanel getMainComponent() {
        return mainPanel;
    }

    private static class ServerListCellRenderer extends ColoredListCellRenderer<McpServerRecord> {
        @Override
        protected void customize(
                @NotNull JList<? extends McpServerRecord> list,
                McpServerRecord value,
                int index,
                boolean selected,
                boolean hasFocus) {
            setIcon(McpServerPresentation.icon(value.getImplementation()));
            append(value.getServerName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            append("  " + McpServerPresentation.implementationName(value.getImplementation()),
                    SimpleTextAttributes.GRAYED_ATTRIBUTES);
            append("  " + McpServerPresentation.statusName(value.getStatus()), statusAttributes(value.getStatus()));
        }

        private static SimpleTextAttributes statusAttributes(McpServerStatus status) {
            return status == McpServerStatus.BUILT ?
                    SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES :
                    new SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER,
                            McpServerPresentation.statusColor(status));
        }
    }
}
