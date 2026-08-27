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

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Files;
import com.dbn.common.util.Strings;
import com.dbn.connection.AuthenticationTokenType;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectivityStatus;
import com.dbn.connection.DatabaseProtocol;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.DatabaseUrlPattern;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.ServerType;
import com.dbn.connection.config.file.DatabaseFileBundle;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.connection.config.provider.ConfigProviderSecretStore;
import com.dbn.connection.config.ui.ConnectionDatabaseSettingsForm;
import com.dbn.driver.DatabaseDriverManager;
import com.dbn.driver.DriverSource;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.getDouble;
import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setDouble;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.common.options.setting.Settings.setSensitiveString;
import static com.dbn.common.options.setting.Settings.setString;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.nvle;
import static com.dbn.connection.AuthenticationType.USER_PASSWORD;
import static com.dbn.connection.config.EasyConnectParameters.sanitizeParameters;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ConnectionDatabaseSettings extends BasicConfiguration<ConnectionSettings, ConnectionDatabaseSettingsForm> implements Cloneable<ConnectionDatabaseSettings> {

    private @NonNls String name;
    private String description;
    private DatabaseType databaseType;
    private DatabaseType derivedDatabaseType = DatabaseType.GENERIC;
    private DatabaseType confirmedDatabaseType = DatabaseType.GENERIC;
    private DatabaseUrlPattern urlPattern;
    private double databaseVersion = 9999;

    // transient session user as identified when connected the first time
    private String sessionUser;
    private boolean sessionUserConfirmed = false;

    private ConnectionConfigType configType;
    private DriverSource driverSource;
    private String driverLibrary;
    private String driver;


    private final DatabaseInfo databaseInfo;
    private final ConfigProviderInfo configProviderInfo = new ConfigProviderInfo(this);
    private final AuthenticationInfo authenticationInfo = new AuthenticationInfo(this, false);

    private transient ConnectivityStatus connectivityStatus = ConnectivityStatus.UNKNOWN;
    private transient long signature = 0;

    public ConnectionDatabaseSettings(ConnectionSettings parent, @NotNull DatabaseType databaseType, ConnectionConfigType configType) {
        super(parent);
        this.databaseType = databaseType;
        this.configType = configType;
        this.urlPattern = databaseType.getDefaultUrlPattern();
        this.databaseInfo = urlPattern.createDefaultInfo();
        this.driverSource = databaseType == DatabaseType.GENERIC ?
                DriverSource.EXTERNAL :
                DriverSource.BUNDLED;

        initAuthType(databaseType);
    }

    private void initAuthType(DatabaseType databaseType) {
        AuthenticationType authenticationType = AuthenticationType.USER_PASSWORD;
        AuthenticationType[] authTypes = databaseType.getAuthTypes();
        if (!authenticationType.isOneOf(authTypes)) {
            authenticationType = authTypes[0];
        }
        authenticationInfo.setType(authenticationType);
    }

    private void deriveDatabaseType() {
        String driver = getDriver();
        derivedDatabaseType = databaseType;
        confirmedDatabaseType = databaseType;
        if (databaseType == DatabaseType.GENERIC && Strings.isNotEmptyOrSpaces(driver)) {
            derivedDatabaseType = DatabaseType.derive(driver);
            confirmedDatabaseType = DatabaseType.resolve(driver);
        }
    }

    @Override
    @NotNull
    public ConnectionDatabaseSettingsForm createConfigurationEditor() {
        return new ConnectionDatabaseSettingsForm(this);
    }

    public String getName() {
        return nvle(name);
    }

    public String getDriver() {
        return driverSource == DriverSource.BUNDLED ? databaseType.getDriverClassName() : driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
        deriveDatabaseType();
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    public void setDatabaseType(DatabaseType databaseType) {
        if (this.databaseType != databaseType) {
            this.databaseType = databaseType;
            urlPattern = databaseType.getDefaultUrlPattern();
            databaseInfo.setUrlType(urlPattern.getUrlType());
            deriveDatabaseType();
        }
        initAuthType(databaseType);
    }

    public void setConfirmedDatabaseType(DatabaseType confirmedDatabaseType) {
        this.confirmedDatabaseType = confirmedDatabaseType;
        this.derivedDatabaseType = confirmedDatabaseType;
    }

    @Override
    public String getConfigElementName() {
        return "database";
    }

    public boolean isDatabaseInitialized() {
        DatabaseInfo databaseInfo = getDatabaseInfo();
        if (databaseInfo.getUrlType() == DatabaseUrlType.FILE) {
            // only for file based databases
            DatabaseFileBundle fileBundle = databaseInfo.getFileBundle();
            return fileBundle != null && fileBundle.isValid();
        }
        return true;
    }

    public String getConnectionUrl() {
        return  databaseInfo.isCustomUrl() ?
                databaseInfo.getUrl() :
                urlPattern.buildUrl(databaseInfo, configProviderInfo);
    }

    public String getConnectionUrlForConnect() {
        if (!isConfigFile() || databaseInfo.isCustomUrl()) {
            return getConnectionUrl();
        }

        String connectionUrl = urlPattern.buildUrl(
                databaseInfo.getVendor(),
                databaseInfo.getHost(),
                databaseInfo.getPort(),
                databaseInfo.getDatabase(),
                databaseInfo.getMainFilePath(),
                databaseInfo.ensureTnsFolder(),
                databaseInfo.getTnsProfile(),
                databaseInfo.getProtocol(),
                databaseInfo.getServerType(),
                configProviderInfo.getProviderSlug(),
                configProviderInfo.getProviderLocation(),
                getUrlParameters()
        );
        return appendConfigHttpsAuthentication(connectionUrl);
    }

    public String getConnectionUrl(String host, String port) {
        if (databaseInfo.isCustomUrl() &&
                (!urlPattern.isValid(databaseInfo.getUrl()) ||
                        Strings.isEmpty(databaseInfo.getHost()) ||
                        Strings.isEmpty(databaseInfo.getPort()))) {
            return databaseInfo.getUrl();
        } else {
            return urlPattern.buildUrl(
                    databaseInfo.getVendor(),
                    host,
                    port,
                    databaseInfo.getDatabase(),
                    databaseInfo.getMainFilePath(),
                    databaseInfo.ensureTnsFolder(),
                    databaseInfo.getTnsProfile(),
                    databaseInfo.getProtocol(),
                    databaseInfo.getServerType(),
                    configProviderInfo.getProviderSlug(),
                    configProviderInfo.getProviderLocation(),
                    getUrlParameters()
            );
        }
    }

    public boolean isConfigHttps() {
        return isConfigFile() && configProviderInfo.isConfigHttps();
    }

    public boolean isAuthenticationProvidedForConnect() {
        return isConfigHttps() || authenticationInfo.isProvided();
    }

    public boolean isConfigFile() {
        return databaseInfo.getUrlType() == DatabaseUrlType.PROVIDER;
    }

    // Oracle's HTTPS config provider expects Basic Auth credentials as URL query parameters.
    // The password is added only to the runtime connect URL and is never stored in source code.
    private String appendConfigHttpsAuthentication(String url) {
        if (!isConfigHttpsUserPasswordAuth()) return url;

        String user = authenticationInfo.getUser();
        char[] password = authenticationInfo.getPassword();
        if (Strings.isEmpty(user) || Chars.isEmpty(password)) return url;

        return url +
                (url.contains("?") ? "&" : "?") +
                "authentication=basic" +
                "&user=" + user +
                "&password=" + Chars.toString(password);
    }

    private boolean isConfigHttpsUserPasswordAuth() {
        return isConfigHttps() &&
                authenticationInfo.getType() == USER_PASSWORD;
    }

    private Map<String, String> getUrlParameters() {
        if (databaseInfo.getUrlType() != DatabaseUrlType.PROVIDER) {
            return databaseInfo.getParameters();
        }

        Map<String, String> parameters = new LinkedHashMap<>(configProviderInfo.getUrlParameters(true));
        ConfigProviderSecretStore.addRuntimeSecrets(parameters, configProviderInfo);
        return parameters;
    }

    private boolean isConfigCloudProvider() {
        return databaseInfo.getUrlType() == DatabaseUrlType.PROVIDER && configProviderInfo.isCloudProviderConfig();
    }

    public void updateSignature() {
        signature = hashCode();
    }

    @Override
    public ConnectionDatabaseSettings clone() {
        Element connectionElement = new Element(getConfigElementName());
        writeConfiguration(connectionElement);
        ConnectionDatabaseSettings clone = new ConnectionDatabaseSettings(getParent(), databaseType, configType);
        clone.readConfiguration(connectionElement);
        clone.setConnectivityStatus(getConnectivityStatus());
        return clone;
    }

    public void validate() throws ConfigurationException{
        List<String> errors = new ArrayList<>();
        DatabaseType databaseType = getDatabaseType();
// TODO: clean up. Now it is allowed generic JDBC database configuration
//        if (databaseType == DatabaseType.UNKNOWN) {
//            errors.add("Database type not provided");
//        }

        String connectionUrl = getConnectionUrl();
        if (Strings.isEmpty(connectionUrl)) {
            errors.add(databaseInfo.isCustomUrl() ?
                    txt("cfg.connection.error.DatabaseConnectionUrlMissing") :
                    txt("cfg.connection.error.DatabaseInfoMissing")
            );
        } else {
            if (!databaseInfo.isCustomUrl() && !urlPattern.isValid(connectionUrl)) {
                errors.add(txt("cfg.connection.error.DatabaseInfoInvalid"));
            }
        }
        validateConfigProvider(errors);

        if (getDriverSource() == DriverSource.EXTERNAL) {
            if (Strings.isEmpty(getDriverLibrary())) {
                errors.add(txt("cfg.connection.error.DriverLibraryMissing"));
            } else {
                String driver = getDriver();
                if (Strings.isEmpty(driver)) {
                    errors.add(txt("cfg.connection.error.DriverClassMissing"));
                } else {
                    DatabaseType driverDatabaseType = DatabaseType.resolve(driver);
                    if (databaseType != DatabaseType.GENERIC && driverDatabaseType != databaseType) {
                        errors.add(txt("cfg.connection.error.DriverDatabaseTypeMismatch"));
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder(txt("cfg.connection.error.DatabaseConfigurationInvalid"));
            for (String error : errors) {
                message.append("\n - ").append(error);
            }
            throw new ConfigurationException(message.toString());
        }
    }

    private void validateConfigProvider(List<String> errors) {
        if (!isConfigCloudProvider()) return;
        configProviderInfo.validate(errors);
    }

    @NotNull
    public ConnectionId getConnectionId() {
        return getParent().getConnectionId();
    }

    public boolean driversLoaded() {
        DatabaseDriverManager driverManager = DatabaseDriverManager.getInstance();
        if (driverSource == DriverSource.EXTERNAL) {
            File libraryFile = getDriverLibraryFile();
            return libraryFile != null && driverManager.driversLoaded(libraryFile);
        }

        if (driverSource == DriverSource.BUNDLED) {
            return driverManager.driversLoaded(databaseType);
        }

        throw new UnsupportedOperationException("Driver source " + driverSource + " is not supported");
    }

    /*********************************************************
    *                 PersistentConfiguration               *
    *********************************************************/
    @Override
    public void readConfiguration(Element element) {
        ConnectionId connectionId = ConnectionId.get(getString(element, "id", null));
        if (connectionId != null) {
            getParent().setConnectionId(connectionId);
        }

        name             = getString(element, "name", name);
        description      = getString(element, "description", description);
        databaseType     = getEnum(element, "database-type", databaseType);
        configType       = getEnum(element, "config-type", configType);
        databaseVersion  = getDouble(element, "database-version", databaseVersion);

        String url = getString(element, "url", databaseInfo.getUrl());
        DatabaseUrlType defaultUrlType =
                isEmptyOrSpaces(url) ?
                        databaseType.getDefaultUrlPattern().getUrlType() :
                        DatabaseUrlType.CUSTOM;

        DatabaseUrlType urlType = getEnum(element, "url-type", defaultUrlType);
        databaseInfo.setUrlType(urlType);
        databaseInfo.setUrl(url);

        if (urlType == DatabaseUrlType.CUSTOM) {
            urlPattern = Commons.nvl(databaseType.resolveUrlPattern(url), DatabaseUrlPattern.GENERIC);
            databaseInfo.initializeDetails(urlPattern);
            configProviderInfo.initialize(url, urlPattern, databaseInfo.getParameters());
        } else {
            databaseInfo.setHost(getString(element, "host", null));
            databaseInfo.setPort(getString(element, "port", null));
            databaseInfo.setDatabase(getString(element, "database", null));
            databaseInfo.setTnsFolder(getString(element, "tns-folder", null));
            databaseInfo.setTnsProfile(getString(element, "tns-profile", null));
            databaseInfo.setServerType(getEnum(element, "server-type", ServerType.class));
            databaseInfo.setProtocol(getEnum(element, "protocol", DatabaseProtocol.class));
            configProviderInfo.readConfiguration(element);

            Element paramsElement = element.getChild("url-parameters");
            Map<String, String> parameters = new HashMap<>();
            if (paramsElement != null) {
                List<Element> paramElements = paramsElement.getChildren();
                for (Element paramElement : paramElements) {
                    String key = stringAttribute(paramElement,"key");
                    String value = stringAttribute(paramElement, "value");
                    parameters.put(key, value);
                }
            }
            if (urlType == DatabaseUrlType.EZCONNECT) {
                parameters = sanitizeParameters(parameters, databaseInfo.getProtocol());
            }
            databaseInfo.setParameters(parameters);
            urlPattern = DatabaseUrlPattern.get(databaseType, urlType);

            if (urlType == DatabaseUrlType.FILE) {
                Element filesElement = element.getChild("files");
                DatabaseFileBundle fileBundle = new DatabaseFileBundle();
                fileBundle.readConfiguration(filesElement);
                databaseInfo.setFileBundle(fileBundle);
            }

            String databaseUrl = urlPattern.buildUrl(databaseInfo, configProviderInfo);
            databaseInfo.setDatabase(databaseUrl);
        }

        driverSource  = getEnum(element, "driver-source", driverSource);
        driverLibrary = Files.convertToAbsolutePath(getProject(), getString(element, "driver-library", driverLibrary));
        driver = getString(element, "driver", driver);

        authenticationInfo.readConfiguration(element);
        sessionUser = getString(element, "session-user", sessionUser);

        deriveDatabaseType();
        updateSignature();
    }

    @Nullable
    public File getDriverLibraryFile() {
        return isEmptyOrSpaces(driverLibrary) ?  null : new File(driverLibrary);
    }

    @Override
    public void writeConfiguration(Element element) {
        String driverLibrary = ConnectionBundleSettings.IS_IMPORT_EXPORT_ACTION.get() ?
                Files.convertToRelativePath(getProject(), this.driverLibrary) :
                this.driverLibrary;

        setString(element, "name", nvle(name));
        setString(element, "description", nvle(description));

        setEnum(element, "database-type", databaseType);
        setEnum(element, "config-type", configType);
        setDouble(element, "database-version", databaseVersion);

        setEnum(element, "driver-source", driverSource);
        setString(element, "driver-library", nvle(driverLibrary));
        setString(element, "driver", nvle(driver));
        setEnum(element, "url-type", databaseInfo.getUrlType());

        if (databaseInfo.isCustomUrl()) {
            setString(element, "url", nvle(databaseInfo.getUrl()));
        } else {
            setString(element, "host", nvle(databaseInfo.getHost()));
            setString(element, "port", nvle(databaseInfo.getPort()));
            setString(element, "database", nvle(databaseInfo.getDatabase()));
            setSensitiveString(element, "tns-folder", nvle(databaseInfo.getTnsFolder()));
            setString(element, "tns-profile", nvle(databaseInfo.getTnsProfile()));
            setEnum(element, "server-type", databaseInfo.getServerType());
            setEnum(element, "protocol", databaseInfo.getProtocol());
            configProviderInfo.writeConfiguration(element);

            Element paramsElement = newElement(element, "url-parameters");
            databaseInfo.getParameters().forEach((key, value) -> {
                if (isEmptyOrSpaces(key)) return;
                if (isEmptyOrSpaces(value)) return;

                Element paramElement = newElement(paramsElement, "parameter");
                setStringAttribute(paramElement, "key", key);
                setStringAttribute(paramElement, "value", value);
            });

            DatabaseFileBundle fileBundle = databaseInfo.getFileBundle();
            if (fileBundle != null) {
                Element filesElement = newElement(element, "files");
                fileBundle.writeConfiguration(filesElement);
            }
        }
        authenticationInfo.writeConfiguration(element);
        setString(element, "session-user", sessionUser);
    }

    public Project getProject() {
        return getParent().getProject();
    }

    public boolean isInteractiveAuthentication() {
        if (configProviderInfo.isInteractiveAuthentication()) {
            return true;
        }

        AuthenticationType authenticationType = authenticationInfo.getType();
        if (authenticationType != AuthenticationType.TOKEN) return false;

        AuthenticationTokenType tokenType = authenticationInfo.getTokenType();
        return tokenType == AuthenticationTokenType.OCI_INTERACTIVE ||
                tokenType == AuthenticationTokenType.AZURE_INTERACTIVE;
    }
}
