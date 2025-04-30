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
import com.dbn.framework.batch.BatchProducer;

import static com.dbn.common.presentation.Presentation.presentableTypeName;

public class JavaDownloadMessageProducer implements BatchProducer {
    public JavaDownloadMessageProducer(JavaDownloadContext context) {
        this.context = context;
    }

    private final JavaDownloadContext context;


    @Override
    public String createHeaderMessage() {
        if (context.isComplete()) {
            if (context.hasErrors()) {
                return "Download process has failed";
            } else {
                return "Download process completed successfully";
            }
        } else {
            if (context.hasErrors()) {
                int errors = context.countErrors();
                int infos = context.countInfos();
                String infoHint = infos == 0 ? "No downloads have succeeded yet." : infos + "records have been successfully downloaded.";

                return "Around " + errors + " errors have occurred during the download process so far. " + infoHint + " " +
                        "Please verify the messages below and decide whether to continue or interrupt the download process";
            } else {
                // this should never really happen...
                return "Download process successful";
            }
        }
    }

    @Override
    public String createErrorMessage(Object data, Exception exception) {
        String typeName = presentableTypeName(data);

        ConnectionHandler connection = context.getDatabaseContext().ensureConnection();
        DatabaseMessageParserInterface messageParserInterface = connection.getMessageParserInterface();

        String message = exception.getMessage();
        message = messageParserInterface.convertToPresentable(message);


        return "Failed to download " + typeName + " to project.\nCause: " + message;
    }

    @Override
    public String createSuccessMessage(Object data) {
        String typeName = presentableTypeName(data);
        return "Successfully downloaded " + typeName + " to project";
    }
}
