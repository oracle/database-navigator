/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.driver.download;

import com.dbn.common.message.AsyncMessageCollector;
import com.dbn.common.message.Message;
import com.dbn.common.message.MessageCollector;
import com.intellij.openapi.progress.ProgressIndicator;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

public class DownloadSession {
    private final MessageCollector messages = new AsyncMessageCollector();
    @Getter
    private final String downloadPath;
    @Getter
    private final ProgressIndicator progressIndicator;
    @Getter
    private final CountDownLatch latch;
    @Getter
    private final List<String> downloadedArtifacts = new CopyOnWriteArrayList<>();

    public DownloadSession(ProgressIndicator progressIndicator, String path, int size) {
        this.progressIndicator = progressIndicator;
        this.downloadPath = path;
        this.latch = new CountDownLatch(size);
    }

    public void countDown() {
        latch.countDown();
    }

    public void addInfoMessage(String message) {
        messages.addInfoMessage(message);
    }

    public void addErrorMessage(String message) {
        messages.addErrorMessage(message);
    }

    public List<Message> getInfoMessages() {
        return messages.getInfoMessages();
    }

    public List<Message> getErrorMessages() {
        return messages.getErrorMessages();
    }

    public void addDownloadedArtifacts(String artifactId) {
        downloadedArtifacts.add(artifactId);
    }
}
