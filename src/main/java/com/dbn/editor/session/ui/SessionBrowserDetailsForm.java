/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.editor.session.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.tab.DBNTabs;
import com.dbn.common.ui.util.TabbedPanes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.DatabaseFeature;
import com.dbn.editor.session.SessionBrowser;
import com.dbn.editor.session.details.SessionDetailsTable;
import com.dbn.editor.session.details.SessionDetailsTableModel;
import com.dbn.editor.session.model.SessionBrowserModelRow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;

import static com.dbn.common.ui.util.Splitters.setSplitPaneProportion;
import static com.dbn.nls.NlsResources.txt;

public class SessionBrowserDetailsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JBScrollPane sessionDetailsTablePane;
    private JSplitPane sessionDetailsSplitPanel;
    private JBTabbedPane detailsTabbedPane;
    private final SessionDetailsTable sessionDetailsTable;
    private JPanel explainPlanPanel;

    private final WeakRef<SessionBrowser> sessionBrowser;
    private final SessionBrowserCurrentSqlPanel currentSqlPanel;

    public SessionBrowserDetailsForm(@NotNull DBNComponent parent, SessionBrowser sessionBrowser) {
        super(parent);
        this.sessionBrowser = WeakRef.of(sessionBrowser);
        sessionDetailsTable = new SessionDetailsTable(this);
        sessionDetailsTablePane.setViewportView(sessionDetailsTable);

        currentSqlPanel = new SessionBrowserCurrentSqlPanel(this, sessionBrowser);


        JComponent component = currentSqlPanel.getComponent();
        DBNTabs.initTabComponent(component, Icons.FILE_SQL_CONSOLE, null, currentSqlPanel);

        detailsTabbedPane.addTab(txt("app.sessionBrowser.title.CurrentStatement"), component);

        ConnectionHandler connection = getConnection();
        String explainPlanTitle = txt("app.sessionBrowser.title.ExplainPlan");
        if (DatabaseFeature.EXPLAIN_PLAN.isSupported(connection)) {
            explainPlanPanel = new JPanel(new BorderLayout());
            //explainPlanTabInfo.setObject(currentSqlPanel);
            detailsTabbedPane.addTab(explainPlanTitle, Icons.EXPLAIN_PLAN_RESULT, new JPanel());
        }

        TabbedPanes.onSelectionChange(detailsTabbedPane, i -> {
            String title = detailsTabbedPane.getTitleAt(i);
            if (title.equals(explainPlanTitle)) {
                // TODO
            }

        });
        setSplitPaneProportion(sessionDetailsSplitPanel, 0.2);
    }

    @NotNull
    private ConnectionHandler getConnection() {
        return getSessionBrowser().getConnection();
    }

    @NotNull
    public SessionBrowser getSessionBrowser() {
        return sessionBrowser.ensure();
    }

    public void update(@Nullable final SessionBrowserModelRow selectedRow) {
        SessionDetailsTableModel model = new SessionDetailsTableModel(selectedRow);
        sessionDetailsTable.setModel(model);
        currentSqlPanel.loadCurrentStatement();
    }

    public SessionBrowserCurrentSqlPanel getCurrentSqlPanel() {
        return currentSqlPanel;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
