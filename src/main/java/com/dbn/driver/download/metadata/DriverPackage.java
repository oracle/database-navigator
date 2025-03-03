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

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.*;

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
public class DriverPackage implements PersistentStateElement {
    private final String id;
    private String name;
    @Setter
    private String path;
    private DatabaseType databaseType;
    private List<Library> libraries = new ArrayList<>();
    @Setter
    private boolean old;

    public DriverPackage(String id, String name, DatabaseType databaseType, List<Library> libraries) {
        this.id = id;
        this.name = name;
        this.databaseType = databaseType;
        this.libraries = libraries;
    }

    public DriverPackage(String id){
        this.id =id;
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
        this.path = stringAttribute(element, "path");
        this.databaseType = enumAttribute(element, "database-type", DatabaseType.class);
        for (Element libElement : element.getChildren("library")) {
            Library library = new Library(
                    stringAttribute(libElement, "group-id"),
                    stringAttribute(libElement, "artifact-id"),
                    stringAttribute(libElement, "version")
            );
            library.readState(libElement);
            libraries.add(library);
        }
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "id", id);
        setStringAttribute(element, "name", name);
        setStringAttribute(element, "path", path);
        setEnumAttribute(element, "database-type", databaseType);
        for (Library library : libraries) {
            Element libElement = newElement(element, "library");
            library.writeState(libElement);
        }
    }
}