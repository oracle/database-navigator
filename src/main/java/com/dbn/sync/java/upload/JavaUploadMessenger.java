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

import static com.dbn.nls.NlsResources.txt;

public class JavaUploadMessenger extends BatchMessengerBase<JavaUploadTask, JavaUploadInput, JavaUploadBatch> {
    public static final JavaUploadMessenger INSTANCE = new JavaUploadMessenger();

    private JavaUploadMessenger() {}

    @Override
    public String getBatchTitle(JavaUploadBatch batch) {
        return txt("app.java.title.JavaUploadProcess");
    }

    @Override
    public String getProgressTitle(JavaUploadBatch batch) {
        if (batch.isRunning() || batch.isPaused()) return txt("app.java.title.UploadingJavaResources");
        if (batch.isCancelled()) return txt("app.java.title.UploadCancelled");
        if (batch.isFinished()) {
            MessageType messageType = getProgressMessageType(batch);
            return switch (messageType) {
                case SUCCESS -> txt("app.java.title.UploadComplete");
                case WARNING -> txt("app.java.title.UploadPartiallyComplete");
                case ERROR -> txt("app.java.title.UploadFailed");
                default -> txt("app.java.title.UploadFinished");
            };
        }

        return txt("app.java.title.JavaUpload");
    }

    @Override
    public String getProgressMessage(JavaUploadBatch batch, @Nullable JavaUploadTask task) {
        String progressText = getProgressText(batch);
        if (task != null) return txt("app.java.text.UploadingJavaResourceNamed", task.getName(), progressText);
        if (batch.isPaused()) return txt("app.batch.text.Paused", progressText);
        if (batch.isCancelled()) return txt("app.java.text.JavaResourceUploadCancelled", progressText);
        if (batch.isFinished()) return txt("app.java.text.JavaResourceUploadFinished", progressText);
        return "";
    }

    @Override
    public String getTaskInitMessage(JavaUploadBatch batch, JavaUploadTask task) {
        if (task.isJavaLibrary()) return txt("app.java.text.UnpackingJavaLibrary");
        if (task.isJavaClass()) return txt("app.java.text.UploadingJavaClass");
        if (task.isJavaSource()) return txt("app.java.text.UploadingJavaSource");
        if (task.isJavaResource()) return txt("app.java.text.UploadingJavaResource");

        return txt("app.java.text.UploadingJavaResource");
    }

    @Override
    public String getTaskSuccessMessage(JavaUploadBatch batch, JavaUploadTask task) {
        if (task.isJavaLibrary()) return txt("app.java.text.JavaLibraryUnpacked");

        String connectionName = batch.getConnectionName();
        String qualifiedName = task.getDatabaseEntity().getQualifiedName();
        if (task.isJavaClass()) return txt("app.java.text.JavaClassUploaded", connectionName, qualifiedName);
        if (task.isJavaSource()) return txt("app.java.text.JavaSourceUploaded", connectionName, qualifiedName);
        if (task.isJavaResource()) return txt("app.java.text.JavaResourceUploaded", connectionName, qualifiedName);

        return txt("app.batch.text.TaskCompleted");
    }

    @Override
    public String getTaskFailureMessage(JavaUploadBatch batch, JavaUploadTask task, Exception e) {
        String message = e.getMessage();
        message = cleanExceptionMessage(batch, message);

        return txt("app.java.error.JavaResourceUploadFailed", message);
    }
}
