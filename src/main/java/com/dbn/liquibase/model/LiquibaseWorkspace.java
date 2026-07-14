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

import com.dbn.common.icon.Icons;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.UUIDs;
import com.dbn.connection.DatabaseType;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Objects;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

/**
 * Persisted configuration for a named Liquibase workspace.
 */
@Getter
@Setter
public class LiquibaseWorkspace implements PersistentStateElement, Presentable, Cloneable<LiquibaseWorkspace> {
    public static final String DEFAULT_ROOT_PATH = "db/liquibase";
    public static final String DEFAULT_CHANGELOG_DIRECTORY = "changes";
    public static final String DEFAULT_SQL_DIRECTORY = "sql";
    public static final String DEFAULT_MASTER_CHANGELOG = "db.changelog-master.yaml";
    public static final String DEFAULT_PROPERTIES_FILE = "liquibase.properties";

    private String id = UUIDs.regular();
    private String name;
    private DatabaseType databaseType;
    private String contentRootPath;
    private String rootPath = DEFAULT_ROOT_PATH;
    private String changelogDirectory = DEFAULT_CHANGELOG_DIRECTORY;
    private String sqlDirectory = DEFAULT_SQL_DIRECTORY;
    private String masterChangelog = DEFAULT_MASTER_CHANGELOG;
    private String propertiesFile = DEFAULT_PROPERTIES_FILE;

    public boolean usesSameContentRoot(@NotNull LiquibaseWorkspace other) {
        return Objects.equals(contentRootPath, other.contentRootPath);
    }

    @Override
    public Icon getIcon() {
        return Icons.DB_LIQUIBASE;
    }

    @Override
    public void readState(@NotNull Element element) {
        id = stringAttribute(element, "id", id);
        name = stringAttribute(element, "name", name);
        databaseType = enumAttribute(element, "database-type", DatabaseType.GENERIC);
        contentRootPath = stringAttribute(element, "content-root-path", contentRootPath);
        rootPath = stringAttribute(element, "root-path", rootPath);
        changelogDirectory = stringAttribute(element, "changelog-directory", changelogDirectory);
        sqlDirectory = stringAttribute(element, "sql-directory", sqlDirectory);
        masterChangelog = stringAttribute(element, "master-changelog", masterChangelog);
        propertiesFile = stringAttribute(element, "properties-file", propertiesFile);
    }

    @Override
    public void writeState(@NotNull Element element) {
        setStringAttribute(element, "id", id);
        setStringAttribute(element, "name", name);
        setEnumAttribute(element, "database-type", databaseType);
        setStringAttribute(element, "content-root-path", contentRootPath);
        setStringAttribute(element, "root-path", rootPath);
        setStringAttribute(element, "changelog-directory", changelogDirectory);
        setStringAttribute(element, "sql-directory", sqlDirectory);
        setStringAttribute(element, "master-changelog", masterChangelog);
        setStringAttribute(element, "properties-file", propertiesFile);
    }

    @Override
    @SneakyThrows
    public LiquibaseWorkspace clone() {
        return (LiquibaseWorkspace) super.clone();
    }
}
