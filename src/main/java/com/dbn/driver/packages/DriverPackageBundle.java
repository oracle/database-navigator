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

package com.dbn.driver.packages;

import com.dbn.common.util.XmlContents;
import com.dbn.connection.DatabaseType;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Lists.convert;
import static java.util.Collections.unmodifiableList;

/**
 * DriverPackages holds metadata for supported database drivers.
 * The information is parsed from a driver-packages.xml file that includes the following structure:
 * <pre>
 * {@code
 * <driver-packages>
 *     <driver-package id="oracle-23.3-standard" name="Oracle 23.3" database-type="ORACLE">
 *         <library group-id="javax.resource" artifact-id="connector-api" version="1.5"/>
 *         <library group-id="oracle.jdbc" artifact-id="ojdbc8" version="23.3.0.23.09"/>
 *     </driver-package>
 *     <driver-package id="mysql-8.0-standard" name="MySQL 8.0.33" database-type="MYSQL">
 *         <library group-id="mysql" artifact-id="mysql-connector-j" version="8.0.33"/>
 *     </driver-package>
 *     ...
 * </driver-packages>
 * }
 * </pre>
 * It supports the association of database types and corresponding libraries.
 * Libraries are specified with groupId, artifactId, and version attributes.
 *
 * @author Ayoub Aarrasse
 */
public class DriverPackageBundle {
    private static final DriverPackageBundle INSTANCE = new DriverPackageBundle();
    private final List<DriverPackage> driverPackages;

    @SneakyThrows
    private DriverPackageBundle() {
        Element element = XmlContents.fileToElement(getClass(), "driver-packages.xml");
        List<Element> packageElements = element.getChildren("driver-package");

        driverPackages = unmodifiableList(convert(packageElements, DriverPackageBundle::createDriverPackage));
    }

    private static DriverPackage createDriverPackage(Element element) {
        String id = stringAttribute(element, "id");
        String name = stringAttribute(element, "name");
        String databaseType = stringAttribute(element, "database-type");

        List<Library> libraries = convert(element.getChildren("library"), DriverPackageBundle::createLibrary);

        return new DriverPackage(id, name, DatabaseType.resolve(databaseType), libraries);
    }

    private static Library createLibrary(Element element) {
        String groupId = stringAttribute(element, "group-id");
        String artifactId = stringAttribute(element, "artifact-id");
        String version = stringAttribute(element, "version");
        List<Developer> developers = convert(element.getChildren("developer"), DriverPackageBundle::createDeveloper);
        List<License> licenses = convert(element.getChildren("license"), DriverPackageBundle::createLicense);
        return new Library(groupId, artifactId, version, developers, licenses);
    }

    private static Developer createDeveloper(Element element) {
        String name = stringAttribute(element, "name");
        String url = stringAttribute(element, "url");
        return new Developer(name, url);
    }

    private static License createLicense(Element element) {
        String name = stringAttribute(element, "name");
        String url = stringAttribute(element, "url");
        return new License(name, url);
    }

    public static List<DriverPackage> driverPackages() {
        return INSTANCE.driverPackages;
    }

    public static List<String> driverPackagesIds() {
        return INSTANCE.driverPackages.stream().map(DriverPackage::getId).collect(Collectors.toList());
    }

    @NotNull
    public static DriverPackage getDriverPackage(String packageId) {
        Optional<DriverPackage> driverPackage = driverPackages().stream().filter(p -> p.getId().equals(packageId)).findFirst();
        return driverPackage.get();
    }
}