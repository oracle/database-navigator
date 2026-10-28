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

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.PersistentState;
import com.dbn.common.dispose.Checks;
import com.dbn.common.project.Projects;
import com.dbn.common.thread.Progress;
import com.dbn.telemetry.data.TelemetryBatchCache;
import com.dbn.telemetry.data.TelemetryReport;
import com.dbn.telemetry.metrics.Metric;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.io.HttpRequests;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

/**
 * Persists interaction-driven telemetry batches and submits closed windows in the background.
 */
@State(name = TelemetryManager.COMPONENT_NAME,
        storages = @Storage(value = "dbn-telemetry.xml", roamingType = RoamingType.DISABLED))
public class TelemetryManager extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.TelemetryManager";

    @NonNls private static final String ENDPOINT = "https://dwz5kzmzlwcwcxq7clhtrcxwkm.apigateway.eu-zurich-1.oci.customer-oci.com/telemetry/v1/batches";
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 20_000;
    private static final int MAX_REPORT_BYTES = 1024 * 1024;
    private static final int MAX_ERROR_RESPONSE_BYTES = 4096;
    private static final String METRICS_VERSION = "1";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TelemetryBatchCache telemetryCache = new TelemetryBatchCache();
    private final AtomicBoolean sending = new AtomicBoolean();

    private TelemetryManager() {
        super(COMPONENT_NAME);
    }

    public static TelemetryManager getInstance() {
        return applicationService(TelemetryManager.class);
    }

    /** Rotates the complete collection window before merging the current observation. */
    public void recordMetric(@NotNull Metric metric) {
        long now = System.currentTimeMillis();
        telemetryCache.record(metric, now, () -> createReport(now));
        sendPendingBatches();
    }

    public @NotNull List<Metric> getCollectedMetricsSnapshot() {
        return telemetryCache.snapshot();
    }

    /** Explicitly closes the current window and submits pending reports regardless of age. */
    public void flushAndReport() {
        telemetryCache.closeActiveBatch(System.currentTimeMillis());
        sendPendingBatches();
    }

    private void sendPendingBatches() {
        if (!telemetryCache.hasPendingReports()) return;
        if (sending.get()) return;

        boolean acquired = false;
        try {
            Project project = Projects.getDefaultProject();
            if (Checks.isNotValid(project)) return;
            if (!sending.compareAndSet(false, true)) return;

            acquired = true;
            Progress.background(project, null, false,
                    txt("prc.telemetry.title.SendingTelemetry"),
                    txt("prc.telemetry.text.SendingTelemetry"), indicator -> {
                boolean completed = false;
                try {
                    for (TelemetryReport report : telemetryCache.pendingReports()) {
                        submitReport(report);
                        telemetryCache.acknowledge(report.batchId());
                    }
                    completed = true;
                } catch (Throwable e) {
                    conditionallyLog(e);
                } finally {
                    sending.set(false);
                    if (completed && telemetryCache.hasPendingReports()) sendPendingBatches();
                }
            });
        } catch (Throwable e) {
            if (acquired) sending.set(false);
            conditionallyLog(e);
        }
    }

    @Override
    public Element getComponentState() {
        Element state = new Element("state");
        telemetryCache.writeState(state);
        return state;
    }

    @Override
    public void loadComponentState(@NotNull Element state) {
        try {
            telemetryCache.readState(state);
        } catch (RuntimeException exception) {
            conditionallyLog(exception);
        }
    }

    /** Sends a closed batch; its identifier and contents remain unchanged across retries. */
    private int submitReport(TelemetryReport report) throws IOException {
        byte[] payload = OBJECT_MAPPER.writeValueAsBytes(report);
        if (payload.length > MAX_REPORT_BYTES) {
            throw new IOException("Telemetry report exceeds the 1 MiB gateway limit");
        }

        return HttpRequests.post(ENDPOINT, HttpRequests.JSON_CONTENT_TYPE)
                .connectTimeout(CONNECT_TIMEOUT_MILLIS)
                .readTimeout(READ_TIMEOUT_MILLIS)
                .accept("application/json")
                .tuner(connection -> connection.setUseCaches(false))
                .connect(request -> {
                    request.write(payload);
                    HttpURLConnection connection = (HttpURLConnection) request.getConnection();
                    int status = connection.getResponseCode();
                    if (status < HttpURLConnection.HTTP_OK || status >= HttpURLConnection.HTTP_MULT_CHOICE) {
                        throw new IOException(createErrorMessage(connection, status));
                    }
                    request.getInputStream().transferTo(OutputStream.nullOutputStream());
                    return status;
                });
    }

    private static Map<String, Object> createReport(long startedAt) {
        DatabaseNavigator databaseNavigator = DatabaseNavigator.getInstance();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("batch_id", UUID.randomUUID().toString());
        report.put("client_id", toStandardUuid(databaseNavigator.getClientId()));
        report.put("plugin_version", databaseNavigator.getPluginVersion());
        report.put("ide_version", ApplicationInfo.getInstance().getFullVersion());
        report.put("java_version", System.getProperty("java.version"));
        report.put("metrics_version", METRICS_VERSION);
        report.put("period_start", Instant.ofEpochMilli(startedAt).toString());
        report.put("period_end", null);
        return report;
    }

    private static String toStandardUuid(String clientId) {
        if (clientId != null && clientId.matches("(?i)[0-9a-f]{32}")) {
            return clientId.substring(0, 8) + "-" +
                    clientId.substring(8, 12) + "-" +
                    clientId.substring(12, 16) + "-" +
                    clientId.substring(16, 20) + "-" +
                    clientId.substring(20);
        }
        return clientId;
    }

    private static String createErrorMessage(HttpURLConnection connection, int status) throws IOException {
        String message = "Telemetry gateway returned HTTP " + status;
        InputStream errorStream = connection.getErrorStream();
        if (errorStream == null) return message;

        try (errorStream) {
            byte[] errorBytes = errorStream.readNBytes(MAX_ERROR_RESPONSE_BYTES);
            String errorContent = new String(errorBytes, StandardCharsets.UTF_8).trim();
            if (!errorContent.isEmpty()) {
                message += ": " + errorContent;
            }
        }
        return message;
    }

}
