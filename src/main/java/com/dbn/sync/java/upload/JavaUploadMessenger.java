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
import com.dbn.common.message.MessageType;
import org.jetbrains.annotations.Nullable;

public class JavaUploadMessenger extends BatchMessengerBase<JavaUploadTask, JavaUploadInput, JavaUploadBatch> {
    public static final JavaUploadMessenger INSTANCE = new JavaUploadMessenger();

    private JavaUploadMessenger() {}

    @Override
    public String getBatchTitle(JavaUploadBatch batch) {
        return "Java Upload Process";
    }

    @Override
    public String getProgressTitle(JavaUploadBatch batch) {
        if (batch.isRunning() || batch.isPaused()) return "Uploading java resources...";
        if (batch.isCancelled()) return "Upload Cancelled";
        if (batch.isFinished()) {
            MessageType messageType = getProgressMessageType(batch);
            switch (messageType) {
                case SUCCESS: return "Upload Complete";
                case WARNING: return "Upload Partially Complete";
                case ERROR: return "Upload Failed";
                default: return "Upload Finished";
            }
        }

        return "Java Upload";
    }

    @Override
    public String getProgressMessage(JavaUploadBatch batch, @Nullable JavaUploadTask task) {
        String progressText = getProgressText(batch);
        if (task != null) return "Uploading " + task.getName() + " " + progressText;
        if (batch.isPaused()) return "Paused " + progressText;
        if (batch.isCancelled()) return "Java resource upload cancelled\n" + progressText;
        if (batch.isFinished()) return "Java resource upload finished\n" + progressText;
        return "";
    }

    @Override
    public String getTaskInitMessage(JavaUploadBatch batch, JavaUploadTask task) {
        return "Uploading java resource...";
    }

    @Override
    public String getTaskSuccessMessage(JavaUploadBatch batch, JavaUploadTask task) {
        String qualification = " (\"" + batch.getConnectionName() + "\" / \"" + batch.getInput().getTargetSchemaName() + "\")";

        if (task.isJavaLibrary()) return "Java library successfully unpacked";

        if (task.isJavaClass()) return "Java class successfully uploaded" + qualification;
        if (task.isJavaSource()) return "Java source successfully uploaded" + qualification;
        if (task.isJavaResource()) return "Java resource successfully uploaded" + qualification;

        return "Task successfully completed";
    }

    @Override
    public String getTaskFailureMessage(JavaUploadBatch batch, JavaUploadTask task, Exception e) {
        String message = e.getMessage();
        message = cleanExceptionMessage(batch, message);

        return "Failed to upload java resource \nCause:" + message;
    }
}
