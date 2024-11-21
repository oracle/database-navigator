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

import com.dbn.common.ref.WeakRefCache;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.Disposable;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Driver;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@Getter
public class DriverBundle implements Disposable {
    private final DriverBundleMetadata metadata;
    private final DriverClassLoader classLoader;
    private final WeakRefCache<ConnectionId, Map<Class<Driver>, Driver>> instances = WeakRefCache.weakKey();

    public DriverBundle(File library) {
        this.metadata = new DriverBundleMetadata(library);
        this.classLoader = new DriverClassLoaderImpl(metadata);
    }

    @Override
    public void dispose() {
        try {
            classLoader.close();
        } catch (Throwable e) {
            conditionallyLog(e);
            log.warn("Failed to dispose class loader", e);
        }
    }

    @Nullable
    public Driver getDriver(String className, ConnectionId connectionId) {
        Class<Driver> driverClass = getDriverClass(className);
        if (driverClass == null) return null;

        // cached driver instances seem to work better (at least for oracle)
        val cache = this.instances.computeIfAbsent(connectionId, id -> new ConcurrentHashMap<>());
        return cache.computeIfAbsent(driverClass, c -> createDriver(c));

        // return createDriver(driver);
    }

    @Nullable
    public Driver createDriver(String className) {
        Class<Driver> driver = getDriverClass(className);
        if (driver == null) return null;

        return createDriver(driver);
    }

    @Nullable
    public Class<Driver> getDriverClass(String className) {
        for (Class<Driver> driverClass : getDriverClasses()) {
            if (Objects.equals(driverClass.getName(), className)) {
                return driverClass;
            }
        }
        return null;
    }

    @NotNull
    @SneakyThrows
    private static Driver createDriver(Class<Driver> driverClass){
        return driverClass.getDeclaredConstructor().newInstance();
    }

    public File getLibrary() {
        return classLoader.getLibrary();
    }

    public List<Class<Driver>> getDriverClasses() {
        return classLoader.getDrivers();
    }

    public List<File> getJars() {
        return classLoader.getJars();
    }

    public boolean isEmpty() {
        return getDriverClasses().isEmpty();
    }
}
