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

package com.dbn.mcp.deploy;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionRef;
import com.dbn.mcp.build.McpBuilderResult;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpTransportType;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.nls.NlsResources.txt;

/**
 * Publishes a built MCP container image to OCIR and creates a Graal application from it.
 * Collects input only; the Docker and database work runs in {@link McpGraalDeployTask}.
 */
public class McpGraalDeployDialog extends DBNDialog<McpGraalDeployForm> {
    private static final String GRAAL_DEPLOYMENT_PORT = "8080";

    private final ConnectionRef connection;
    private final McpServerDefinition definition;
    private final McpBuilderResult result;

    private Action buildAndPushAction;
    private Action createApplicationAction;

    public McpGraalDeployDialog(
            @Nullable Project project,
            @NotNull ConnectionRef connection,
            @NotNull McpServerDefinition definition,
            @NotNull McpBuilderResult result) {
        super(project, txt("msg.mcp.title.GraalDeployment"), true);
        this.connection = connection;
        this.definition = definition;
        this.result = result;
        setDefaultSize(620, 460);

        init();
        getForm().setInputChangeHandler(() -> updateActionAvailability());
        updateActionAvailability();
    }

    @NotNull
    @Override
    protected McpGraalDeployForm createForm() {
        return new McpGraalDeployForm(this, definition, result);
    }

    protected final Action[] initializeActions() {
        renameAction(getCancelAction(), txt("msg.shared.button.Close"));

        buildAndPushAction = createAction(txt("msg.mcp.button.BuildAndPushImage"), () -> buildAndPushImage());
        createApplicationAction = createAction(txt("msg.mcp.button.CreateGraalApplication"), () -> createApplication());

        return actions(buildAndPushAction, createApplicationAction, getCancelAction());
    }

    /**
     * The application can only be created once the user has pushed the image and pasted back the
     * OCID that OCI assigns to it - the image name alone is not accepted by Graal.
     */
    private void updateActionAvailability() {
        McpGraalDeploymentInput input = getForm().getDeploymentInput();
        createApplicationAction.setEnabled(
                McpGraalDeploymentInput.isValidContainerImageOcid(input.getContainerImageOcid()));
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        McpGraalDeploymentInput input = getForm().getDeploymentInput();

        if (isEmptyOrSpaces(input.getApplicationName())) {
            return new ValidationInfo(txt("msg.mcp.error.GraalApplicationNameRequired"));
        }
        if (!McpGraalDeploymentInput.isValidRegionKey(input.getRegionKey())) {
            return new ValidationInfo(txt("msg.mcp.error.OcirRegionInvalid"));
        }
        if (isEmptyOrSpaces(input.getNamespace())) {
            return new ValidationInfo(txt("msg.mcp.error.OcirNamespaceRequired"));
        }
        if (isEmptyOrSpaces(input.getRepository())) {
            return new ValidationInfo(txt("msg.mcp.error.OcirRepositoryRequired"));
        }
        if (isEmptyOrSpaces(input.getTag())) {
            return new ValidationInfo(txt("msg.mcp.error.ImageTagRequired"));
        }
        return super.doValidate();
    }

    /**
     * Deployment targets the documented Graal environment, which accepts only a linux/amd64
     * HTTP server image listening on {@value #GRAAL_DEPLOYMENT_PORT}.
     */
    @Nullable
    private String validateDeployability() {
        if (!definition.getImplementation().isContainer()) {
            return txt("msg.mcp.error.GraalDeploymentRequiresContainer");
        }
        if (definition.getTransportType() != McpTransportType.HTTP) {
            return txt("msg.mcp.error.GraalDeploymentRequiresHttp");
        }
        if (!GRAAL_DEPLOYMENT_PORT.equals(definition.getHttpPort())) {
            return txt("msg.mcp.error.GraalDeploymentRequiresPort", GRAAL_DEPLOYMENT_PORT);
        }

        Path sourceDirectory = result.getSourceDirectory();
        if (sourceDirectory == null || !Files.isDirectory(sourceDirectory)) {
            return txt("msg.mcp.error.GraalSourceProjectMissing");
        }
        if (!Files.isRegularFile(sourceDirectory.resolve("Dockerfile.graal"))
                || !Files.isRegularFile(sourceDirectory.resolve("pom.xml"))) {
            return txt("msg.mcp.error.GraalSourceProjectIncomplete");
        }
        return null;
    }

    private void buildAndPushImage() {
        deployTask().buildAndPushImage(getForm().getDeploymentInput());
    }

    private void createApplication() {
        deployTask().createApplication(getForm().getDeploymentInput());
    }

    private McpGraalDeployTask deployTask() {
        return new McpGraalDeployTask(
                getProject(), connection, definition, result,
                this::validateDeployability,
                ocid -> applyResolvedOcid(ocid));
    }

    /** Fills in the OCID resolved from the registry and unlocks the application-creation action. */
    private void applyResolvedOcid(String ocid) {
        Dispatch.run((ModalityState) null, () -> {
            getForm().setContainerImageOcid(ocid);
            updateActionAvailability();
        });
    }
}
