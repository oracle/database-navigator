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

package com.dbn.driver.download.metadata;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.connection.DatabaseType;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

/**
 * DriverPackage represents a set of Maven libraries required for a specific database driver.
 * Each driver package includes an id, a name, a database type, and a list of libraries.
 * <p>
 * Example:
 * <pre>
 * {@code
 * <driver-package id="oracle-23.3-standard" name="Oracle 23.3" database-type="ORACLE">
 *     <library group-id="javax.resource" artifact-id="connector-api" version="1.5"/>
 *     <library group-id="oracle.jdbc" artifact-id="ojdbc8" version="23.3.0.23.09"/>
 * </driver-package>
 * }
 * </pre>
 *
 * @author Ayoub Aarrasse
 */
@Getter
@Setter
public class DriverPackage implements PersistentStateElement, Comparable<DriverPackage> {
    private String id;
    private String name;
    private DatabaseType databaseType;
    private List<Library> libraries = new ArrayList<>();
    private boolean obsolete;
    private boolean latest;

    public DriverPackage(String id, String name, DatabaseType databaseType, List<Library> libraries) {
        this.id = id;
        this.name = name;
        this.databaseType = databaseType;
        this.libraries = libraries;
    }

    public DriverPackage(String id) {
        this.id = id;
    }

    public boolean matches(DatabaseType databaseType) {
        return this.databaseType == databaseType || databaseType == DatabaseType.GENERIC;
    }

    @Override
    public String toString() {
        return name;
    }

    public int size() {
        return libraries.size();
    }

    @Override
    public void readState(Element element) {
        this.name = stringAttribute(element, "name");
        this.databaseType = enumAttribute(element, "database-type", DatabaseType.class);
        this.latest = booleanAttribute(element, "latest", false);
        for (Element libraryElement : childrenOf(element, "library")) {
            Library library = new Library();
            library.readState(libraryElement);
            libraries.add(library);
        }
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "id", id);
        setStringAttribute(element, "name", name);
        setEnumAttribute(element, "database-type", databaseType);
        if (latest) setBooleanAttribute(element, "latest", true);
        for (Library library : libraries) {
            Element libraryElement = newElement(element, "library");
            library.writeState(libraryElement);
        }
    }

    @Override
    public int compareTo(@NotNull DriverPackage o) {
        return this.name.compareTo(o.name);
    }
}
