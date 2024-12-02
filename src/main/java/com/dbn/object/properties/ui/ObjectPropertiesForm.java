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

package com.dbn.object.properties.ui;

import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.browser.model.BrowserTreeEventListener;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.environment.options.EnvironmentSettings;
import com.dbn.common.environment.options.EnvironmentVisibilitySettings;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.PooledThread;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Naming;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ObjectPropertiesForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel objectLabel;
    private JLabel objectTypeLabel;
    private JScrollPane objectPropertiesScrollPane;
    private JPanel closeActionPanel;
    private JPanel headerPanel;
    private JLabel closeLabel;
    private DBObjectRef<?> object;

    private final AtomicReference<PooledThread> refreshHandle = new AtomicReference<>();
    private final ObjectPropertiesTable objectPropertiesTable;

    public ObjectPropertiesForm(DBNForm parent) {
        super(parent);
        objectPropertiesTable = new ObjectPropertiesTable(this, new ObjectPropertiesTableModel());
        objectPropertiesScrollPane.setViewportView(objectPropertiesTable);
        objectTypeLabel.setText("Object properties:");
        objectLabel.setText("(no object selected)");

        closeLabel.setText("");
        closeLabel.setIcon(Icons.ACTION_CLOSE_SMALL);
        closeLabel.setCursor(Cursors.handCursor());
        closeLabel.addMouseListener(closeMouseListener());
        closeLabel.setToolTipText("Hide Object Properties");

        Project project = ensureProject();
        ProjectEvents.subscribe(project, this, BrowserTreeEventListener.TOPIC, browserTreeEventListener());
        ProjectEvents.subscribe(project, this, EnvironmentManagerListener.TOPIC, environmentManagerListener());
    }

    private MouseListener closeMouseListener() {
        return Mouse.listener().onClick(e -> {
            DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(ensureProject());
            browserManager.showObjectProperties(false);
        });
    }

    @NotNull
    private BrowserTreeEventListener browserTreeEventListener() {
        return new BrowserTreeEventListener() {
            @Override
            public void selectionChanged() {
                Project project = ensureProject();
                DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);
                if (!browserManager.getShowObjectProperties().value()) return;

                List<DBObject> selectedObjects = browserManager.getSelectedObjects();
                DBObject selectedObject = selectedObjects.size() == 1 ? selectedObjects.get(0) : null;
                setObject(selectedObject);
            }
        };
    }

    @NotNull
    private EnvironmentManagerListener environmentManagerListener() {
        return new EnvironmentManagerListener() {
            @Override
            public void configurationChanged(Project project) {
                EnvironmentSettings environmentSettings = getEnvironmentSettings(project);
                EnvironmentVisibilitySettings visibilitySettings = environmentSettings.getVisibilitySettings();
                boolean coloredTabs = visibilitySettings.getConnectionTabs().value();

                DBObject object = getObject();
                EnvironmentType environmentType = object == null || !coloredTabs ?
                        EnvironmentType.DEFAULT :
                        object.getEnvironmentType();

                UserInterface.setBackgroundRecursive(headerPanel, environmentType.getColor());
            }
        };
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Nullable
    public DBObject getObject() {
        return DBObjectRef.get(object);
    }

    public void setObject(@Nullable DBObject object) {
        DBObject localObject = getObject();
        if (Objects.equals(object, localObject)) return;

        this.object = object == null ? null : DBObjectRef.of(object);
        Background.run(refreshHandle, () -> {
            ObjectPropertiesTableModel tableModel = object == null ?
                    new ObjectPropertiesTableModel() :
                    new ObjectPropertiesTableModel(object.getPresentableProperties());
            Disposer.register(ObjectPropertiesForm.this, tableModel);

            Dispatch.run(() -> {
                if (object == null) {
                    objectTypeLabel.setText("Object properties:");
                    objectLabel.setText("(no object selected)");
                    objectLabel.setIcon(null);
                    UserInterface.setBackgroundRecursive(headerPanel, EnvironmentType.DEFAULT.getColor());
                } else {
                    objectLabel.setText(object.getName());
                    objectLabel.setIcon(object.getIcon());
                    objectTypeLabel.setText(Naming.capitalize(object.getTypeName()) + ":");
                    UserInterface.setBackgroundRecursive(headerPanel, object.getEnvironmentType().getColor());
                }

                ObjectPropertiesTableModel oldTableModel = (ObjectPropertiesTableModel) objectPropertiesTable.getModel();
                objectPropertiesTable.setModel(tableModel);
                objectPropertiesTable.accommodateColumnsSize();


                UserInterface.repaint(mainPanel);
                Disposer.dispose(oldTableModel);
            });
        });
    }
}
