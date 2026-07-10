/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.liquibase.model;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.connection.ConnectionId;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

/**
 * Persisted Liquibase configuration associated with one database connection and content root.
 */
@Getter
@Setter
public class LiquibaseArtifact implements PersistentStateElement, Cloneable<LiquibaseArtifact> {
    public static final String DEFAULT_ROOT_PATH = "db/liquibase";
    public static final String DEFAULT_CHANGELOG_DIRECTORY = "changes";
    public static final String DEFAULT_SQL_DIRECTORY = "sql";
    public static final String DEFAULT_MASTER_CHANGELOG = "db.changelog-master.yaml";
    public static final String DEFAULT_PROPERTIES_FILE = "liquibase.properties";

    private String name;
    private ConnectionId connectionId;
    private String contentRootPath;
    private String rootPath = DEFAULT_ROOT_PATH;
    private String changelogDirectory = DEFAULT_CHANGELOG_DIRECTORY;
    private String sqlDirectory = DEFAULT_SQL_DIRECTORY;
    private String masterChangelog = DEFAULT_MASTER_CHANGELOG;
    private String propertiesFile = DEFAULT_PROPERTIES_FILE;

    public boolean usesSameContentRoot(@NotNull LiquibaseArtifact other) {
        return Objects.equals(contentRootPath, other.contentRootPath);
    }

    @Override
    public void readState(@NotNull Element element) {
        name = stringAttribute(element, "name", name);
        connectionId = connectionIdAttribute(element, "connection-id");
        contentRootPath = stringAttribute(element, "content-root-path", contentRootPath);
        rootPath = stringAttribute(element, "root-path", rootPath);
        changelogDirectory = stringAttribute(element, "changelog-directory", changelogDirectory);
        sqlDirectory = stringAttribute(element, "sql-directory", sqlDirectory);
        masterChangelog = stringAttribute(element, "master-changelog", masterChangelog);
        propertiesFile = stringAttribute(element, "properties-file", propertiesFile);
    }

    @Override
    public void writeState(@NotNull Element element) {
        setStringAttribute(element, "name", name);
        setConstantAttribute(element, "connection-id", connectionId);
        setStringAttribute(element, "content-root-path", contentRootPath);
        setStringAttribute(element, "root-path", rootPath);
        setStringAttribute(element, "changelog-directory", changelogDirectory);
        setStringAttribute(element, "sql-directory", sqlDirectory);
        setStringAttribute(element, "master-changelog", masterChangelog);
        setStringAttribute(element, "properties-file", propertiesFile);
    }

    @Override
    @SneakyThrows
    public LiquibaseArtifact clone() {
        return (LiquibaseArtifact) super.clone();
    }
}
