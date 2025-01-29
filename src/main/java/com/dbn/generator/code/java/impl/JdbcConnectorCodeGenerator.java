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
import com.dbn.connection.AuthenticationTokenType;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionSettings;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.dbn.nls.NlsResources.txt;

public class JdbcConnectorCodeGenerator extends JavaCodeGenerator<JdbcConnectorCodeGeneratorInput, JdbcConnectorCodeGeneratorResult> {
    public JdbcConnectorCodeGenerator(CodeGeneratorType type) {
        super(type);
    }

    @Override
    public boolean supports(DatabaseContext context) {
        if (!super.supports(context)) return false;

        if (context instanceof ConnectionHandler) {
            ConnectionHandler connection = (ConnectionHandler) context;
            return !connection.isVirtual();
        }
        return false;
    }

    @Override
    public JdbcConnectorCodeGeneratorInput createInput(DatabaseContext databaseContext) {
        return new JdbcConnectorCodeGeneratorInput(databaseContext);
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
        addConnectionProperties(context, properties);
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

    private static void addConnectionProperties(DatabaseContext context, Properties properties) {
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
        addProperty(properties, "TNS_FOLDER", databaseInfo.getTnsFolder());
        addProperty(properties, "TNS_PROFILE", databaseInfo.getTnsProfile());

        AuthenticationInfo authenticationInfo = databaseSettings.getAuthenticationInfo();
        AuthenticationType authType = authenticationInfo.getType();
        AuthenticationTokenType authTokenType = authenticationInfo.getTokenType();
        addProperty(properties, "AUTH_TYPE", authType);
        addProperty(properties, "AUTH_TYPE_NAME", authType == null ? null  :authType.getName());
        addProperty(properties, "AUTH_TOKEN_TYPE", authTokenType);
        addProperty(properties, "AUTH_TOKEN_TYPE_NAME", authTokenType == null ? null : authTokenType.getName());

        addProperty(properties, "USER_NAME", authenticationInfo.getUser());
        // TODO add toggle in the input form, allowing the user to decide whether passwords are allowed to be propagated to the generated code
        //addProperty(properties, "PASSWORD", authenticationInfo.getPassword());
        addProperty(properties, "TOKEN_CONFIG_FILE", authenticationInfo.getTokenConfigFile());
        addProperty(properties, "TOKEN_PROFILE", authenticationInfo.getTokenProfile());

        // custom properties as csv
        Map<String, String> props = settings.getPropertiesSettings().getProperties();
        String propsCsv = props
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
        addProperty(properties, "PROPERTIES", propsCsv);
    }

    private static void addProperty(Properties properties, @NonNls String key, Object value) {
        if (value == null) return;
        properties.put(key, value.toString());
    }

    @Override
    protected String getTitle(OutcomeType outcomeType) {
        switch (outcomeType) {
            case SUCCESS: return txt("msg.shared.title.Success");
            case FAILURE: return txt("msg.shared.title.Failure");
        }
        return "";
    }

    @Override
    protected String getMessage(OutcomeType outcomeType) {
        switch (outcomeType) {
            case SUCCESS: return "Successfully created Jdbc Connector";
            case FAILURE: return "Failed to create Jdbc Connector";
        }
        return "";
    }

    @Override
    public AnAction createAction(DatabaseContext context) {
        return super.createAction(context);
    }
}
