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

package com.dbn.execution.script;

import com.dbn.common.approval.UserApprovable;
import com.dbn.common.approval.UserApprovalAction;
import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.connection.DatabaseType;
import com.intellij.openapi.util.SystemInfo;
import lombok.Data;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Set;
import java.util.UUID;

import static com.dbn.common.approval.UserApprovalAction.COMMAND_LINE_EXECUTION;
import static com.dbn.common.approval.UserApprovalAction.PASSWORD_ENVIRONMENT_VARIABLE;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.nls.NlsResources.txt;

@Data
public class CmdLineInterface implements Cloneable<CmdLineInterface>, PersistentConfiguration, Presentable, UserApprovable {
    public static final String DEFAULT_ID = "DEFAULT";

    private DatabaseType databaseType;
    private String executablePath;
    private String id;
    private String name;
    private String description;

    private interface Defaults {
        String extension = SystemInfo.isWindows ? ".exe" : "";
        CmdLineInterface ORACLE = new CmdLineInterface(DEFAULT_ID, DatabaseType.ORACLE, "sqlplus", txt("app.execution.const.CmdLineInterface_ORACLE"), "sqlplus" + extension);
        CmdLineInterface MYSQL = new CmdLineInterface(DEFAULT_ID, DatabaseType.MYSQL, "mysql", txt("app.execution.const.CmdLineInterface_MYSQL"), "mysql" + extension);
        CmdLineInterface POSTGRES = new CmdLineInterface(DEFAULT_ID, DatabaseType.POSTGRES, "psql ", txt("app.execution.const.CmdLineInterface_POSTGRES"), "psql" + extension);
        CmdLineInterface SQLITE = new CmdLineInterface(DEFAULT_ID, DatabaseType.SQLITE, "sqlite3 ", txt("app.execution.const.CmdLineInterface_SQLITE"), "sqlite3" + extension);
        CmdLineInterface GENERIC = new CmdLineInterface(DEFAULT_ID, DatabaseType.GENERIC, "sql ", txt("app.execution.const.CmdLineInterface_GENERIC"), "sql" + extension);
    }

    public static CmdLineInterface getDefault(@Nullable DatabaseType databaseType) {
        if (databaseType == null) return Defaults.GENERIC;

        return switch (databaseType) {
            case ORACLE -> Defaults.ORACLE;
            case MYSQL -> Defaults.MYSQL;
            case POSTGRES -> Defaults.POSTGRES;
            case SQLITE -> Defaults.SQLITE;
            default -> Defaults.GENERIC;
        };
    }

    public static DatabaseType resolveDatabaseType(String executableName) {
        for (DatabaseType databaseType : DatabaseType.nativelySupported()) {
            CmdLineInterface cmdLineInterface = getDefault(databaseType);
            String executablePath = cmdLineInterface.getExecutablePath();
            if (Strings.containsIgnoreCase(executableName, executablePath)) {
                return databaseType;
            }
        }
        return DatabaseType.GENERIC;
    }

    public CmdLineInterface() {

    }

    public CmdLineInterface(DatabaseType databaseType, @NonNls String executablePath, String name, @NonNls String description) {
        this(UUID.randomUUID().toString(), databaseType, executablePath, name, description);
    }

    public CmdLineInterface(String id, DatabaseType databaseType, @NonNls String executablePath, String name, @NonNls String description) {
        this.id = id;
        this.name = name;
        this.databaseType = databaseType;
        this.executablePath = executablePath;
        this.description = description;
    }

    public boolean isDefault() {
        return DEFAULT_ID.equals(id);
    }

    @Override
    public Set<UserApprovalAction> getApprovalActions() {
        return Set.of(COMMAND_LINE_EXECUTION, PASSWORD_ENVIRONMENT_VARIABLE);
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return databaseType.getIcon();
    }

    @NotNull
    @Override
    public String getName() {
        return Commons.nvl(name, "");
    }

    @Nullable
    @Override
    public String getDescription() {
        return Commons.nvl(description, executablePath);
    }

    @Override
    public void readConfiguration(Element element) {
        id = stringAttribute(element, "id");
        if (Strings.isEmpty(id)) id = UUID.randomUUID().toString();
        name = stringAttribute(element, "name");
        executablePath = stringAttribute(element, "executable-path");
        databaseType = enumAttribute(element, "database-type", DatabaseType.class);
    }

    @Override
    public void writeConfiguration(Element element) {
        element.setAttribute("id", id);
        element.setAttribute("name", name);
        element.setAttribute("executable-path", executablePath);
        element.setAttribute("database-type", databaseType.id());
    }

    @Override
    @SneakyThrows
    public CmdLineInterface clone() {
        return cast(super.clone());
    }
}
