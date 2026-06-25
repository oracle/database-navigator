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

package com.dbn.database;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseType;
import com.dbn.database.mysql.MySqlExecutionInterface;
import com.dbn.database.postgres.PostgresExecutionInterface;
import com.dbn.execution.script.CmdLineInterface;
import com.dbn.execution.script.ScriptExecutionInput;
import com.dbn.execution.script.ScriptPasswordDelivery;
import com.dbn.options.ProjectSettingsManager;
import com.intellij.mock.MockProject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static com.dbn.connection.DatabaseUrlType.DATABASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ScriptCredentialFileExecutionInputTest {
    private static final char[] MYSQL_PASSWORD = {'s', 'e', 'c', '"', 'r', '\\', 'e', 't'};
    private static final char[] POSTGRES_PASSWORD = {'s', ':', 'e', '\\', 'c'};

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void resetPasswordDeliveryProperty() {
        clearPasswordDeliveryProperty();
    }

    @Test
    public void mysqlUsesTemporaryCredentialFileByDefault() throws Exception {
        clearPasswordDeliveryProperty();
        DatabaseClientCommand input = new MySqlExecutionInterface().createScriptExecutionCommand(
                scriptExecutionInput(DatabaseType.MYSQL, MYSQL_PASSWORD, "mysql"),
                temporaryFolder.newFile("mysql-script.sql"),
                "select 1", null);

        assertPasswordNotExposed(input, "MYSQL_PWD", MYSQL_PASSWORD);

        File credentialFile = onlyFile(".cnf");
        assertEquals("[client]\npassword=\"sec\\\"r\\\\et\"\n", Files.readString(credentialFile.toPath()));

        List<String> parameters = input.getCommandLine().getParametersList().getParameters();
        assertTrue(parameters.get(0).startsWith("--defaults-extra-file="));
        assertTrue(parameters.get(0).endsWith(credentialFile.getPath()));
    }

    @Test
    public void postgresUsesTemporaryCredentialFileByDefault() throws Exception {
        clearPasswordDeliveryProperty();
        DatabaseClientCommand input = new PostgresExecutionInterface().createScriptExecutionCommand(
                scriptExecutionInput(DatabaseType.POSTGRES, POSTGRES_PASSWORD, "psql"),
                temporaryFolder.newFile("postgres-script.sql"),
                "select 1", null);

        Map<String, String> environment = input.getCommandLine().getEnvironment();
        assertPasswordNotExposed(input, "PGPASSWORD", POSTGRES_PASSWORD);

        String passwordFilePath = environment.get("PGPASSFILE");
        assertNotNull(passwordFilePath);
        assertFalse(passwordFilePath.contains("s:e\\c"));

        File credentialFile = new File(passwordFilePath);
        assertEquals("localhost:5432:postgres:scott:s\\:e\\\\c\n", Files.readString(credentialFile.toPath()));
    }

    @Test
    public void legacyEnvironmentModePreservesPasswordEnvironmentVariables() throws Exception {
        setPasswordDeliveryProperty(ScriptPasswordDelivery.ENVIRONMENT_VARIABLE);

        DatabaseClientCommand mysqlInput = new MySqlExecutionInterface().createScriptExecutionCommand(
                scriptExecutionInput(DatabaseType.MYSQL, MYSQL_PASSWORD, "mysql"),
                temporaryFolder.newFile("mysql-script.sql"),
                "select 1", null);

        DatabaseClientCommand postgresInput = new PostgresExecutionInterface().createScriptExecutionCommand(
                scriptExecutionInput(DatabaseType.POSTGRES, POSTGRES_PASSWORD, "psql"),
                temporaryFolder.newFile("postgres-script.sql"),
                "select 1", null);

        assertEquals("sec\"r\\et", mysqlInput.getCommandLine().getEnvironment().get("MYSQL_PWD"));
        assertEquals("s:e\\c", postgresInput.getCommandLine().getEnvironment().get("PGPASSWORD"));
        assertNull(postgresInput.getCommandLine().getEnvironment().get("PGPASSFILE"));
    }

    @Test
    public void invalidCredentialDeliveryPropertyFallsBackToTempFile() {
        assertEquals(ScriptPasswordDelivery.CREDENTIAL_FILE, ScriptPasswordDelivery.resolve(null));
        assertEquals(ScriptPasswordDelivery.CREDENTIAL_FILE, ScriptPasswordDelivery.resolve(""));
        assertEquals(ScriptPasswordDelivery.CREDENTIAL_FILE, ScriptPasswordDelivery.resolve("unknown"));
        assertEquals(ScriptPasswordDelivery.CREDENTIAL_FILE, ScriptPasswordDelivery.resolve("environment"));
        assertEquals(ScriptPasswordDelivery.ENVIRONMENT_VARIABLE, ScriptPasswordDelivery.resolve("ENVIRONMENT_VARIABLE"));
    }

    private static void setPasswordDeliveryProperty(ScriptPasswordDelivery delivery) {
        System.setProperty(ScriptPasswordDelivery.PROPERTY_NAME, delivery.name());
    }

    private static void clearPasswordDeliveryProperty() {
        System.clearProperty(ScriptPasswordDelivery.PROPERTY_NAME);
    }

    private static void assertPasswordNotExposed(DatabaseClientCommand input, String environmentVariable, char[] password) {
        String secret = new String(password);
        Map<String, String> environment = input.getCommandLine().getEnvironment();
        assertFalse(environment.containsKey(environmentVariable));
        assertFalse(environment.containsValue(secret));
        assertFalse(input.getPresentableCommand().contains(secret));
    }

    private File onlyFile(String suffix) {
        File[] files = temporaryFolder.getRoot().listFiles((dir, name) -> name.endsWith(suffix));
        assertNotNull(files);
        assertEquals(1, files.length);
        return files[0];
    }

    private static ScriptExecutionInput scriptExecutionInput(DatabaseType databaseType, char[] password, String executablePath) {
        MockProject project = mockProject();
        ScriptExecutionInput input = new ScriptExecutionInput(
                project,
                null,
                connection(databaseType, password),
                null,
                false);
        input.setCmdLineInterface(new CmdLineInterface(databaseType, executablePath, executablePath, null));
        return input;
    }

    private static MockProject mockProject() {
        try {
            MockProject project = new MockProject(null, Disposer.newDisposable());
            Constructor<ProjectSettingsManager> constructor = ProjectSettingsManager.class.getDeclaredConstructor(Project.class);
            constructor.setAccessible(true);
            project.registerService(ProjectSettingsManager.class, constructor.newInstance(project));
            return project;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to create mock project", e);
        }
    }

    private static ConnectionHandler connection(DatabaseType databaseType, char[] password) {
        ConnectionId connectionId = ConnectionId.create();
        DatabaseInfo databaseInfo = new DatabaseInfo(
                databaseType == DatabaseType.MYSQL ? "mysql" : "postgresql",
                "localhost",
                databaseType == DatabaseType.MYSQL ? "3306" : "5432",
                databaseType == DatabaseType.MYSQL ? "mysql" : "postgres",
                DATABASE);

        AuthenticationInfo authenticationInfo = new AuthenticationInfo(null, false);
        authenticationInfo.setUser("scott");
        authenticationInfo.setPassword(password);

        return (ConnectionHandler) Proxy.newProxyInstance(
                ConnectionHandler.class.getClassLoader(),
                new Class[]{ConnectionHandler.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getConnectionId" -> connectionId;
                    case "getDatabaseInfo" -> databaseInfo;
                    case "getAuthenticationInfo" -> authenticationInfo;
                    case "getDatabaseType" -> databaseType;
                    case "toString" -> "test connection";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == double.class) return 0.0d;
        if (type == float.class) return 0.0f;
        if (type == long.class) return 0L;
        if (type == int.class) return 0;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        return null;
    }
}
