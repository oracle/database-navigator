package com.dbn.driver;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.PersistentState;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.util.Files;
import com.dbn.connection.DatabaseType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Strings.cachedLowerCase;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.driver.DatabaseDriverManager.COMPONENT_NAME;

/**
 * JDBC Driver loader.
 * <p>
 * Features:
 * <ol>
 * <li>Supports single JDBC driver library</li>
 * <li>Supports directory scanning for JDBC drivers</li>
 * <li>Isolated classloader</li>
 * <li>Reload JDBC driver libraries</li>
 * </ol>
 */
@Slf4j
@State(
        name = COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseDriverManager extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.DatabaseDriverManager";

    private final Map<File, DriverBundle> drivers = new ConcurrentHashMap<>();
    private final Map<File, DriverBundleMetadata> driverMetadata = new ConcurrentHashMap<>();
    private final Map<DatabaseType, DriverBundle> bundledDrivers = new ConcurrentHashMap<>();

    public static DatabaseDriverManager getInstance() {
        return applicationService(DatabaseDriverManager.class);
    }

    public DatabaseDriverManager() {
        super(COMPONENT_NAME);
        // TODO make this configurable
        // DriverManager.setLoginTimeout(30);
    }

    public DriverBundle loadDrivers(File libraryFile, boolean force) {
        try {
            if (force) {
                DriverBundle drivers = this.drivers.remove(libraryFile);
                Disposer.dispose(drivers);
            }
            return drivers.computeIfAbsent(libraryFile, f -> new DriverBundle(f));

        } catch (Exception e) {
            conditionallyLog(e);
            log.warn("failed to load drivers from library " + libraryFile, e);
            throw e;
        }
    }

    @Nullable
    public DriverBundleMetadata getDriverMetadata(File library) {
        return driverMetadata.get(library);
    }

    public void setDriverMetadata(File library, DriverBundleMetadata metadata) {
        driverMetadata.put(library, metadata);
    }

    public void resetDriverMetadata(File library) {
        driverMetadata.remove(library);
    }

    @Nullable
    @SneakyThrows
    private DriverBundle loadBundledDrivers(DatabaseType databaseType) {
        String libraryRoot = "bundled-jdbc-" + cachedLowerCase(databaseType.name());
        log.info("Loading driver library " + libraryRoot);

        File deploymentRoot = Files.getPluginDeploymentRoot();
        File library = Files.findFileRecursively(deploymentRoot, libraryRoot);
        if (library == null) return null;

        return loadDrivers(library, false);
    }

    @SneakyThrows
    private File getPluginLibrary() {
        Class clazz = getClass();
        URL classResource = clazz.getResource(clazz.getSimpleName() + ".class");
        if (classResource == null) return null;

        String url = classResource.toString();
        if (!url.startsWith("jar:file:")) return null;

        String path = url.replaceAll("^jar:(file:.*[.]jar)!/.*", "$1");
        return new File(new URL(path).toURI());
    }

    public boolean driversLoaded(DatabaseType databaseType) {
        return bundledDrivers.containsKey(databaseType);
    }

    public boolean driversLoaded(File libraryFile) {
        return drivers.containsKey(libraryFile);
    }

    @SneakyThrows
    public DriverBundle getBundledDrivers(DatabaseType databaseType) {
        return bundledDrivers.computeIfAbsent(databaseType, dt -> loadBundledDrivers(databaseType));
    }

    public DriverBundle getDrivers(File libraryFile) throws Exception {
        if (libraryFile.exists()) {
            return loadDrivers(libraryFile, false);
        } else {
            throw new Exception("Could not find library \"" + libraryFile.getAbsolutePath() +"\".");
        }
    }

    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Element driverClassesElement = newElement(element, "known-driver-classes");
        for (val entry : driverMetadata.entrySet()) {
            File file = entry.getKey();
            if (!file.exists()) continue;

            DriverBundleMetadata metadata = entry.getValue();
            Element libraryElement = newElement(driverClassesElement, "library");
            metadata.writeState(libraryElement);
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element driverClassesElement = element.getChild("known-driver-classes");
        if (driverClassesElement != null) {
            for (Element libraryElement : driverClassesElement.getChildren()) {
                DriverBundleMetadata metadata = new DriverBundleMetadata();
                metadata.readState(libraryElement);
                if (!metadata.isValid()) continue;

                driverMetadata.put(metadata.getLibrary(), metadata);
            }
        }
    }
}
