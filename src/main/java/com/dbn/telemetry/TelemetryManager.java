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
import com.dbn.telemetry.metrics.Metric;
import com.dbn.telemetry.metrics.MetricCollector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.util.io.HttpRequests;
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

import static com.dbn.common.component.Components.applicationService;

/**
 * Submits a single batch of DBN telemetry metrics to the telemetry API Gateway.
 * Calls are synchronous; callers should invoke this method on a background thread.
 */
public class TelemetryManager extends ApplicationComponentBase {
    public static final String COMPONENT_NAME = "DBNavigator.Application.TelemetryManager";

    @NonNls private static final String ENDPOINT = "https://dwz5kzmzlwcwcxq7clhtrcxwkm.apigateway.eu-zurich-1.oci.customer-oci.com/telemetry/v1/batches";
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 20_000;
    private static final int MAX_REPORT_BYTES = 1024 * 1024;
    private static final int MAX_ERROR_RESPONSE_BYTES = 4096;
    private static final String METRICS_VERSION = "1";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final MetricCollector metricCollector = new MetricCollector();

    private TelemetryManager() {
        super(COMPONENT_NAME);
    }

    public static TelemetryManager getInstance() {
        return applicationService(TelemetryManager.class);
    }

    /**
     * Records one metric observation in the in-memory aggregate collector.
     */
    public void recordMetric(@NotNull Metric metric) {
        metricCollector.record(metric);
    }

    /**
     * Returns a consistent snapshot of metrics collected so far without resetting them.
     */
    public @NotNull List<Metric> getCollectedMetricsSnapshot() {
        return metricCollector.snapshot();
    }

    /**
     * Atomically returns and clears the current metric aggregates. A sender should retain this
     * snapshot and restore it to the collector if submission fails.
     */
    public @NotNull List<Metric> drainCollectedMetrics() {
        return metricCollector.drain();
    }

    /**
     * Sends the supplied metric objects in one batch. The objects are serialized as the
     * {@code items} array inside the JSON {@code metrics} object.
     *
     * @return the HTTP success status returned by the gateway, normally 202
     * @throws IOException if serialization, the network request, or the gateway response fails
     */
    public int submitMetrics(@NotNull List<?> metrics) throws IOException {
        if (metrics.isEmpty()) {
            throw new IllegalArgumentException("At least one telemetry metric is required");
        }

        List<?> metricSnapshot = List.copyOf(metrics);
        Map<String, Object> report = createReport(metricSnapshot);
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

    private static Map<String, Object> createReport(List<?> metrics) {
        DatabaseNavigator databaseNavigator = DatabaseNavigator.getInstance();

        Map<String, Object> metricsContent = new LinkedHashMap<>();
        metricsContent.put("items", metrics);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("batch_id", UUID.randomUUID().toString());
        report.put("client_id", toStandardUuid(databaseNavigator.getClientId()));
        report.put("plugin_version", databaseNavigator.getPluginVersion());
        report.put("ide_version", ApplicationInfo.getInstance().getFullVersion());
        report.put("java_version", System.getProperty("java.version"));
        report.put("metrics_version", METRICS_VERSION);
        report.put("period_start", null);
        report.put("period_end", Instant.now().toString());
        report.put("metrics", metricsContent);
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
