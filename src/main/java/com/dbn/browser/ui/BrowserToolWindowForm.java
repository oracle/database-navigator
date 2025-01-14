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

package com.dbn.browser.ui;

import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.browser.options.BrowserDisplayMode;
import com.dbn.browser.options.DatabaseBrowserSettings;
import com.dbn.browser.options.listener.DisplayModeSettingsListener;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.object.common.DBObject;
import com.dbn.object.properties.ui.ObjectPropertiesForm;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class BrowserToolWindowForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel browserPanel;
    private JPanel objectPropertiesPanel;
    private @Getter DatabaseBrowserForm browserForm;

    private BrowserDisplayMode displayMode;
    private final ObjectPropertiesForm objectPropertiesForm;

    public BrowserToolWindowForm(Disposable parent, @NotNull Project project) {
        super(parent, project);
        //toolWindow.setIcon(dbBrowser.getIcon(0));
        DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);
        rebuild();

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.Browser.Controls");
        setAccessibleName(actionToolbar, txt("app.databaseBrowser.aria.DatabaseBrowserActions"));
        actionsPanel.add(actionToolbar.getComponent());

        objectPropertiesPanel.setVisible(browserManager.getShowObjectProperties().value());
        objectPropertiesForm = new ObjectPropertiesForm(this);
        objectPropertiesPanel.add(objectPropertiesForm.getComponent());


        ProjectEvents.subscribe(project, this, DisplayModeSettingsListener.TOPIC, mode -> changeDisplayMode(mode));
        ProjectEvents.subscribe(project, this,
                ConnectionConfigListener.TOPIC,
                ConnectionConfigListener
                        .whenSetupChanged(() -> rebuild())
                        .whenNameChanged(id -> refreshTabs(id)));
    }

    public void rebuild() {
        Project project = ensureProject();
        DatabaseBrowserSettings browserSettings = DatabaseBrowserSettings.getInstance(project);
        displayMode = browserSettings.getGeneralSettings().getDisplayMode();
        DatabaseBrowserForm oldBrowserForm = this.browserForm;

        this.browserForm =
                displayMode == BrowserDisplayMode.TABBED ? new TabbedBrowserForm(this) :
                displayMode == BrowserDisplayMode.SIMPLE ? new SimpleBrowserForm(this) :
                displayMode == BrowserDisplayMode.SELECTOR ? new SelectorBrowserForm(this) : null;

        browserPanel.removeAll();
        browserPanel.add(this.browserForm.getComponent(), BorderLayout.CENTER);
        UserInterface.repaint(browserPanel);

        Disposer.dispose(oldBrowserForm);
    }

    public DatabaseBrowserTree getBrowserTree(ConnectionId connectionId) {
        if (browserForm instanceof TabbedBrowserForm) {
            TabbedBrowserForm tabbedBrowserForm = (TabbedBrowserForm) browserForm;
            return tabbedBrowserForm.getBrowserTree(connectionId);
        }

        if (browserForm instanceof SelectorBrowserForm) {
            SelectorBrowserForm selectorBrowserForm = (SelectorBrowserForm) browserForm;
            return selectorBrowserForm.getBrowserTree(connectionId);
        }

        if (browserForm instanceof SimpleBrowserForm) {
            return browserForm.getBrowserTree();
        }

        return null;
    }



    public void showObjectProperties() {
        Project project = ensureProject();
        DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);
        DatabaseBrowserTree activeBrowserTree = browserManager.getActiveBrowserTree();
        BrowserTreeNode treeNode = activeBrowserTree == null ? null : activeBrowserTree.getSelectedNode();
        if (treeNode instanceof DBObject) {
            DBObject object = (DBObject) treeNode;
            objectPropertiesForm.setObject(object);
        }

        objectPropertiesPanel.setVisible(true);
    }

    public void hideObjectProperties() {
        objectPropertiesPanel.setVisible(false);
    }

    @Nullable
    public DatabaseBrowserTree getActiveBrowserTree() {
        return browserForm.getBrowserTree();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    private void changeDisplayMode(BrowserDisplayMode displayMode) {
        if (this.displayMode != displayMode) {
            this.displayMode = displayMode;
            rebuild();
        }
    }

    private void refreshTabs(ConnectionId connectionId) {
        if (browserForm instanceof TabbedBrowserForm && isValid(browserForm)) {
            TabbedBrowserForm tabbedBrowserForm = (TabbedBrowserForm) browserForm;
            tabbedBrowserForm.refreshTabInfo(connectionId);
        }
    }

    @Override
    public void disposeInner() {
        browserForm = Disposer.replace(browserForm, null);
        super.disposeInner();
    }
}
