/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.common.ui.panel;

import com.dbn.common.Result;
import com.dbn.common.message.MessageType;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.dbn.common.thread.Dispatch.asyncOnce;
import static com.dbn.common.ui.link.Hyperlinks.onHyperlinkAccess;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.common.message.MessageType.SUCCESS;

/**
 * An inline asynchronous operation control with an action link, progress
 * indicator, and result banner.
 */
public class DBNAsyncOperationPanel extends JPanel {
    private final Object operationKey = new Object();

    private JPanel mainPanel;
    private DBNHyperlinkLabel operationLink;
    private JPanel progressPanel;
    private JPanel resultPanel;

    private final DBNBanner resultBanner = new DBNBanner(SUCCESS);
    private Runnable action;
    private long invalidationVersion;
    private boolean operationEnabled = true;
    private boolean operationPending;

    public DBNAsyncOperationPanel(@NotNull @Nls String actionText, @NotNull @Nls String progressText) {
        super(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        operationLink.setHyperlinkText(actionText);
        onHyperlinkAccess(operationLink, event -> runAction());

        progressPanel.add(new AsyncProcessIcon(progressText), BorderLayout.CENTER);
        progressPanel.setVisible(false);

        resultPanel.add(resultBanner, BorderLayout.CENTER);
        resultPanel.setVisible(false);
    }

    public void setAction(@Nullable Runnable action) {
        this.action = action;
    }

    private void runAction() {
        if (action != null) action.run();
    }

    public <T> boolean execute(
            @NotNull Supplier<Result<T>> operation,
            @NotNull Consumer<Result<T>> resultConsumer) {
        long operationVersion = invalidationVersion;
        boolean scheduled = asyncOnce(
                mainPanel,
                operationKey,
                () -> executeOperation(operation),
                result -> handleResult(operationVersion, result, resultConsumer));

        if (scheduled) showProgress();
        return scheduled;
    }

    private static <T> Result<T> executeOperation(Supplier<Result<T>> operation) {
        try {
            return operation.get();
        } catch (Throwable e) {
            conditionallyLog(e);
            return new Result<>(e);
        }
    }

    private <T> void handleResult(
            long operationVersion,
            Result<T> result,
            Consumer<Result<T>> resultConsumer) {
        operationPending = false;
        progressPanel.setVisible(false);

        if (operationVersion == invalidationVersion) {
            resultConsumer.accept(result);
        }
        refresh();
    }

    private void showProgress() {
        operationPending = true;
        updateOperationAvailability();
        progressPanel.setVisible(true);
        resultPanel.setVisible(false);
        refresh();
    }

    public void setOperationEnabled(boolean operationEnabled) {
        this.operationEnabled = operationEnabled;
        updateOperationAvailability();
    }

    private void updateOperationAvailability() {
        operationLink.setEnabled(operationEnabled && !operationPending);
    }

    public void invalidateOperationResult() {
        invalidationVersion++;
        if (!resultPanel.isVisible()) return;

        resultPanel.setVisible(false);
        refresh();
    }

    public void showResult(
            @NotNull MessageType messageType,
            @NotNull @Nls String message) {
        showResult(messageType, message, null);
    }

    public void showResult(
            @NotNull MessageType messageType,
            @NotNull @Nls String message,
            @Nullable @Nls String details) {
        operationPending = false;
        updateOperationAvailability();
        progressPanel.setVisible(false);
        resultBanner
                .setMessageType(messageType)
                .setMessage(message, details);
        resultPanel.setVisible(true);
        refresh();
    }

    private void refresh() {
        mainPanel.revalidate();
        mainPanel.repaint();
    }
}
