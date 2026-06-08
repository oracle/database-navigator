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

package com.dbn.vector.service;

import com.dbn.common.Priority;
import com.dbn.common.util.Json;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.request.EmbeddingDestinationConfig;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import java.sql.SQLException;
import java.util.Map;

import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.nls.NlsResources.txt;

public class VectorEmbeddingRequestVerifier {
    public static boolean verifyRequest(VectorEmbeddingRequest request, ProgressIndicator indicator) {
        Project project = request.getProject();
        try {
            if (!verifyDestinationModelMatch(request, indicator)) return false;
            //..

        } catch (Exception e) {
            showErrorDialog(project,
                    txt("msg.vector.title.VerificationError"),
                    txt("msg.vector.error.EmbeddingRequestVerificationFailed"), e);
            return false;
        }

        return true;
    }

    private static boolean verifyDestinationModelMatch(VectorEmbeddingRequest request, ProgressIndicator indicator) throws SQLException {
        Project project = request.getProject();
        ConnectionHandler connection = request.getConnection();

        indicator.setText2(txt("prc.vector.text.VerifyingDestinationModelConsistency"));
        String modelMetadata = DatabaseInterfaceInvoker.load(Priority.MEDIUM, project, request.getConnectionId(), conn -> {
            DatabaseVectorInterface vectorInterface = connection.getVectorInterface();
            EmbeddingDestinationConfig destinationConfig = request.getDestinationConfig();
            return vectorInterface.loadDestinationModelMetadata(conn,
                    destinationConfig.getSchemaName(),
                    destinationConfig.getTableName());
        });
        if (indicator.isCanceled()) return false;

        if (Strings.isEmpty(modelMetadata)) return true; // no records available yet

        Map<String, ?> destinationModelConfig = Json.readAsMap(modelMetadata);
        Map<String, ?> requestModelConfig = request.getModelConfig().getConfigMap();
        if (destinationModelConfig.equals(requestModelConfig)) return true;

        String destinationModel = String.valueOf(destinationModelConfig.get("model"));
        String requestModel = String.valueOf(requestModelConfig.get("model"));


        showErrorDialog(project,
                txt("msg.vector.title.InconsistentModelSelection"),
                txt("msg.vector.error.InconsistentModelSelection",
                        request.getDestinationConfig().getQualifiedTableName(),
                        requestModel,
                        destinationModel));
        return false;
    }
}
