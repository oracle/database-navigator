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

package com.dbn.sync.java.upload;

import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseMessageParserInterface;
import com.dbn.framework.batch.BatchMessageProducer;

import static com.dbn.common.presentation.Presentation.presentableTypeName;

public class JavaUploadMessageProducer implements BatchMessageProducer {
    public JavaUploadMessageProducer(JavaUploadContext context) {
        this.context = context;
    }

    private final JavaUploadContext context;


    @Override
    public String createHeaderMessage() {
        if (context.isComplete()) {
            if (context.hasErrors()) {
                return "Upload process has failed";
            } else {
                return "Upload process completed successfully";
            }
        } else {
            if (context.hasErrors()) {
                int errors = context.countErrors();
                int infos = context.countInfos();
                String infoHint = infos == 0 ? "No uploads have succeeded yet." : infos + "records have been successfully uploaded.";
                return "Around " + errors + " errors have occurred during the upload process so far. " + infoHint + " " +
                        "Please verify the messages below and decide whether to continue or interrupt the upload process";
            } else {
                // this should never really happen...
                return "Upload process successful";
            }
        }
    }

    @Override
    public String createErrorMessage(Object subject, Exception exception) {
        String message = exception.getMessage();
        ConnectionHandler connection = context.getDatabaseContext().ensureConnection();
        DatabaseMessageParserInterface messageParserInterface = connection.getMessageParserInterface();
        message = messageParserInterface.convertToPresentable(message);

        if (subject == null) return "Upload task failed.\nCause: " + message;

        String typeName = presentableTypeName(subject);
        return "Failed to upload " + typeName + " to database.\nCause: " + message;
    }

    @Override
    public String createSuccessMessage(Object subject) {
        if (subject == null) return "Upload task successfully completed.";

        String typeName = presentableTypeName(subject);
        return "Successfully uploaded " + typeName + " to database";
    }
}
