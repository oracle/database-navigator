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

package com.dbn.object.action;

import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.browser.ui.DatabaseBrowserTree;
import com.dbn.common.action.BasicAction;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.connection.ConnectionAction;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.NlsActions.ActionText;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Point;
import java.util.List;

import static com.dbn.common.ui.util.Popups.popupBuilder;
import static com.dbn.common.util.Naming.capitalizeWords;
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
public abstract class ObjectListShowAction extends BasicAction {
    private final DBObjectRef<?> sourceObject;
    private RelativePoint popupLocation;

    public ObjectListShowAction(@ActionText String text, DBObject sourceObject) {
        super(text);
        this.sourceObject = DBObjectRef.of(sourceObject);
    }

    public @Nullable List<DBObject> getRecentObjectList() {return null;}
    public abstract List<DBObject> getObjectList();
    public abstract String getTitle();
    public abstract String getEmptyListMessage();
    public abstract String getListName();

    @NotNull
    public DBObject getSourceObject() {
        return DBObjectRef.ensure(sourceObject);
    }

    @Override
    public final void actionPerformed(@NotNull AnActionEvent e) {
        DBObject sourceObject = getSourceObject();
        Project project = sourceObject.getProject();
        String listName = getListName();
        String title = txt("msg.objects.title.LoadingObjects", capitalizeWords(listName));
        ConnectionAction.invoke(title, true, sourceObject,
                action -> Progress.prompt(project, sourceObject, true,
                        txt("prc.objects.title.LoadingObjects"),
                        txt("prc.objects.text.LoadingObjects", listName),
                        progress -> showObjectList(e.getDataContext(), action)));
    }

    private void showObjectList(DataContext dataContext, ConnectionAction action) {
        if (!action.isCancelled()) {
            List<DBObject> recentObjectList = getRecentObjectList();
            List<DBObject> objects = getObjectList();
            if (action.isCancelled()) return;

            Dispatch.run(() -> {
                if (objects.isEmpty()) {
                    JLabel label = new JLabel(getEmptyListMessage(), Icons.EXEC_MESSAGES_INFO, SwingConstants.LEFT);
                    label.setBorder(JBUI.Borders.empty(8));
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.add(label);
                    panel.setBackground(Colors.LIGHT_BLUE);
                    ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, null);
                    JBPopup popup = popupBuilder.createPopup();
                    showPopup(popup);
                } else {
                    ObjectListActionGroup actionGroup = new ObjectListActionGroup(this, objects, recentObjectList);
                    ListPopup popup = popupBuilder(actionGroup, dataContext).
                            withTitle(getTitle()).
                            withMaxRowCount(10).
                            withSpeedSearch().
                            build();

                    showPopup(popup);
                }
            });
        }
    }

    private void showPopup(JBPopup popup) {
        if (popupLocation == null) {
            DBObject sourceObject = getSourceObject();
            DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(sourceObject.getProject());
            DatabaseBrowserTree activeBrowserTree = browserManager.getActiveBrowserTree();
            if (activeBrowserTree != null) {
                popupLocation = TreeUtil.getPointForSelection(activeBrowserTree);
                Point point = popupLocation.getPoint();
                point.setLocation(point.getX() + 20, point.getY() + 4);
            }
        }
        if (popupLocation != null) {
            popup.show(popupLocation);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
    }

    protected abstract AnAction createObjectAction(DBObject object);
}
