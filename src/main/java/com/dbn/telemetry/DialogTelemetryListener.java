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

package com.dbn.telemetry;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.telemetry.metrics.HistogramMetric;
import com.dbn.telemetry.metrics.MetricKey;
import com.dbn.telemetry.metrics.MetricType;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/**
 * Records the visible duration of a DBN dialog each time its window is shown.
 */
public final class DialogTelemetryListener extends ComponentAdapter {
    private static final String DURATION_METRIC_NAME = "dbn.dialog.duration";
    private static final String DURATION_UNIT = "s";
    private static final List<Double> DURATION_BUCKETS_SECONDS = List.of(
            0.1, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0, 60.0, 120.0, 300.0, 600.0, 1800.0, 3600.0);

    private final String dialogId;
    private final IntSupplier exitCode;
    private boolean visible;
    private long visibleSinceNanos;

    private DialogTelemetryListener(@NotNull String dialogId, @NotNull IntSupplier exitCode) {
        this.dialogId = dialogId;
        this.exitCode = exitCode;
    }

    public static void installTelemetryListener(DBNDialog dialog) {
        try {
            Window window = dialog.getWindow();
            if (window == null) return;

            String dialogId = dialog.getDialogId();
            IntSupplier exitCode = () -> dialog.getExitCode();

            DialogTelemetryListener listener = new DialogTelemetryListener(dialogId, exitCode);
            window.addComponentListener(listener);
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    listener.recordVisibleDuration();
                    window.removeComponentListener(listener);
                    window.removeWindowListener(this);
                }
            });
        } catch (RuntimeException exception) {
            conditionallyLog(exception);
        }
    }

    @Override
    public void componentShown(ComponentEvent event) {
        synchronized (this) {
            if (visible) return;
            visible = true;
            visibleSinceNanos = System.nanoTime();
        }
    }

    @Override
    public void componentHidden(ComponentEvent event) {
        recordVisibleDuration();
    }

    private void recordVisibleDuration() {
        long startedAtNanos;
        synchronized (this) {
            if (!visible) return;
            visible = false;
            startedAtNanos = visibleSinceNanos;
        }

        double durationSeconds = (System.nanoTime() - startedAtNanos) / 1_000_000_000.0;
        if (durationSeconds <= 0) return;

        try {
            MetricKey key = new MetricKey(
                    DURATION_METRIC_NAME,
                    MetricType.HISTOGRAM,
                    DURATION_UNIT,
                    Map.of("dialog_id", dialogId, "exit_action", getExitAction()));
            TelemetryManager.getInstance().recordMetric(
                    HistogramMetric.observation(key, DURATION_BUCKETS_SECONDS, durationSeconds));
        } catch (RuntimeException exception) {
            // Telemetry collection must not interfere with dialog lifecycle.
            conditionallyLog(exception);
        }
    }

    private String getExitAction() {
        int code = exitCode.getAsInt();
        if (code == DialogWrapper.OK_EXIT_CODE) return "ok";
        if (code == DialogWrapper.CANCEL_EXIT_CODE) return "cancel";
        return "other";
    }
}
