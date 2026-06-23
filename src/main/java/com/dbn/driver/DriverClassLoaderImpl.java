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

package com.dbn.driver;

import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.util.Measured;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.driver.DriverLibraryScanner.getLoadableJars;
import static com.dbn.driver.DriverLibraryScanner.validateClassEntryCount;
import static com.dbn.driver.DriverLibraryScanner.validateScanTime;
import static com.dbn.nls.NlsResources.txt;
import static com.intellij.openapi.progress.ProgressManager.checkCanceled;

@Slf4j
@Getter
class DriverClassLoaderImpl extends URLClassLoader implements DriverClassLoader {
    private final DriverBundleMetadata metadata;
    private final Set<File> jars = new LinkedHashSet<>();
    private final Set<Class<Driver>> drivers = new LinkedHashSet<>();
    private final Set<String> classNames = new HashSet<>();
    private final Map<String, Class> loadedClasses = new HashMap<>();

    public DriverClassLoaderImpl(DriverBundleMetadata metadata) {
        super(getUrls(metadata.getLibrary()), getParentClassLoader());
        this.metadata = metadata;
        load();
    }

    private static ClassLoader getParentClassLoader() {
        // WORKING solution - ide dependencies are inherited in the driver package
        return ClassLoader.getSystemClassLoader();

        // TODO conclude and cleanup
        //IDEAL solution - no dependencies injection (assuming driver library self-sufficiency)
        // Fails with missing slf4j api classes
        //return ClassLoader.getPlatformClassLoader();

        // OLD implementation prior to JDBC-4347 changes
        //return DriverClassLoader.class.getClassLoader()
    }


    @SneakyThrows
    private void load() {
        ProgressMonitor.setProgressText(txt("prc.connection.text.LoadingJdbcDrivers", getLibrary()));
        List<DriverLibrary> libraries = new ArrayList<>();
        for (URL url : getURLs()) {
            URI uri = url.toURI();
            File jarFile = new File(uri);
            DriverLibrary driverLibrary = new DriverLibrary(jarFile);
            classNames.addAll(driverLibrary.getClassNames());
            libraries.add(driverLibrary);
        }

        for (DriverLibrary library : libraries) {
            Measured.run("loading library " + library.getJar(), () -> load(library));
        }

        if (!metadata.isEmpty()) {
            DatabaseDriverManager driverManager = DatabaseDriverManager.getInstance();
            driverManager.setDriverMetadata(getLibrary(), metadata);
        }

    }

    private void load(DriverLibrary library) {
        ProgressMonitor.setProgressDetail(library.getJar().getAbsolutePath());
        File jar = library.getJar();
        jars.add(jar);

        try {
            Set<String> classNames = nvl(
                    getKnownDriverClassNames(),
                    library.getClassNames());

            long startedAt = System.currentTimeMillis();
            int classEntryCount = 0;
            for (String className : classNames) {
                checkCanceled();
                validateScanTime(startedAt);
                validateClassEntryCount(++classEntryCount);

                try {
                    Class<?> clazz = loadClass(className);
                    if (Driver.class.isAssignableFrom(clazz)) {
                        Class<Driver> driver = cast(clazz);
                        drivers.add(driver);
                        metadata.getDriverClassNames().add(driver.getName());
                    }
                } catch (Throwable e) {
                    conditionallyLog(e);
                    log.warn("Failed to load driver class {}. Cause: {}", className, e.getMessage());
                }
            }
        } catch (Throwable e) {
            conditionallyLog(e);
            log.warn("Failed to load drivers. Cause: {}", e.getMessage());
        }
    }

    @Nullable
    private Set<String> getKnownDriverClassNames() {
        DriverBundleMetadata previousMetadata = getPreviousMetadata();
        if (previousMetadata == null) return null;

        Set<String> driverClassNames = previousMetadata.getDriverClassNames();
        if (driverClassNames.isEmpty()) return null;

        return driverClassNames;
    }

    @Nullable
    private DriverBundleMetadata getPreviousMetadata() {
        DatabaseDriverManager driverManager = DatabaseDriverManager.getInstance();
        DriverBundleMetadata previousMetadata = driverManager.getDriverMetadata(getLibrary());
        if (previousMetadata == null) return null;
        if (previousMetadata.isEmpty()) return null;
        if (!previousMetadata.matchesSignature(metadata)) return null;

        return previousMetadata;
    }

    @Override
    public File getLibrary() {
        return this.metadata.getLibrary();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException{
        synchronized(this.getClassLoadingLock(name)) {
            Class<?> clazz = loadedClasses.get(name);
            if (clazz != null) return clazz;
            if (classNames.contains(name)) {
                try {
                    clazz = findClass(name);
                    if (clazz != null && resolve) resolveClass(clazz);
                } catch (Throwable e) {
                    conditionallyLog(e);
                }
            }
            if (clazz == null) return super.loadClass(name, resolve);

            loadedClasses.put(clazz.getName(), clazz);
            return clazz;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            DatabaseDriverManager driverManager = DatabaseDriverManager.getInstance();
            driverManager.resetDriverMetadata(getLibrary());
        }
    }

    @SneakyThrows
    private static URL[] getUrls(File library) {
        List<File> jars = getLoadableJars(library);
        if (jars.isEmpty()) throw new IOException("No files found at location");

        return jars.stream().
                map(file -> getFileUrl(file)).
                toArray(URL[]::new);
    }

    @SneakyThrows
    private static URL getFileUrl(File file) {
        return file.toURI().toURL();
    }

}
