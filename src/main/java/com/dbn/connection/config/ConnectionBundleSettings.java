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

package com.dbn.connection.config;

import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.thread.ThreadLocalFlag;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.config.ui.ConnectionBundleSettingsForm;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettings;
import com.dbn.options.TopLevelConfig;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.dbn.common.options.setting.Settings.newElement;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ConnectionBundleSettings extends BasicProjectConfiguration<ProjectSettings, ConnectionBundleSettingsForm> implements TopLevelConfig {
    public static final ThreadLocalFlag IS_IMPORT_EXPORT_ACTION = new ThreadLocalFlag(false);
    private final List<ConnectionSettings> connections = new CopyOnWriteArrayList<>();

    public ConnectionBundleSettings(ProjectSettings parent) {
        super(parent);
    }

    public static void init(@NotNull Project project) {
        getInstance(project);
    }

    public static ConnectionBundleSettings getInstance(@NotNull Project project) {
        return ProjectSettings.get(project).getConnectionSettings();
    }

    public ConnectionSettings getConnectionSettings(ConnectionId connectionId) {
        for (ConnectionSettings connection : connections) {
            if (connection.getConnectionId() == connectionId) {
                return connection;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public String getId() {
        return "DBNavigator.Project.ConnectionSettings";
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.connection.title.Connections");
    }

    @Override
    public String getHelpTopic() {
        return "connectionBundle";
    }

    @Override
    public ConfigId getConfigId() {
        return ConfigId.CONNECTIONS;
    }

    @Override
    public String getConfigElementName() {
        return "connections";
    }

    @Override
    public boolean isModified() {
        if (super.isModified()) {
            return true;
        }
        for (ConnectionSettings connectionSetting : connections) {
            if (connectionSetting.isModified() || connectionSetting.isNew()) return true;
        }
        return false;
    }

    @Override
    public void reset() {
        super.reset();
        for (ConnectionSettings connectionSetting : connections) {
            connectionSetting.reset();
        }
    }

    @NotNull
    @Override
    public ConnectionBundleSettings getOriginalSettings() {
        return getInstance(getProject());
    }

    /*********************************************************
    *                   UnnamedConfigurable                 *
    *********************************************************/
    @Override
    @NotNull
    public ConnectionBundleSettingsForm createConfigurationEditor() {
        return new ConnectionBundleSettingsForm(this);
    }

    /*********************************************************
     *                      Configurable                     *
     *********************************************************/
    @Override
    public void readConfiguration(Element element) {
        Project project = getProject();
        connections.clear();
        for (Element child : element.getChildren()) {
            ConnectionSettings connection = new ConnectionSettings(this);
            connection.readConfiguration(child);
            connections.add(connection);
        }

        if (!project.isDefault() && !isTransitory()) {
            ConnectionManager connectionManager = ConnectionManager.getInstance(project);
            ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
            connectionBundle.applySettings(this);
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        for (ConnectionSettings connectionSetting : connections) {
            Element connectionElement = newElement(element, "connection");
            connectionSetting.writeConfiguration(connectionElement);
        }
    }

    @Override
    public void disposeUIResources() {
        super.disposeUIResources();
        for (ConnectionSettings connectionSetting : connections) {
            connectionSetting.disposeUIResources();
        }
    }
}
