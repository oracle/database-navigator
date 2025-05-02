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

package com.dbn.sync.java.download;

import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseMessageParserInterface;
import com.dbn.framework.batch.BatchMessageProducer;

import static com.dbn.common.presentation.Presentation.presentableTypeName;

public class JavaDownloadMessageProducer implements BatchMessageProducer {
    public JavaDownloadMessageProducer(JavaDownloadContext context) {
        this.context = context;
    }

    private final JavaDownloadContext context;

    @Override
    public String createErrorDialogTitle() {
        return "Download Errors";
    }

    @Override
    public String createErrorResolutionMessage() {
        int errors = context.countErrors();
        int infos = context.countInfos();
        String successHint = infos == 0 ? "No downloads have succeeded yet." : infos + "records have been successfully downloaded.";
        return "Around " + errors + " errors have occurred during the download process so far. " + successHint + " " +
                "Please verify the messages below and decide whether to continue or cancel the download process";
    }

    @Override
    public String createErrorMessage(Object subject, Exception exception) {
        String message = exception.getMessage();
        ConnectionHandler connection = context.getDatabaseContext().ensureConnection();
        DatabaseMessageParserInterface messageParserInterface = connection.getMessageParserInterface();
        message = messageParserInterface.convertToPresentable(message);

        if (subject == null) return "Download task failed.\nCause: " + message;

        String typeName = presentableTypeName(subject);
        return "Failed to download " + typeName + " to project.\nCause: " + message;
    }

    @Override
    public String createSuccessMessage(Object subject) {
        if (subject == null) return "Download task successfully completed.";

        String typeName = presentableTypeName(subject);
        return "Successfully downloaded " + typeName + " to project";
    }
}
