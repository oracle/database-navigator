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

package com.dbn.telemetry.listener;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/**
 * Records the visible duration of a DBN dialog each time its window is shown.
 */
@NonNls
public final class DialogTelemetryListener extends TelemetryListener {
    private static final String DURATION_METRIC_NAME = "dbn.dialog.duration";
    private static final List<Double> DURATION_BUCKETS_SECONDS = List.of(
            0.1, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0, 60.0, 120.0, 300.0, 600.0, 1800.0, 3600.0);

    private final String dialogId;
    private final IntSupplier exitCode;
    private final Supplier<ConnectionHandler> connection;
    private Map<String, String> databaseInfo = Map.of();
    private boolean visible;
    private long visibleSinceNanos;

    private DialogTelemetryListener(@NotNull DBNDialog<?> dialog) {
        this.dialogId = dialog.getDialogId();
        this.exitCode = () -> dialog.getExitCode();
        this.connection = () -> dialog.getConnection();
    }

    @Override
    protected @NotNull String getMetricName() {
        return DURATION_METRIC_NAME;
    }

    @Override
    protected @NotNull List<Double> getDurationBucketsSeconds() {
        return DURATION_BUCKETS_SECONDS;
    }

    public static void installTelemetryListener(DBNDialog<?> dialog) {
        try {
            Window window = dialog.getWindow();
            if (window == null) return;

            DialogTelemetryListener listener = new DialogTelemetryListener(dialog);
            ComponentAdapter componentListener = listener.new DialogComponentListener();
            window.addComponentListener(componentListener);
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    listener.recordVisibleDuration();
                    window.removeComponentListener(componentListener);
                    window.removeWindowListener(this);
                }
            });
        } catch (RuntimeException exception) {
            conditionallyLog(exception);
        }
    }

    private final class DialogComponentListener extends ComponentAdapter {
        @Override
        public void componentShown(ComponentEvent event) {
            synchronized (DialogTelemetryListener.this) {
                if (visible) return;
                visibleSinceNanos = start();
                visible = visibleSinceNanos != 0;
                databaseInfo = visible ? databaseAttributes(connection) : Map.of();
            }
        }

        @Override
        public void componentHidden(ComponentEvent event) {
            recordVisibleDuration();
        }
    }

    private void recordVisibleDuration() {
        long startedAtNanos;
        Map<String, String> databaseInfo;
        synchronized (this) {
            if (!visible) return;
            visible = false;
            startedAtNanos = visibleSinceNanos;
            databaseInfo = this.databaseInfo;
        }

        recordDuration(startedAtNanos, () -> {
            Map<String, String> attributes = new HashMap<>(databaseInfo);
            attributes.put("dialog_id", dialogId);
            attributes.put("exit_action", getExitAction());
            return attributes;
        });
    }

    private String getExitAction() {
        int code = exitCode.getAsInt();
        if (code == DialogWrapper.OK_EXIT_CODE) return "ok";
        if (code == DialogWrapper.CANCEL_EXIT_CODE) return "cancel";
        return "other";
    }
}
