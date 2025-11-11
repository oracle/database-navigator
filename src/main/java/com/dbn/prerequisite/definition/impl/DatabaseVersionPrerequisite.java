/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.prerequisite.definition.impl;

import com.dbn.common.Priority;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.connection.info.ConnectionInfo;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.prerequisite.definition.PrerequisiteDefinition;
import com.dbn.prerequisite.definition.PrerequisiteDefinitionBase;
import com.dbn.prerequisite.definition.PrerequisiteDefinitionProviderBase;
import com.dbn.prerequisite.evaluation.PrerequisiteEvaluator;
import com.dbn.prerequisite.model.PrerequisiteCategory;
import com.dbn.prerequisite.model.PrerequisiteType;
import com.dbn.prerequisite.resolution.PrerequisiteAdvice;
import com.dbn.prerequisite.resolution.PrerequisiteAdvisor;
import com.dbn.prerequisite.resolution.PrerequisiteResolver;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static com.dbn.nls.NlsResources.txt;

@Getter
public abstract class DatabaseVersionPrerequisite extends PrerequisiteDefinitionProviderBase {
    private final int majorVersion;
    private final int minorVersion;

    protected DatabaseVersionPrerequisite(PrerequisiteType prerequisiteType, int majorVersion, int minorVersion) {
        super(prerequisiteType);
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public PrerequisiteType getAlternativeType() {
        return null;
    }

    @NotNull
    @Override
    public PrerequisiteDefinition createDefinition(PrerequisiteEvaluator evaluator, PrerequisiteResolver resolver, PrerequisiteAdvisor advisor) {
        return new PrerequisiteDefinitionBase(
                txt("app.prerequisite.title.VersionPrerequisite", majorVersion, minorVersion),
                txt("app.prerequisite.text.VersionPrerequisite", majorVersion, minorVersion),
                getType(),
                getAlternativeType(),
                PrerequisiteCategory.VERSION,
                evaluator,
                resolver,
                advisor);
    }

    @NotNull
    @Override
    protected PrerequisiteEvaluator createEvaluator() {
        return context -> {
            return DatabaseInterfaceInvoker.load(Priority.HIGH,
                    txt("prc.prerequisite.title.CheckingVersionPrerequisite"),
                    txt("prc.prerequisite.text.CheckingVersionPrerequisite", majorVersion, minorVersion),
                    context.getProject(),
                    context.getConnectionId(),
                    c -> evaluateDatabaseVersion(c));
        };
    }

    private @NotNull Boolean evaluateDatabaseVersion(DBNConnection c) throws SQLException {
        DatabaseMetaData metaData = c.getMetaData();
        int databaseMajorVersion = metaData.getDatabaseMajorVersion();
        int databaseMinorVersion = metaData.getDatabaseMinorVersion();

        if (databaseMajorVersion > majorVersion) return true;
        if (databaseMajorVersion == majorVersion) return databaseMinorVersion >= minorVersion;
        return false;
    }

    @Nullable
    @Override
    protected PrerequisiteResolver createResolver() {
        // users cannot simply upgrade the database, hence no "resolver"
        return null;
    }

    @NotNull
    @Override
    protected PrerequisiteAdvisor createAdvisor() {
        return context -> {
            return new PrerequisiteAdvice(
                    "Upgrade database",
                    "Earliest database version supporting this feature is " + majorVersion + "." + minorVersion + ".",
                    "--your database version is " + getDatabaseVersion(context));
        };
    }

    private static String getDatabaseVersion(DatabaseContext context) {
        ConnectionHandler connection = context.getConnection();
        if (connection == null) return "UNKNOWN";

        ConnectionInfo connectionInfo = connection.getConnectionInfo();
        if (connectionInfo == null) return "UNKNOWN";

        return connectionInfo.getProductVersionNumber();
    }

    @Override
    public String toString() {
        return majorVersion + "." + minorVersion;
    }
}
