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

import com.dbn.batch.impl.BatchMessengerBase;
import com.dbn.common.message.MessageType;
import org.jetbrains.annotations.Nullable;

public class JavaDownloadMessenger extends BatchMessengerBase<JavaDownloadTask, JavaDownloadInput, JavaDownloadBatch> {
    public static final JavaDownloadMessenger INSTANCE = new JavaDownloadMessenger();

    private JavaDownloadMessenger() {}

    @Override
    public String getBatchTitle(JavaDownloadBatch batch) {
        return "Java Download Process";
    }

    @Override
    public String getProgressTitle(JavaDownloadBatch batch) {
        if (batch.isRunning() || batch.isPaused()) return "Downloading java resources...";
        if (batch.isCancelled()) return "Download Cancelled";
        if (batch.isFinished()) {
            MessageType messageType = getProgressMessageType(batch);
            switch (messageType) {
                case SUCCESS: return "Download Complete";
                case WARNING: return "Download Partially Complete";
                case ERROR: return "Download Failed";
                default: return "Download Finished";
            }
        }

        return "Java Download";
    }

    @Override
    public String getProgressMessage(JavaDownloadBatch batch, @Nullable JavaDownloadTask task) {
        String progressText = getProgressText(batch);
        if (task != null) return "Downloading " + task.getName() + " " + progressText;
        if (batch.isPaused()) return "Paused " + progressText;
        if (batch.isCancelled()) return "Java resource download cancelled\n" + progressText;
        if (batch.isFinished()) return "Java resource download finished\n" + progressText;
        return "";
    }

    @Override
    public String getTaskInitMessage(JavaDownloadBatch batch, JavaDownloadTask task) {
        return "Downloading java resource...";
    }

    @Override
    public String getTaskSuccessMessage(JavaDownloadBatch batch, JavaDownloadTask task) {
        return "Java resource successfully downloaded\n" + task.getTargetFile().getPath() + "";
    }

    @Override
    public String getTaskFailureMessage(JavaDownloadBatch batch, JavaDownloadTask task, Exception e) {
        String message = e.getMessage();
        message = cleanExceptionMessage(batch, message);
        return "Failed to download java resource\nCause:" + message;
    }
}
