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

import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.environment.EnvironmentTypeId;
import com.dbn.common.environment.options.EnvironmentSettings;
import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ui.ConnectionDetailSettingsForm;
import com.dbn.options.general.GeneralProjectSettings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.dbn.common.options.setting.Settings.getBoolean;
import static com.dbn.common.options.setting.Settings.getInteger;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.setBoolean;
import static com.dbn.common.options.setting.Settings.setInteger;
import static com.dbn.common.options.setting.Settings.setString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ConnectionDetailSettings extends BasicProjectConfiguration<ConnectionSettings, ConnectionDetailSettingsForm> {
    private Charset charset = StandardCharsets.UTF_8;
    private EnvironmentTypeId environmentTypeId = EnvironmentTypeId.DEFAULT;
    private boolean enableSessionManagement = true;
    private boolean enableDdlFileBinding = true;
    private boolean enableDatabaseLogging = true;
    private boolean connectAutomatically = true;
    private boolean restoreWorkspace = true;
    private boolean restoreWorkspaceDeep = false;
    private int connectivityTimeoutSeconds = 30; // default to 30 seconds instead of 5 to support longer ADB connect times
    private int idleMinutesToDisconnect = 30;
    private int idleMinutesToDisconnectPool = 5;
    private int credentialExpiryMinutes = 10;
    private int maxConnectionPoolSize = 7;


    private String alternativeStatementDelimiter;

    public ConnectionDetailSettings(ConnectionSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.connection.title.DetailSettings");
    }

    @Override
    public String getHelpTopic() {
        return "connectionPropertySettings";
    }

    /*********************************************************
     *                        Custom                         *
     *********************************************************/

    @NotNull
    public EnvironmentType getEnvironmentType() {
        EnvironmentSettings environmentSettings = GeneralProjectSettings.getInstance(getProject()).getEnvironmentSettings();
        return environmentSettings.getEnvironmentType(environmentTypeId);
    }

    public boolean isRestoreWorkspaceDeep() {
        return restoreWorkspace && restoreWorkspaceDeep;
    }

    /*********************************************************
     *                     Configuration                     *
     *********************************************************/
    @NotNull
    @Override
    public ConnectionDetailSettingsForm createConfigurationEditor() {
        return new ConnectionDetailSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "details";
    }

    @Override
    public void readConfiguration(Element element) {
        String charsetName = getString(element, "charset", "UTF-8");
        charset = Charset.forName(charsetName);

        enableSessionManagement = getBoolean(element, "session-management", enableSessionManagement);
        enableDdlFileBinding = getBoolean(element, "ddl-file-binding", enableDdlFileBinding);
        enableDatabaseLogging = getBoolean(element, "database-logging", enableDatabaseLogging);
        connectAutomatically = getBoolean(element, "connect-automatically", connectAutomatically);
        restoreWorkspace = getBoolean(element, "restore-workspace", restoreWorkspace);
        restoreWorkspaceDeep = getBoolean(element, "restore-workspace-deep", restoreWorkspaceDeep);
        environmentTypeId = EnvironmentTypeId.get(getString(element, "environment-type", EnvironmentTypeId.DEFAULT.id()));
        connectivityTimeoutSeconds = getInteger(element, "connectivity-timeout", connectivityTimeoutSeconds);
        idleMinutesToDisconnect = getInteger(element, "idle-time-to-disconnect", idleMinutesToDisconnect);
        idleMinutesToDisconnectPool = getInteger(element, "idle-time-to-disconnect-pool", idleMinutesToDisconnectPool);
        credentialExpiryMinutes = getInteger(element, "credential-expiry-time", credentialExpiryMinutes);
        maxConnectionPoolSize = getInteger(element, "max-connection-pool-size", maxConnectionPoolSize);
        alternativeStatementDelimiter = getString(element, "alternative-statement-delimiter", null);
    }

    @Override
    public void writeConfiguration(Element element) {
        setString(element, "charset", charset.name());
        
        setBoolean(element, "session-management", enableSessionManagement);
        setBoolean(element, "ddl-file-binding", enableDdlFileBinding);
        setBoolean(element, "database-logging", enableDatabaseLogging);
        setBoolean(element, "connect-automatically", connectAutomatically);
        setBoolean(element, "restore-workspace", restoreWorkspace);
        setBoolean(element, "restore-workspace-deep", restoreWorkspaceDeep);
        setString(element, "environment-type", environmentTypeId.id());
        setInteger(element, "connectivity-timeout", connectivityTimeoutSeconds);
        setInteger(element, "idle-time-to-disconnect", idleMinutesToDisconnect);
        setInteger(element, "idle-time-to-disconnect-pool", idleMinutesToDisconnectPool);
        setInteger(element, "credential-expiry-time", credentialExpiryMinutes);
        setInteger(element, "max-connection-pool-size", maxConnectionPoolSize);
        setString(element, "alternative-statement-delimiter", Commons.nvl(alternativeStatementDelimiter, ""));
    }

    public ConnectionId getConnectionId() {
        ConnectionSettings parent = getParent();
        return parent == null ? null : parent.getConnectionId();
    }
}
