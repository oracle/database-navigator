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

package com.dbn.migration.flyway.workspace;

import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Cloneable;
import com.dbn.migration.shared.workspace.DatabaseMigrationWorkspace;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

/**
 * Persisted configuration for a named Flyway workspace.
 *
 * <p>The root path represents the Flyway working directory relative to the selected
 * IntelliJ content root. The remaining paths describe the conventional project files;
 * Flyway configuration may override the migration locations at runtime.</p>
 */
@Getter
@Setter
public class FlywayWorkspace extends DatabaseMigrationWorkspace implements Presentable, Cloneable<FlywayWorkspace> {
    public static final String DEFAULT_ROOT_PATH = ".";
    public static final String DEFAULT_MIGRATIONS_DIRECTORY = "migrations";
    public static final String DEFAULT_CONFIGURATION_FILE = "flyway.toml";
    public static final String DEFAULT_USER_CONFIGURATION_FILE = "flyway.user.toml";

    private String migrationsDirectory = DEFAULT_MIGRATIONS_DIRECTORY;
    private String configurationFile = DEFAULT_CONFIGURATION_FILE;
    private String userConfigurationFile = DEFAULT_USER_CONFIGURATION_FILE;

    public FlywayWorkspace() {
        super(DEFAULT_ROOT_PATH);
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public void readState(@NotNull Element element) {
        super.readState(element);
        setMigrationsDirectory(stringAttribute(element, "migrations-directory", getMigrationsDirectory()));
        setConfigurationFile(stringAttribute(element, "configuration-file", getConfigurationFile()));
        setUserConfigurationFile(stringAttribute(element, "user-configuration-file", getUserConfigurationFile()));
    }

    @Override
    public void writeState(@NotNull Element element) {
        super.writeState(element);
        setStringAttribute(element, "migrations-directory", getMigrationsDirectory());
        setStringAttribute(element, "configuration-file", getConfigurationFile());
        setStringAttribute(element, "user-configuration-file", getUserConfigurationFile());
    }

    @Override
    @SneakyThrows
    public FlywayWorkspace clone() {
        return (FlywayWorkspace) super.clone();
    }
}
