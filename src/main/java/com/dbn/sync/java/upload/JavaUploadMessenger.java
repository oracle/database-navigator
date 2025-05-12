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

import com.dbn.batch.impl.BatchMessengerBase;

public class JavaUploadMessenger extends BatchMessengerBase<JavaUploadTask, JavaUploadInput, JavaUploadBatch> {
    public static final JavaUploadMessenger INSTANCE = new JavaUploadMessenger();

    private JavaUploadMessenger() {}

    @Override
    public String getBatchTitle(JavaUploadBatch batch) {
        return "Java Upload Process";
    }

    @Override
    public String getBatchProgressTitle(JavaUploadBatch batch) {
        return "Uploading java resources...";
    }

    @Override
    public String getBatchProgressDetail(JavaUploadBatch batch) {
        // TODO conditional progress detail (current state / counters)
        return "";
    }

    @Override
    public String createTaskInitMessage(JavaUploadBatch batch, JavaUploadTask task) {
        return "Uploading java resource...";
    }

    @Override
    public String createTaskSuccessMessage(JavaUploadBatch batch, JavaUploadTask task) {
        return "Java resource successfully uploaded (\"" + batch.getConnectionName() + "\" / \"" + batch.getInput().getTargetSchemaName() + "\")" ;
    }

    @Override
    public String createTaskErrorMessage(JavaUploadBatch batch, JavaUploadTask task, Exception e) {
        String message = e.getMessage();
        message = cleanExceptionMessage(batch, message);

        return "Failed to upload java resource \nCause:" + message;
    }
}
