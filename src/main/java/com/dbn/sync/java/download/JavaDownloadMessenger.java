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

import static com.dbn.nls.NlsResources.txt;

public class JavaDownloadMessenger extends BatchMessengerBase<JavaDownloadTask, JavaDownloadInput, JavaDownloadBatch> {
    public static final JavaDownloadMessenger INSTANCE = new JavaDownloadMessenger();

    private JavaDownloadMessenger() {}

    @Override
    public String getBatchTitle(JavaDownloadBatch batch) {
        return txt("app.java.title.JavaDownloadProcess");
    }

    @Override
    public String getProgressTitle(JavaDownloadBatch batch) {
        if (batch.isRunning() || batch.isPaused()) return txt("app.java.title.DownloadingJavaResources");
        if (batch.isCancelled()) return txt("app.java.title.DownloadCanceled");
        if (batch.isFinished()) {
            MessageType messageType = getProgressMessageType(batch);
            return switch (messageType) {
                case SUCCESS -> txt("app.java.title.DownloadComplete");
                case WARNING -> txt("app.java.title.DownloadPartiallyComplete");
                case ERROR -> txt("app.java.title.DownloadFailed");
                default -> txt("app.java.title.DownloadFinished");
            };
        }

        return txt("app.java.title.JavaDownload");
    }

    @Override
    public String getProgressMessage(JavaDownloadBatch batch, @Nullable JavaDownloadTask task) {
        String progressText = getProgressText(batch);
        if (task != null) return txt("app.java.text.DownloadingJavaResourceNamed", task.getName(), progressText);
        if (batch.isPaused()) return txt("app.batch.text.Paused", progressText);
        if (batch.isCancelled()) return txt("app.java.text.JavaResourceDownloadCanceled", progressText);
        if (batch.isFinished()) return txt("app.java.text.JavaResourceDownloadFinished", progressText);
        return "";
    }

    @Override
    public String getTaskInitMessage(JavaDownloadBatch batch, JavaDownloadTask task) {
        return txt("app.java.text.DownloadingJavaResource");
    }

    @Override
    public String getTaskSuccessMessage(JavaDownloadBatch batch, JavaDownloadTask task) {
        return txt("app.java.text.JavaResourceDownloaded", task.getTargetFile().getPath());
    }

    @Override
    public String getTaskFailureMessage(JavaDownloadBatch batch, JavaDownloadTask task, Exception e) {
        String message = e.getMessage();
        message = cleanExceptionMessage(batch, message);
        return txt("app.java.error.JavaResourceDownloadFailed", message);
    }
}
