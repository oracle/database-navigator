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

import com.dbn.common.action.DataKeys;
import com.dbn.common.action.DataProviders;
import com.dbn.common.color.Colors;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.latent.Latent;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.find.DataSearchComponent;
import com.dbn.data.find.SearchableDataComponent;
import com.dbn.data.grid.ui.table.basic.BasicTable;
import com.dbn.editor.data.ui.table.cell.DatasetTableCellEditor;
import com.dbn.editor.session.SessionBrowser;
import com.dbn.editor.session.model.SessionBrowserModel;
import com.dbn.editor.session.ui.table.SessionBrowserTable;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class SessionBrowserForm extends DBNFormBase implements SearchableDataComponent {
    private JPanel actionsPanel;
    private JPanel mainPanel;
    private JPanel searchPanel;
    private JPanel loadingIconPanel;
    private JPanel detailsPanel;
    private JPanel editorPanel;
    private JLabel loadingLabel;
    private JLabel loadTimestampLabel;
    private DBNScrollPane tableScrollPane;
    private SessionBrowserTable browserTable;

    private final Latent<DataSearchComponent> dataSearchComponent = Latent.basic(() -> {
        DataSearchComponent dataSearchComponent = new DataSearchComponent(SessionBrowserForm.this);
        searchPanel.add(dataSearchComponent.getComponent(), BorderLayout.CENTER);
        DataProviders.register(dataSearchComponent.getSearchField(), this);
        return dataSearchComponent;
    });

    private final WeakRef<SessionBrowser> sessionBrowser;
    private final SessionBrowserDetailsForm detailsForm;

    public SessionBrowserForm(SessionBrowser sessionBrowser) {
        super(sessionBrowser, sessionBrowser.getProject());
        this.sessionBrowser = WeakRef.of(sessionBrowser);
        editorPanel.setBorder(Borders.tableBorder(1, 0, 0, 0));
        browserTable = new SessionBrowserTable(this, sessionBrowser);
        tableScrollPane.setViewportView(browserTable);
        browserTable.initTableGutter();
        detailsForm = new SessionBrowserDetailsForm(this, sessionBrowser);
        detailsPanel.add(detailsForm.getComponent(), BorderLayout.CENTER);

        loadTimestampLabel.setForeground(Colors.HINT_COLOR);
        refreshLoadTimestamp();

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.SessionBrowser");
        setAccessibleName(actionToolbar, txt("app.sessionBrowser.aria.SessionBrowserActions"));

        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);
        loadingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
        hideLoadingHint();

        DataProviders.register(actionsPanel, this);
        Disposer.register(this, browserTable);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public SessionBrowserDetailsForm getDetailsForm() {
        return detailsForm;
    }

    public void showLoadingHint() {
        Dispatch.run(() -> {
            Failsafe.nd(this);
            loadingLabel.setVisible(true);
            loadingIconPanel.setVisible(true);
            loadTimestampLabel.setVisible(false);
            refreshLoadTimestamp();
        });
    }

    public void hideLoadingHint() {
        Dispatch.run(() -> {
            Failsafe.nd(this);
            loadingLabel.setVisible(false);
            loadingIconPanel.setVisible(false);
            refreshLoadTimestamp();
        });
    }

    public void refreshLoadTimestamp() {
        boolean visible = !loadingLabel.isVisible();
        if (visible) {
            SessionBrowserModel model = getBrowserTable().getModel();
            long timestamp = model.getTimestamp();
/*
            RegionalSettings regionalSettings = RegionalSettings.getInstance(sessionBrowser.getProject());
            String dateTime = regionalSettings.getFormatter().formatTime(new Date(timestamp));
            loadTimestampLabel.setText("Updated: " + dateTime + " (" + DateFormatUtil.formatPrettyDateTime(timestamp)+ ")");
*/

            loadTimestampLabel.setText("Updated: " + DateFormatUtil.formatPrettyDateTime(timestamp));
        }
        loadTimestampLabel.setVisible(visible);
    }


    @NotNull
    public SessionBrowserTable getBrowserTable() {
        return Failsafe.nn(browserTable);
    }

    @NotNull
    public SessionBrowser getSessionBrowser() {
        return sessionBrowser.ensure();
    }

    @NotNull
    private ConnectionHandler getConnectionHandler() {
        return getSessionBrowser().getConnection();
    }


    /*********************************************************
     *              SearchableDataComponent                  *
     *********************************************************/
    @Override
    public void showSearchHeader() {
        getBrowserTable().clearSelection();

        DataSearchComponent dataSearchComponent = getSearchComponent();
        dataSearchComponent.initializeFindModel();
        JTextComponent searchField = dataSearchComponent.getSearchField();
        if (searchPanel.isVisible()) {
            searchField.selectAll();
        } else {
            searchPanel.setVisible(true);    
        }
        searchField.requestFocus();

    }

    private DataSearchComponent getSearchComponent() {
        return dataSearchComponent.get();
    }

    @Override
    public void hideSearchHeader() {
        getSearchComponent().resetFindModel();
        searchPanel.setVisible(false);
        SessionBrowserTable editorTable = getBrowserTable();
        UserInterface.repaintAndFocus(editorTable);
    }

    @Override
    public void cancelEditActions() {}

    @Override
    public String getSelectedText() {
        TableCellEditor cellEditor = getBrowserTable().getCellEditor();
        if (cellEditor instanceof DatasetTableCellEditor) {
            DatasetTableCellEditor tableCellEditor = (DatasetTableCellEditor) cellEditor;
            return tableCellEditor.getTextField().getSelectedText();
        }
        return null;
    }

    @NotNull
    @Override
    public BasicTable getTable() {
        return getBrowserTable();
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.SESSION_BROWSER.is(dataId)) return getSessionBrowser();
        return null;
    }

    @Override
    public void disposeInner() {
        DataManager.removeDataProvider(actionsPanel);
        super.disposeInner();
        browserTable = null;
    }
}
