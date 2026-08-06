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

import com.dbn.mcp.registry.McpDeploymentInfo;
import com.dbn.mcp.registry.McpServerRecord;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

/**
 * The three operations a deployment consists of, in the order they must happen. They are modelled
 * separately rather than as one action because each fails for its own reasons and is worth
 * repeating on its own - the image build in particular takes minutes.
 */
public enum McpDeploymentStep {
    BUILD_IMAGE("msg.mcp.text.DeployStepBuildImage", "msg.mcp.text.DeployStepBuildImageDetail"),
    PUSH_IMAGE("msg.mcp.text.DeployStepPushImage", "msg.mcp.text.DeployStepPushImageDetail"),
    CREATE_APPLICATION("msg.mcp.text.DeployStepCreateApplication", "msg.mcp.text.DeployStepCreateApplicationDetail");

    private final @NonNls String titleKey;
    private final @NonNls String detailKey;

    McpDeploymentStep(@NonNls String titleKey, @NonNls String detailKey) {
        this.titleKey = titleKey;
        this.detailKey = detailKey;
    }

    public String getTitle() {
        return txt(titleKey);
    }

    public String getDetail() {
        return txt(detailKey);
    }

    /** When this step last succeeded, or 0 when it has not run for the given server. */
    public long completedAt(@NotNull McpServerRecord record) {
        McpDeploymentInfo deployment = record.getDeployment();
        if (deployment == null) return 0;

        return switch (this) {
            case BUILD_IMAGE -> deployment.getImageBuiltAt();
            case PUSH_IMAGE -> deployment.getImagePushedAt();
            case CREATE_APPLICATION -> deployment.getDeployTimestamp();
        };
    }

    public boolean isCompleted(@NotNull McpServerRecord record) {
        return completedAt(record) > 0;
    }

    /**
     * Whether this step can run now: every earlier step must have succeeded, and creating the
     * application additionally needs the OCID that identifies the pushed image.
     */
    public boolean isRunnable(@NotNull McpServerRecord record, @Nullable String imageOcid) {
        return switch (this) {
            case BUILD_IMAGE -> true;
            case PUSH_IMAGE -> BUILD_IMAGE.isCompleted(record);
            case CREATE_APPLICATION -> PUSH_IMAGE.isCompleted(record)
                    && McpGraalDeploymentInput.isValidContainerImageOcid(imageOcid);
        };
    }
}
