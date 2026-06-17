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

package com.dbn.generator.code.java.impl;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.common.util.Chars;
import com.dbn.connection.AuthenticationTokenType;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.EasyConnectParameters;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.generator.code.CodeGeneratorType;
import com.dbn.generator.code.java.JavaCodeGenerator;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.dbn.common.util.Messages.options;
import static com.dbn.common.util.Messages.showAcknowledgementDialog;
import static com.dbn.common.util.Parameters.toParameterString;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.nls.NlsResources.txt;

public class JdbcConnectorCodeGenerator extends JavaCodeGenerator<JdbcConnectorCodeGeneratorInput, JdbcConnectorCodeGeneratorResult> {
    private static final @NonNls String PLACEHOLDER_USER_NAME = "<user-name>";
    private static final @NonNls String PLACEHOLDER_TOKEN_CONFIG_FILE = "<oci-token-config-file>";
    private static final @NonNls String PLACEHOLDER_TOKEN_PROFILE = "<oci-token-profile>";
    private static final @NonNls String PLACEHOLDER_OCI_COMPARTMENT = "<oci-compartment-ocid>";
    private static final @NonNls String PLACEHOLDER_OCI_DATABASE = "<oci-database-ocid>";

    public JdbcConnectorCodeGenerator(CodeGeneratorType type) {
        super(type);
    }

    @Override
    public boolean supports(DatabaseContext context) {
        if (!super.supports(context)) return false;

        if (context instanceof ConnectionHandler connection) {
            return !connection.isVirtual();
        }
        return false;
    }

    @Override
    public JdbcConnectorCodeGeneratorInput createInput(DatabaseContext databaseContext) {
        return new JdbcConnectorCodeGeneratorInput(databaseContext);
    }

    @Override
    public boolean prepareDestination(JdbcConnectorCodeGeneratorInput input) {
        boolean destinationPrepared = super.prepareDestination(input);
        if (!destinationPrepared) return false;

        Boolean embedSensitiveValues = promptSensitiveValueHandling(input);
        if (embedSensitiveValues == null) return false;

        input.setEmbedSensitiveValues(embedSensitiveValues);
        return true;
    }

    @Nullable
    @Override
    public JdbcConnectorCodeGeneratorResult generateCode(JdbcConnectorCodeGeneratorInput input) throws Exception {
        // prepare template and properties
        FileTemplate template = initTemplate(input);
        Properties properties = initProperties(input);

        // generate and format class
        VirtualFile javaFile = generateClass(input, template, properties);
        return new JdbcConnectorCodeGeneratorResult(input, javaFile);
    }

    @NotNull
    private FileTemplate initTemplate(JdbcConnectorCodeGeneratorInput input) {
        String templateName = getType().getTemplate();

        Project project = input.getProject();
        FileTemplateManager templateManager = FileTemplateManager.getInstance(project);
        return templateManager.getTemplate(templateName);
    }

    @NotNull
    private Properties initProperties(JdbcConnectorCodeGeneratorInput input) throws ConfigurationException {
        DatabaseContext context = input.getDatabaseContext();

        Properties properties = new Properties();
        addInputProperties(input, properties);
        addConnectionProperties(input, context, properties);
        return properties;
    }

    private static VirtualFile generateClass(JdbcConnectorCodeGeneratorInput input, FileTemplate template, Properties properties) throws Exception {
        String className = input.getClassName();
        PsiDirectory directory = input.getTargetDirectory();

        PsiElement javaClass = FileTemplateUtil.createFromTemplate(template, className, properties, directory);
        reformatClass(javaClass);

        return javaClass.getContainingFile().getVirtualFile();
    }

    private static void addInputProperties(JdbcConnectorCodeGeneratorInput input, Properties properties) throws ConfigurationException {
        addProperty(properties, "CLASS_NAME", input.getClassName());
        addProperty(properties, "PACKAGE_NAME", input.getPackageName());
    }

    private static void addConnectionProperties(JdbcConnectorCodeGeneratorInput input, DatabaseContext context, Properties properties) {
        ConnectionHandler connection = context.ensureConnection();
        ConnectionSettings settings = connection.getSettings();

        ConnectionDatabaseSettings databaseSettings = settings.getDatabaseSettings();
        addProperty(properties, "DATABASE_TYPE", databaseSettings.getDatabaseType());
        addProperty(properties, "JDBC_URL", databaseSettings.getConnectionUrl());
        addProperty(properties, "JDBC_DRIVER", databaseSettings.getDriver());
        addProperty(properties, "JDBC_URL_PATTERN", databaseSettings.getUrlPattern().getUrlTemplate());

        DatabaseInfo databaseInfo = databaseSettings.getDatabaseInfo();
        addProperty(properties, "JDBC_URL_TYPE", databaseInfo.getUrlType());
        addProperty(properties, "JDBC_URL_TYPE_NAME", databaseInfo.getUrlType().getName());
        addProperty(properties, "HOST", databaseInfo.getHost());
        addProperty(properties, "PORT", databaseInfo.getPort());
        addProperty(properties, "DATABASE", databaseInfo.getDatabase());
        addProperty(properties, "PROTOCOL", databaseInfo.getProtocol());
        addProperty(properties, "SERVER_TYPE", databaseInfo.getServerType());
        // ensure the quoted parameters are jave-escaped  because
        // they will be added to Java code.
        addProperty(properties, "PARAMETERS",
                toParameterString(EasyConnectParameters.ensureParametersIfEasyConnect(
                        databaseInfo.getParameters(), databaseInfo, true)));
        addProperty(properties, "TNS_FOLDER", databaseInfo.getTnsFolder());
        addProperty(properties, "TNS_PROFILE", databaseInfo.getTnsProfile());

        AuthenticationInfo authenticationInfo = databaseSettings.getAuthenticationInfo();
        AuthenticationType authType = authenticationInfo.getType();
        AuthenticationTokenType authTokenType = authenticationInfo.getTokenType();
        addProperty(properties, "AUTH_TYPE", authType);
        addProperty(properties, "AUTH_TYPE_NAME", authType == null ? null : authType.getName());
        addProperty(properties, "AUTH_TOKEN_TYPE", authTokenType);
        addProperty(properties, "AUTH_TOKEN_TYPE_NAME", authTokenType == null ? null : authTokenType.getName());

        addSensitiveProperty(properties, "USER_NAME", authenticationInfo.getUser(), PLACEHOLDER_USER_NAME, input.isEmbedSensitiveValues());
        // TODO add toggle in the input form, allowing the user to decide whether passwords are allowed to be propagated to the generated code
        //addProperty(properties, "PASSWORD", authenticationInfo.getPassword());
        addSensitiveProperty(properties, "TOKEN_CONFIG_FILE", authenticationInfo.getTokenConfigFile(), PLACEHOLDER_TOKEN_CONFIG_FILE, input.isEmbedSensitiveValues());
        addSensitiveProperty(properties, "TOKEN_PROFILE", authenticationInfo.getTokenProfile(), PLACEHOLDER_TOKEN_PROFILE, input.isEmbedSensitiveValues());
        addSensitiveProperty(properties, "OCI_COMPARTMENT", authenticationInfo.getCompartmentOcid(), PLACEHOLDER_OCI_COMPARTMENT, input.isEmbedSensitiveValues());
        addSensitiveProperty(properties, "OCI_DATABASE", authenticationInfo.getDatabaseOcid(), PLACEHOLDER_OCI_DATABASE, input.isEmbedSensitiveValues());

        // add AZURE token properties
        addProperty(properties, "AZURE_TOKEN_CLIENT_ID", authenticationInfo.getAzureClientId());
        addProperty(properties, "AZURE_TOKEN_TENANT_ID", authenticationInfo.getAzureTenantId());
        addProperty(properties, "AZURE_TOKEN_DATABASE_ID_URI", authenticationInfo.getAzureDatabaseApplicationIdUri());
        addProperty(properties, "AZURE_TOKEN_CLIENT_SECRET_FILE", authenticationInfo.getAzureClientCertificateFile());
        char[] azureClientSecretFilePassword = authenticationInfo.getAzureClientCertificatePassword();
        if (Chars.isNotEmpty(azureClientSecretFilePassword)) {
            addProperty(properties, "AZURE_TOKEN_CLIENT_SECRET_FILE_PASSWORD", azureClientSecretFilePassword);
        }
        addProperty(properties, "AZURE_TOKEN_CLIENT_SECRET_TOKEN", authenticationInfo.getAzureClientSecret());

        // custom properties as csv
        Map<String, String> props = settings.getPropertiesSettings().getProperties();
        String propsCsv = props
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
        addProperty(properties, "PROPERTIES", propsCsv);
    }

    @Nullable
    private static Boolean promptSensitiveValueHandling(JdbcConnectorCodeGeneratorInput input) {
        AuthenticationInfo authenticationInfo = getAuthenticationInfo(input);
        List<String> sensitiveValueNames = getSensitiveValueNames(authenticationInfo);
        if (sensitiveValueNames.isEmpty()) return false;

        Project project = input.getProject();
        String valueList = sensitiveValueNames.stream().map(value -> " - " + value).collect(Collectors.joining("\n"));

        int option = showAcknowledgementDialog(project,
                txt("msg.codeGenerator.title.JdbcConnectorSensitiveValues"),
                txt("msg.codeGenerator.question.JdbcConnectorSensitiveValues", valueList),
                options(
                        txt("msg.codeGenerator.button.UsePlaceholders"),
                        txt("msg.codeGenerator.button.EmbedActualValues"),
                        txt("msg.shared.button.Cancel")), 0, null);

        return switch (option) {
            case 0 -> false;
            case 1 -> true;
            default -> null;
        };
    }

    private static AuthenticationInfo getAuthenticationInfo(JdbcConnectorCodeGeneratorInput input) {
        ConnectionHandler connection = input.getDatabaseContext().ensureConnection();
        ConnectionSettings settings = connection.getSettings();
        ConnectionDatabaseSettings databaseSettings = settings.getDatabaseSettings();
        return databaseSettings.getAuthenticationInfo();
    }

    private static List<String> getSensitiveValueNames(AuthenticationInfo authenticationInfo) {
        List<String> valueNames = new ArrayList<>();
        addSensitiveValueName(valueNames, authenticationInfo.getUser(), "USER_NAME");
        addSensitiveValueName(valueNames, authenticationInfo.getTokenConfigFile(), "TOKEN_CONFIG_FILE");
        addSensitiveValueName(valueNames, authenticationInfo.getTokenProfile(), "TOKEN_PROFILE");
        addSensitiveValueName(valueNames, authenticationInfo.getCompartmentOcid(), "OCI_COMPARTMENT");
        addSensitiveValueName(valueNames, authenticationInfo.getDatabaseOcid(), "OCI_DATABASE");
        return valueNames;
    }

    private static void addSensitiveValueName(List<String> valueNames, @Nullable String value, @NonNls String name) {
        if (isNotEmpty(value)) valueNames.add(name);
    }

    private static void addSensitiveProperty(Properties properties, @NonNls String key, @Nullable String value, @NonNls String placeholder, boolean embedValue) {
        if (isEmpty(value)) return;
        addProperty(properties, key, embedValue ? value : placeholder);
    }

    private static void addProperty(Properties properties, @NonNls String key, Object value) {
        if (value == null) return;
        properties.put(key, value.toString());
    }

    @Override
    protected String getTitle(OutcomeType outcomeType) {
        return switch (outcomeType) {
            case SUCCESS -> txt("msg.shared.title.Success");
            case FAILURE -> txt("msg.shared.title.Failure");
            default -> "";
        };
    }

    @Override
    protected String getMessage(OutcomeType outcomeType) {
        return switch (outcomeType) {
            case SUCCESS -> "Successfully created Jdbc Connector";
            case FAILURE -> "Failed to create Jdbc Connector";
            default -> "";
        };
    }

    @Override
    public AnAction createAction(DatabaseContext context) {
        return super.createAction(context);
    }
}
