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

import com.dbn.common.message.MessageBundle;
import com.dbn.common.message.MessageCollector;
import com.dbn.common.progress.ProgressIndicatorDelegate;
import com.dbn.common.routine.ThrowableCallable;
import com.intellij.openapi.progress.ProgressIndicator;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.common.thread.Progress.progressOf;

@Getter
public class DownloadSession extends ProgressIndicatorDelegate {
    private static final ThreadLocal<DownloadSession> LOCAL = new ThreadLocal<>();

    @Delegate
    private final MessageBundle messages = new MessageCollector(true);
    private int downloadSize;
    private String downloadPath;
    private AtomicInteger outstandingSize;
    private CountDownLatch countDownLatch;

    private final List<String> downloadedArtifacts = new CopyOnWriteArrayList<>();

    public DownloadSession(ProgressIndicator progressIndicator) {
        super(progressIndicator);
        setIndeterminate(false);
        setFraction(0.0);
    }

    public DownloadSession withDownloadSize(int downloadSize) {
         this.downloadSize = downloadSize;
         this.outstandingSize = new AtomicInteger(downloadSize);
         return this;
    }

    public DownloadSession withDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
        return this;
    }

    public DownloadSession withLatchControl() {
        if (downloadSize == 0) throw new IllegalStateException("Download size must be set before latch control is enabled");
        this.countDownLatch = new CountDownLatch(downloadSize);
        return this;
    }

    public void countDown() {
        outstandingSize.decrementAndGet();
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }

    public void updateProgress() {
        super.setFraction(getProgress());
    }

    public void updateProgress(String text2) {
        super.setFraction(getProgress());
        super.setText2(text2);
    }

    @Override
    public void setFraction(double fraction) {
        // prevent updates from within com.intellij.platform.templates.github.DownloadUtil
    }

    @Override
    public void setText2(String text) {
        // prevent updates from within com.intellij.platform.templates.github.DownloadUtil
    }

    public boolean isComplete() {
        return outstandingSize.get() == 0;
    }

    @SneakyThrows
    public boolean awaitCompletion() {
        return countDownLatch.await(500, TimeUnit.MILLISECONDS);
    }

    public double getProgress() {
        return progressOf(downloadSize - getOutstandingSize(), downloadSize);
    }

    public int getOutstandingSize() {
        return outstandingSize.get();
    }

    public void addDownloadedArtifacts(String artifactId) {
        downloadedArtifacts.add(artifactId);
    }

    public static DownloadSession current() {
        return LOCAL.get();
    }

    public <R, E extends Throwable> R surround(ThrowableCallable<R, E> callable) throws E {
        LOCAL.set(this);
        try {
            return callable.call();
        } finally {
            LOCAL.remove();
        }
    }
}
