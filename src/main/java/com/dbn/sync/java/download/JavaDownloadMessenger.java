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

import com.dbn.batch.BatchMessenger;

public class JavaDownloadMessenger implements BatchMessenger<JavaDownloadTask, JavaDownloadInput, JavaDownloadBatch> {
    public static final JavaDownloadMessenger INSTANCE = new JavaDownloadMessenger();

    private JavaDownloadMessenger() {}

    @Override
    public String getBatchTitle(JavaDownloadBatch batch) {
        return "Java Download Process";
    }

    @Override
    public String getBatchProgressMessage(JavaDownloadBatch batch) {
        return "Downloading java resources...";
    }

    @Override
    public String createTaskInitMessage(JavaDownloadBatch batch, JavaDownloadTask task) {
        return "Downloading java resource...";
    }

    @Override
    public String createTaskSuccessMessage(JavaDownloadBatch batch, JavaDownloadTask task) {
        return "Java resource successfully downloaded\n" + task.getTargetFile().getPath() + "";
    }

    @Override
    public String createTaskErrorMessage(JavaDownloadBatch batch, JavaDownloadTask task, Exception e) {
        return "Failed to download java resource\nCause:" +  e.getMessage();
    }
}
