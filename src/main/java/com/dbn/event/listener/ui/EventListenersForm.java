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

package com.dbn.event.listener.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.color.Colors;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.table.DBNTableWithGutter;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.dbn.event.listener.EventListenerManager;
import com.dbn.event.listener.model.DataChangeListener;
import com.dbn.event.listener.model.DataChangeListenerBundle;
import com.dbn.event.ui.EventMonitorDetailsForm;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.util.ui.AsyncProcessIcon;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ClientProperty.NO_BORDER;

public class EventListenersForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel controlPanel;
    private JPanel actionsPanel;
    private JLabel loadingLabel;
    private JPanel loadingIconPanel;
    private JPanel searchPanel;
    private DBNScrollPane listenersScrollPane;

    private @Getter DBNTableWithGutter<DataChangeListenerBundle> listenersTable;

    public EventListenersForm(EventMonitorDetailsForm parent, DataChangeListenerBundle listeners) {
        super(parent);
        initActionToolbar();
        initLoadIndicator();
        initTable(listeners);

        // start loading when the form is shown
        whenShown(() -> load());
    }

    private void initLoadIndicator() {
        loadingIconPanel.add(new AsyncProcessIcon("Loading"));
        loadingIconPanel.setVisible(false);
        loadingLabel.setVisible(false);
    }

    private void initActionToolbar() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.EventListener.Controls");
        actionsPanel.add(actionToolbar.getComponent());
        controlPanel.setBorder(Borders.lineBorder(Colors.getTableGridColor(), 0, 0, 1, 0));
    }

    private void initTable(DataChangeListenerBundle listeners) {
        listenersTable = new DBNTableWithGutter<>(this, listeners, true);
        listenersScrollPane.setViewportView(listenersTable);
        NO_BORDER.set(listenersTable, true);
    }

    public void refresh() {
        load();
    }

    private void load() {
        markLoading(true);
        Background.run(() -> {
            try {
                DataChangeListenerBundle model = listenersTable.getModel();
                model.load();
            } catch (Exception e) {
                // TODO show load exception (maybe as a banner??)
            } finally {
                markLoading(false);
            }
        });
    }

    private void markLoading(boolean loading) {
        dispatch(() -> {
            loadingIconPanel.setVisible(loading);
            loadingLabel.setVisible(loading);
            listenersTable.setLoading(loading);
        });
    }

    public void  deleteSelectedRegistrations(){

        DataChangeListenerBundle listenersTableModel = listenersTable.getModel();
        int [] selectedRows = listenersTable.getSelectedRows();
        if(selectedRows.length > 0){
            for (int i = 0; i < selectedRows.length; i++) {
                DataChangeListener dataChangeListener = listenersTableModel.getListeners().get(selectedRows[i]);
                Long regId = dataChangeListener.getRegId();
                EventListenerManager.getInstance().unregisterListenerByRegId(regId,listenersTableModel.getConnection(),dataChangeListener.getTableName(),this::refresh);
            }
        }

    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.EVENT_LISTENERS_FORM.is(dataId)) return this;
        return null;
    }

    public boolean isLoading() {
        return listenersTable.isLoading();
    }

    public void showSearchHeader(){

    }
}
