package com.dbn.connection.config;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Files;
import com.dbn.common.util.Strings;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectivityStatus;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.DatabaseUrlPattern;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.config.file.DatabaseFileBundle;
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
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.getDouble;
import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setDouble;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.common.options.setting.Settings.setString;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Slf4j
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ConnectionDatabaseSettings extends BasicConfiguration<ConnectionSettings, ConnectionDatabaseSettingsForm> {

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

    private final DatabaseInfo databaseInfo;
    private DriverSource driverSource;
    private String driverLibrary;
    private String driver;

    private ConnectionConfigType configType;
    private final AuthenticationInfo authenticationInfo = new AuthenticationInfo(this, false);

    private transient ConnectivityStatus connectivityStatus = ConnectivityStatus.UNKNOWN;
    private transient long signature = 0;

    public ConnectionDatabaseSettings(ConnectionSettings parent, @NotNull DatabaseType databaseType, ConnectionConfigType configType) {
        super(parent);
        this.databaseType = databaseType;
        this.configType = configType;
        this.urlPattern = databaseType.getDefaultUrlPattern();
        this.databaseInfo = urlPattern.getDefaultInfo();
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
        return nvl(name);
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
        return databaseInfo.isCustomUrl() ?
                databaseInfo.getUrl() :
                urlPattern.buildUrl(databaseInfo);
    }

    public String getConnectionUrl(String host, String port) {
        if (databaseInfo.isCustomUrl()) {
            return databaseInfo.getUrl();
        } else {
            return urlPattern.buildUrl(
                    databaseInfo.getVendor(),
                    host,
                    port,
                    databaseInfo.getDatabase(),
                    databaseInfo.getMainFilePath(),
                    databaseInfo.ensureTnsFolder(),
                    databaseInfo.getTnsProfile());
        }
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
                    "Database connection url not provided" :
                    "Database information not provided (host, port, database, file)"
            );
        } else {
            if (!databaseInfo.isCustomUrl() && !urlPattern.isValid(connectionUrl)) {
                errors.add("Database information incomplete or invalid (host, port, database, file)");
            }
        }

        if (getDriverSource() == DriverSource.EXTERNAL) {
            if (Strings.isEmpty(getDriverLibrary())) {
                errors.add("JDBC driver library not provided");
            } else {
                String driver = getDriver();
                if (Strings.isEmpty(driver)) {
                    errors.add("JDBC driver not provided");
                } else {
                    DatabaseType driverDatabaseType = DatabaseType.resolve(driver);
                    if (databaseType != DatabaseType.GENERIC && driverDatabaseType != databaseType) {
                        errors.add("JDBC driver does not match the selected database type");
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder("Invalid or incomplete database configuration:");
            for (String error : errors) {
                message.append("\n - ").append(error);
            }
            // TODO NLS
            throw new ConfigurationException(message.toString());
        }
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
                Strings.isEmptyOrSpaces(url) ?
                        databaseType.getDefaultUrlPattern().getUrlType() :
                        DatabaseUrlType.CUSTOM;

        DatabaseUrlType urlType = getEnum(element, "url-type", defaultUrlType);
        databaseInfo.setUrlType(urlType);
        databaseInfo.setUrl(url);

        if (urlType == DatabaseUrlType.CUSTOM) {
            urlPattern = Commons.nvl(databaseType.resolveUrlPattern(url), DatabaseUrlPattern.GENERIC);
            databaseInfo.initializeDetails(urlPattern);
        } else {
            databaseInfo.setHost(getString(element, "host", null));
            databaseInfo.setPort(getString(element, "port", null));
            databaseInfo.setDatabase(getString(element, "database", null));
            databaseInfo.setTnsFolder(getString(element, "tns-folder", null));
            databaseInfo.setTnsProfile(getString(element, "tns-profile", null));

            urlPattern = DatabaseUrlPattern.get(databaseType, urlType);

            if (urlType == DatabaseUrlType.FILE) {
                Element filesElement = element.getChild("files");
                DatabaseFileBundle fileBundle = new DatabaseFileBundle();
                fileBundle.readConfiguration(filesElement);
                databaseInfo.setFileBundle(fileBundle);
            }

            databaseInfo.initializeUrl(urlPattern);
        }

        driverSource  = getEnum(element, "driver-source", driverSource);
        // TODO temporary backward compatibility
        if (driverSource == DriverSource.BUILTIN) driverSource = DriverSource.BUNDLED;

        driverLibrary = Files.convertToAbsolutePath(getProject(), getString(element, "driver-library", driverLibrary));
        driver = getString(element, "driver", driver);

        authenticationInfo.readConfiguration(element);
        sessionUser = getString(element, "session-user", sessionUser);


        // TODO backward compatibility (to remove)
        Element propertiesElement = element.getChild("properties");
        if (propertiesElement != null) {
            for (Element propertyElement : propertiesElement.getChildren()) {
                Map<String, String> properties = getParent().getPropertiesSettings().getProperties();
                properties.put(
                        stringAttribute(propertyElement, "key"),
                        stringAttribute(propertyElement, "value"));
            }
        }
        deriveDatabaseType();
        updateSignature();
    }

    @Nullable
    public File getDriverLibraryFile() {
        return Strings.isEmptyOrSpaces(driverLibrary) ?  null : new File(driverLibrary);
    }

    @Override
    public void writeConfiguration(Element element) {
        String driverLibrary = ConnectionBundleSettings.IS_IMPORT_EXPORT_ACTION.get() ?
                Files.convertToRelativePath(getProject(), this.driverLibrary) :
                this.driverLibrary;

        setString(element, "name", nvl(name));
        setString(element, "description", nvl(description));

        setEnum(element, "database-type", databaseType);
        setEnum(element, "config-type", configType);
        setDouble(element, "database-version", databaseVersion);

        setEnum(element, "driver-source", driverSource);
        setString(element, "driver-library", nvl(driverLibrary));
        setString(element, "driver", nvl(driver));
        setEnum(element, "url-type", databaseInfo.getUrlType());

        if (databaseInfo.isCustomUrl()) {
            setString(element, "url", nvl(databaseInfo.getUrl()));
        } else {
            setString(element, "host", nvl(databaseInfo.getHost()));
            setString(element, "port", nvl(databaseInfo.getPort()));
            setString(element, "database", nvl(databaseInfo.getDatabase()));
            setString(element, "tns-folder", nvl(databaseInfo.getTnsFolder()));
            setString(element, "tns-profile", nvl(databaseInfo.getTnsProfile()));
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
}
