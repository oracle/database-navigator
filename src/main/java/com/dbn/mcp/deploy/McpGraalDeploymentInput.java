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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NonNls;

import java.util.regex.Pattern;

/**
 * Immutable deployment values collected from the deployment dialog. Deliberately carries no
 * credentials: authentication to the container registry is established by the user out of band
 * ("docker login"), and database access is granted to the Graal application by the platform.
 */
@Getter
@RequiredArgsConstructor
public final class McpGraalDeploymentInput {
    /** Graal requires the image OCID rather than the image name or tag. */
    public static final @NonNls String CONTAINER_IMAGE_OCID_PREFIX = "ocid1.containerimage.";

    private static final @NonNls Pattern REGION_KEY = Pattern.compile("[a-z0-9]{2,8}");
    private static final @NonNls String OCIR_HOST_SUFFIX = ".ocir.io";

    private final String applicationName;
    private final String regionKey;
    private final String namespace;
    private final String repository;
    private final String tag;
    private final String containerImageOcid;

    /** Fully qualified OCIR image reference: {@code <region-key>.ocir.io/<namespace>/<repository>:<tag>}. */
    public @NonNls String getFullImageName() {
        return regionKey + OCIR_HOST_SUFFIX + "/" + namespace + "/" + repository + ":" + tag;
    }

    public static boolean isValidRegionKey(String value) {
        return value != null && REGION_KEY.matcher(value.trim()).matches();
    }

    public static boolean isValidContainerImageOcid(String value) {
        return value != null && value.trim().startsWith(CONTAINER_IMAGE_OCID_PREFIX);
    }
}
