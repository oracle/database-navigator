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

package com.dbn.common.ui.form;

import com.dbn.common.color.Colors;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ui.Layouts;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.misc.DBNSelector;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionHandlerStatusListener;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;

public class DBNHeaderForm extends DBNFormBase {
    public static final LineBorder BORDER = new LineBorder(Colors.getOutlineColor());
    private JLabel objectLabel;
    private JPanel mainPanel;
    private JPanel buttonsPanel;
    private JPanel selectorPanel;
    private JPanel actionsPanel;

    public DBNHeaderForm(DBNForm parent) {
        super(parent);
        //mainPanel.setBorder(BORDER);
        objectLabel.setForeground(Colors.getLabelForeground());
    }

    public DBNHeaderForm(DBNForm parent, String title, Icon icon) {
        this(parent, title, icon, null);
    }

    public DBNHeaderForm(DBNForm parent, String title, Icon icon, Color background) {
        this(parent);
        objectLabel.setText(title);
        objectLabel.setIcon(icon);
        mainPanel.setBackground(background);
        Layouts.horizontalBoxLayout(buttonsPanel);
    }

    public DBNHeaderForm(DBNForm parent, @NotNull DBObject object) {
        this(parent);
        update(object);
    }

    public DBNHeaderForm(DBNForm parent, @NotNull DBObjectRef<?> objectRef) {
        this(parent);
        update(objectRef);
    }

    public DBNHeaderForm(DBNForm parent, @NotNull Presentable presentable) {
        this(parent);
        update(presentable);
    }

    public DBNHeaderForm(DBNForm parent, @NotNull ConnectionHandler connection) {
        this(parent);
        update(connection);
    }

    public DBNHeaderForm(DBNForm parent, @NotNull Object contextObject) {
        this(parent);
        if (contextObject instanceof DBObject) update((DBObject) contextObject); else
        if (contextObject instanceof DBObjectRef) update((DBObjectRef) contextObject); else
        if (contextObject instanceof ConnectionHandler) update((ConnectionHandler) contextObject); else
        if (contextObject instanceof Presentable) update((Presentable) contextObject); else
        if (contextObject instanceof VirtualFile) update((VirtualFile) contextObject); else
            throw new UnsupportedOperationException("Unsupported context object of type " + contextObject.getClass());
    }

    public void update(@NotNull DBObject object) {
        ConnectionHandler connection = object.getConnection();

        String connectionName = connection.getName();
        objectLabel.setText(connectionName + " - " + object.getQualifiedName());
        objectLabel.setIcon(object.getIcon());
        updateBorderAndBackground((Presentable) object);
    }

    public void update(@NotNull DBObjectRef<?> objectRef) {
        ConnectionHandler connection = objectRef.getConnection();

        String connectionName = connection == null ? "UNKNOWN" : connection.getName();
        objectLabel.setText(connectionName + " - " + objectRef.getQualifiedName());
        objectLabel.setIcon(objectRef.getObjectType().getIcon());
        updateBorderAndBackground(objectRef);
    }

    private void update(@NotNull Presentable presentable) {
        objectLabel.setText(presentable.getName());
        objectLabel.setIcon(presentable.getIcon());
        updateBorderAndBackground(presentable);
    }

    private void update(@NotNull VirtualFile presentable) {
        objectLabel.setText(presentable.getPath());
        objectLabel.setIcon(presentable.getFileType().getIcon());
    }

    private void update(@NotNull ConnectionHandler connection) {
        update((Presentable) connection);
        ConnectionId id = connection.getConnectionId();
        Project project = connection.getProject();

        ProjectEvents.subscribe(project, this, ConnectionHandlerStatusListener.TOPIC, (connectionId) -> {
            if (connectionId != id) return;

            ConnectionHandler connHandler = ConnectionHandler.get(connectionId);
            if (connHandler == null) return;

            objectLabel.setIcon(connHandler.getIcon());
        });
    }

    private void updateBorderAndBackground(Presentable presentable) {
        if (presentable instanceof DatabaseContext) {
            DatabaseContext connectionProvider = (DatabaseContext) presentable;
            updateBorderAndBackground(connectionProvider);
        }
        //mainPanel.setBorder(BORDER);
    }

    private void updateBorderAndBackground(DatabaseContext connectionProvider) {
        ConnectionHandler connection = connectionProvider.getConnection();
        Color background = null;
        if (connection != null) {
            Project project = connection.getProject();
            if (getEnvironmentSettings(project).getVisibilitySettings().getDialogHeaders().value()) {
                background = connection.getEnvironmentType().getColor();
            }
        }
        mainPanel.setBackground(Commons.nvl(background, () -> Colors.getLighterPanelBackground()));
    }

    public void setBackground(Color background) {
        mainPanel.setBackground(background);
    }

    public void setTitle(String title) {
        objectLabel.setText(title);
    }

    public void setIcon(Icon icon) {
        objectLabel.setIcon(icon);
    }

    public void addButton(AbstractButton button) {
        button.setOpaque(false);
        buttonsPanel.add(button);
    }

    public void setSelector(String tooltip, ActionGroup actions) {
        DBNSelector selector = new DBNSelector(tooltip, actions);
        selector.bindComponent(objectLabel);
        selectorPanel.add(selector);
    }

    public void setActions(ActionGroup actionGroup) {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, actionGroup);
        JComponent toolbarComponent = actionToolbar.getComponent();
        UserInterface.visitRecursively(toolbarComponent, c -> c.setOpaque(false));
        toolbarComponent.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                Component child = e.getChild();
                if (child instanceof JComponent) {
                    JComponent component = (JComponent) child;
                    UserInterface.visitRecursively(component, c -> c.setOpaque(false));
                    //component.setOpaque(false);
                }
            }
        });
        actionsPanel.add(toolbarComponent);
    }

    public DBNHeaderForm withEmptyBorder() {
        //mainPanel.setBorder(null);
        return this;
    }

    public Color getBackground() {
        return mainPanel.getBackground();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
