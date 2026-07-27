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

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.artifacts.ArtifactsClient;
import com.oracle.bmc.artifacts.model.ContainerImageSummary;
import com.oracle.bmc.artifacts.requests.ListContainerImagesRequest;
import com.oracle.bmc.artifacts.responses.ListContainerImagesResponse;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Resolves the OCID that OCI assigns to a pushed container image. Graal's create_application
 * accepts the image OCID only - "docker push" reports a digest, never an OCID - so after a
 * successful push the registry has to be queried for it.
 * <p>
 * Authentication comes from the standard OCI configuration file (~/.oci/config by default), so
 * no credentials are ever entered into or held by the plugin.
 */
@Slf4j
final class McpOciImageResolver {

    /** Matches the deployed image by repository and tag, newest first when a tag was reused. */
    @Nullable
    String resolveImageOcid(@NotNull McpGraalDeploymentInput input) throws IOException {
        ConfigFileReader.ConfigFile configFile = ConfigFileReader.parseDefault();
        ConfigFileAuthenticationDetailsProvider authProvider =
                new ConfigFileAuthenticationDetailsProvider(configFile);

        @NonNls String compartmentId = authProvider.getTenantId();
        if (compartmentId == null) {
            throw new IOException(txt("msg.mcp.exception.OciConfigIncomplete"));
        }

        try (ArtifactsClient client = ArtifactsClient.builder().build(authProvider)) {
            // the image lives in the region it was pushed to, which is not necessarily the
            // default region of ~/.oci/config - querying the wrong region silently finds nothing
            client.setRegion(Region.fromRegionCodeOrId(input.getRegionKey()));

            ListContainerImagesRequest request = ListContainerImagesRequest.builder()
                    .compartmentId(compartmentId)
                    // the repository usually lives in a child compartment, not the tenancy root
                    .compartmentIdInSubtree(Boolean.TRUE)
                    .repositoryName(input.getRepository())
                    .version(input.getTag())
                    .build();

            ListContainerImagesResponse response = client.listContainerImages(request);
            List<ContainerImageSummary> items = response.getContainerImageCollection().getItems();
            if (items == null || items.isEmpty()) return null;

            return items.stream()
                    .max(Comparator.comparing(ContainerImageSummary::getTimeCreated,
                            Comparator.nullsFirst(Comparator.naturalOrder())))
                    .map(ContainerImageSummary::getId)
                    .orElse(null);
        }
    }
}
