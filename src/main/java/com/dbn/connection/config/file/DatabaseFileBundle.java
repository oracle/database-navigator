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

package com.dbn.connection.config.file;

import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Naming;
import com.dbn.common.util.Strings;
import com.intellij.openapi.options.ConfigurationException;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Lists.filter;

@Getter
public class DatabaseFileBundle implements PersistentConfiguration, Cloneable<DatabaseFileBundle> {
    private final List<DatabaseFile> files = new ArrayList<>();

    public DatabaseFileBundle() {
    }

    public DatabaseFileBundle(String file) {
        files.add(new DatabaseFile(file, "main"));
    }

    public int size() {
        return files.size();
    }

    public DatabaseFile get(int rowIndex) {
        return files.get(rowIndex);
    }

    public void add(DatabaseFile databaseFile) {
        adjustSchemaName(databaseFile);
        files.add(databaseFile);
    }

    public void add(int rowIndex, DatabaseFile databaseFile) {
        adjustSchemaName(databaseFile);
        files.add(rowIndex, databaseFile);
    }

    public void remove(int rowIndex) {
        files.remove(rowIndex);
    }

    @Nullable
    public DatabaseFile getFile(String schema) {
        return files.stream().filter(f -> Objects.equals(f.getSchema(), schema)).findFirst().orElse(null);
    }

    public DatabaseFile getMainFile() {
        return getFile("main");
    }

    public List<DatabaseFile> getAttachedFiles() {
        return filter(files, f -> !f.isMain());
    }

    public String getMainFilePath() {
        DatabaseFile mainFile = getMainFile();
        return mainFile == null ? "" : mainFile.getPath();
    }

    public String getFirstFilePath() {
        return isEmpty() ? "" : files.get(0).getPath();
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    public boolean isValid() {
        if (isEmpty()) return false;
        return files.stream().allMatch(f -> f.isValid());
    }

    public void adjustSchemaName(DatabaseFile databaseFile) {
        String path = databaseFile.getPath();
        String schema = databaseFile.getSchema();
        if (Strings.isEmpty(schema) && Strings.isNotEmpty(path)) {
            schema = databaseFile.getFileName();
            Set<String> taken = files.stream().map(f -> f.getSchema()).collect(Collectors.toSet());
            schema = Naming.nextNumberedIdentifier(schema, false, () -> taken);
            databaseFile.setSchema(schema);
        }
    }

    public void validate() throws ConfigurationException {
        Set<String> set = new HashSet<>();
        if (!files.stream().map(file -> file.getPath()).allMatch(e -> set.add(e))) {
            throw new ConfigurationException("Invalid Database files configuration. Duplicate database files.");
        }
        set.clear();
        if (!files.stream().map(file -> file.getSchema()).allMatch(e -> set.add(e))) {
            throw new ConfigurationException("Invalid Database files configuration. Duplicate database identifiers");
        }
    }

    @Override
    public void readConfiguration(Element element) {
        if (element == null) return;
        for (Element child : element.getChildren()) {
            String path = stringAttribute(child, "path");
            String schema = stringAttribute(child, "schema");
            DatabaseFile databaseFile = new DatabaseFile(path, schema);
            if (Strings.isEmpty(schema)) {
                adjustSchemaName(databaseFile);
            }
            add(databaseFile);
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        for (DatabaseFile file : files) {
            String path = file.getPath();
            String schema = file.getSchema();
            if (Strings.isNotEmpty(path) || Strings.isNotEmpty(schema)) {
                Element child = newElement(element, "file");
                setStringAttribute(child, "path", path);
                setStringAttribute(child, "schema", schema);
            }
        }
    }

    @Override
    public DatabaseFileBundle clone() {
        DatabaseFileBundle fileBundle = new DatabaseFileBundle();
        for (DatabaseFile file : files) {
            fileBundle.files.add(file.clone());
        }
        return fileBundle;
    }
}
