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

package com.dbn.migration.liquibase.workspace;

import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Cloneable;
import com.dbn.migration.shared.workspace.DatabaseMigrationWorkspace;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

/**
 * Persisted configuration for a named Liquibase workspace.
 */
@Getter
@Setter
public class LiquibaseWorkspace extends DatabaseMigrationWorkspace implements Presentable, Cloneable<LiquibaseWorkspace> {
    public static final String DEFAULT_ROOT_PATH = "db/liquibase";
    public static final String DEFAULT_CHANGELOG_DIRECTORY = "changes";
    public static final String DEFAULT_SQL_DIRECTORY = "sql";
    public static final String DEFAULT_DOCUMENTATION_DIRECTORY = "docs";
    public static final String DEFAULT_MASTER_CHANGELOG = "db.changelog-master.yaml";
    public static final String DEFAULT_PROPERTIES_FILE = "liquibase.properties";

    private String changelogDirectory = DEFAULT_CHANGELOG_DIRECTORY;
    private String sqlDirectory = DEFAULT_SQL_DIRECTORY;
    private String documentationDirectory = DEFAULT_DOCUMENTATION_DIRECTORY;
    private String masterChangelog = DEFAULT_MASTER_CHANGELOG;
    private String propertiesFile = DEFAULT_PROPERTIES_FILE;
    private LiquibaseChangelogFormat changelogFormat = LiquibaseChangelogFormat.YAML;

    public LiquibaseWorkspace() {
        super(DEFAULT_ROOT_PATH);
    }

    @Override
    public Icon getIcon() {
        return null;// Icons.DB_LIQUIBASE;
    }

    @Override
    public void readState(@NotNull Element element) {
        super.readState(element);
        setChangelogDirectory(stringAttribute(element, "changelog-directory", getChangelogDirectory()));
        setSqlDirectory(stringAttribute(element, "sql-directory", getSqlDirectory()));
        setDocumentationDirectory(stringAttribute(element, "documentation-directory", getDocumentationDirectory()));
        setMasterChangelog(stringAttribute(element, "master-changelog", getMasterChangelog()));
        setChangelogFormat(enumAttribute(element, "changelog-format", getChangelogFormat()));
        setPropertiesFile(stringAttribute(element, "properties-file", getPropertiesFile()));
    }

    @Override
    public void writeState(@NotNull Element element) {
        super.writeState(element);
        setStringAttribute(element, "changelog-directory", getChangelogDirectory());
        setStringAttribute(element, "sql-directory", getSqlDirectory());
        setStringAttribute(element, "documentation-directory", getDocumentationDirectory());
        setEnumAttribute(element, "changelog-format", getChangelogFormat());
        setStringAttribute(element, "master-changelog", getMasterChangelog());
        setStringAttribute(element, "properties-file", getPropertiesFile());
    }

    @Override
    @SneakyThrows
    public LiquibaseWorkspace clone() {
        return (LiquibaseWorkspace) super.clone();
    }
}
